package eiss.cube.service.http.process.meters;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.query.Sort;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.report.Power;
import eiss.cube.json.messages.report.ReportRequest;
import eiss.cube.json.messages.report.ReportResponse;
import eiss.cube.service.http.process.api.Api;
import eiss.models.cubes.AggregatedMeterData;
import eiss.models.cubes.CubeMeter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static dev.morphia.aggregation.Group.grouping;
import static dev.morphia.aggregation.Group.sum;
import static dev.morphia.aggregation.Projection.expression;
import static dev.morphia.aggregation.Projection.projection;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;
import static javax.servlet.http.HttpServletResponse.*;

@Slf4j
@Api
@Path("/meters")
public class PostRoute implements Handler<RoutingContext> {

    private Vertx vertx;
    private Datastore datastore;
    private Gson gson;

    @Inject
    public PostRoute(Vertx vertx, @Cube Datastore datastore, Gson gson) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();
        String jsonBody = context.getBodyAsString();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            vertx.executeBlocking(op -> {
                try {
                    ReportRequest req = gson.fromJson(jsonBody, ReportRequest.class);
                    if (req == null) {
                        op.fail("Bad request");
                    } else {
                        log.debug("Request: {}", req);
                        ReportResponse res = new ReportResponse();

                        // Pulses
                        if (req.getType().equalsIgnoreCase("p")) {
                            switch (req.getAggregation()) {
                                case "1m":
                                    getMinutelyReport(req, res, 1, 60);
                                    break;
                                case "5m":
                                    getMinutelyReport(req, res, 5, 12);
                                    break;
                                case "15m":
                                    getMinutelyReport(req, res, 15, 4);
                                    break;
                                case "30m":
                                    getMinutelyReport(req, res, 30, 2);
                                    break;
                                case "1h":
                                default:
                                    getHourlyReport(req, res, 1);
                                    break;
                            }
                        }
                        // ~Pulses

                        // Cycles
                        if (req.getType().equalsIgnoreCase("c")) {
                            switch (req.getAggregation()) {
                                case "1m":
                                    converCyclesToReport(req, res, 1);
                                    break;
                                case "5m":
                                    converCyclesToReport(req, res, 5);
                                    break;
                                case "15m":
                                    converCyclesToReport(req, res, 15);
                                    break;
                                case "30m":
                                    converCyclesToReport(req, res, 30);
                                    break;
                                case "1h":
                                default:
                                    converCyclesToReport(req, res, 60);
                                    break;
                            }
                        }
                        // ~Cycles

                        op.complete(gson.toJson(res));
                    }
                } catch (Exception e) {
                    op.fail(e.getMessage());
                }
            }, res -> {
                if (res.succeeded()) {
                    response.setStatusCode(SC_OK)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end((String)res.result());
                } else {
                    response.setStatusCode(SC_BAD_REQUEST)
                            .setStatusMessage(res.cause().getMessage())
                            .end();
                }
            });
        } else {
            response.setStatusCode(SC_BAD_REQUEST)
                    .end();
        }
    }

    private void getMinutelyReport(ReportRequest req, ReportResponse res, final int roundToMin, final int periodsPerHour) {
        final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

        // Step 1 - Prepare query params
        Instant begin = req.getDay(); //.plus(req.getUtcOffset(), ChronoUnit.MINUTES);
        Instant end = begin.plus(1439, ChronoUnit.MINUTES);

        Query<CubeMeter> q = datastore.createQuery(CubeMeter.class);
        // filter
        q.and(
            q.criteria("cubeID").equal(new ObjectId(req.getCubeID())),
            q.criteria("type").equalIgnoreCase(req.getType()),
            q.criteria("value").notEqual(Double.NaN),
            q.criteria("timestamp").greaterThanOrEq(begin),
            q.criteria("timestamp").lessThan(end)
        );
        // sorting
        q.order(Sort.ascending("timestamp"));
        // ~Step 1 - Prepare query params

        // Step 2 - aggregated Meter data by timestamp to minute
        AggregationPipeline pipeline = datastore.createAggregation(CubeMeter.class)
            .match(q)
            .project(
                projection("value"),
                projection("minutely",
                    expression("$dateFromParts", new BasicDBObject()
                        .append("year", new BasicDBObject("$year", "$timestamp"))
                        .append("month", new BasicDBObject("$month", "$timestamp"))
                        .append("day", new BasicDBObject("$dayOfMonth", "$timestamp"))
                        .append("hour", new BasicDBObject("$hour", "$timestamp"))
                        .append("minute",
                            new BasicDBObject("$multiply", Arrays.asList(
                                new BasicDBObject("$ceil",
                                    new BasicDBObject("$divide", Arrays.asList(
                                        new BasicDBObject("$minute", "$timestamp"),
                                        roundToMin
                                        )
                                    )
                                ),
                                roundToMin
                                )
                            )
                        )
                    )
                )
            )
            .group("minutely",
                grouping("power", sum("value"))
            );

        Iterator<AggregatedMeterData> aggregation = pipeline.aggregate(AggregatedMeterData.class);

        List<Power> usage = new ArrayList<>();
        aggregation.forEachRemaining(o -> {
            double power = (o.getPower() * periodsPerHour) / factor;
            usage.add(Power.of(o.getId(), Math.round(power * 100.0) / 100.0));
        });
        // ~Step 2 - aggregated Meter data by timestamp to minute

        // Step 3 - Unwind each "roundToMin" minutes between begin & end into array of timestamp
        List<Instant> minutes = new ArrayList<>();
        long num = (long)(Math.ceil(Duration.between(begin, end).toMinutes() / (double)roundToMin) * roundToMin);
        for (long i = 0; i <= num; i = i + roundToMin ) {
            minutes.add(begin.plus(i, ChronoUnit.MINUTES));
        }
        // ~Step 3 - Unwind each "roundToMin" minutes between begin & end into array of timestamp

        // Step 4 - add each hour with level of Power into usage
        List<Power> usagePerRoundToMin = new ArrayList<>();
        minutes.forEach(timestamp -> {
            Optional<Power> p = usage.stream().filter(power -> power.getTimestamp().equals(timestamp)).findFirst();
            if (p.isPresent()) {
                usagePerRoundToMin.add(p.get());
            } else {
                usagePerRoundToMin.add(Power.of(timestamp, 0.0d));
            }
        });
        // ~Step 4 - add each minutes with level of Power into usage

        // Step 5 - make a chronological array
        usagePerRoundToMin.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
        // ~Step 5 - make a chronological array

        res.setUsage(usagePerRoundToMin);
    }

    private void getHourlyReport(ReportRequest req, ReportResponse res, final int periodsPerHour) {
        final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

        // Step 1 - Prepare query params
        Instant begin = req.getDay(); //.plus(req.getUtcOffset(), ChronoUnit.MINUTES);
        Instant end = begin.plus(1439, ChronoUnit.MINUTES);

        Query<CubeMeter> q = datastore.createQuery(CubeMeter.class);
        // filter
        q.and(
            q.criteria("cubeID").equal(new ObjectId(req.getCubeID())),
            q.criteria("type").equalIgnoreCase(req.getType()),
            q.criteria("value").notEqual(Double.NaN),
            q.criteria("timestamp").greaterThanOrEq(begin),
            q.criteria("timestamp").lessThan(end)
        );
        // sorting
        q.order(Sort.ascending("timestamp"));
        // ~Step 1 - Prepare query params

        // Step 2 - aggregated Meter data by timestamp to hour
        Iterator<AggregatedMeterData> aggregation = datastore.createAggregation(CubeMeter.class)
            .match(q)
            .project(
                projection("value"),
                projection("hourly",
                    expression("$dateFromParts", new BasicDBObject()
                        .append("year", new BasicDBObject("$year", "$timestamp"))
                        .append("month", new BasicDBObject("$month", "$timestamp"))
                        .append("day", new BasicDBObject("$dayOfMonth", "$timestamp"))
                        .append("hour", new BasicDBObject("$hour", "$timestamp"))
                    )
                )
            )
            .group("hourly",
                grouping("power", sum("value"))
            )
            .aggregate(AggregatedMeterData.class);

        List<Power> usage = new ArrayList<>();
        aggregation.forEachRemaining(o -> {
            double power = (o.getPower() * periodsPerHour) / factor;
            usage.add(Power.of(o.getId(), Math.round(power * 100.0) / 100.0));
        });
        // ~Step 2 - aggregated Meter data by timestamp to hour

        // Step 3 - Unwind each 1 hour between begin & end into array of hours
        List<Instant> hours = new ArrayList<>();
        long num = Duration.between(begin, end).toHours();
        for (long i = 0; i <= num; i++ ) {
            hours.add(begin.plus(i, ChronoUnit.HOURS));
        }
        // ~Step 3 - Unwind each 1 hour between begin & end into array of hours

        // Step 4 - add each hour with level of Power into usage
        List<Power> usagePerHour = new ArrayList<>();
        hours.forEach(timestamp -> {
            Optional<Power> p = usage.stream().filter(power -> power.getTimestamp().equals(timestamp)).findFirst();
            if (p.isPresent()) {
                usagePerHour.add(p.get());
            } else {
                usagePerHour.add(Power.of(timestamp, 0.0d));
            }
        });
        // ~Step 4 - add each minutes with level of Power into usage

        // Step 5 - make a chronological array
        usagePerHour.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
        // ~Step 5 - make a chronological array

        res.setUsage(usagePerHour);
    }

    private void converCyclesToReport(ReportRequest req, ReportResponse res, final int roundToMin) {
        // Step 1 - Prepare query params
        Instant begin = req.getDay(); //.plus(req.getUtcOffset(), ChronoUnit.MINUTES);
        Instant end = begin.plus(1439, ChronoUnit.MINUTES);

        Query<CubeMeter> q = datastore.createQuery(CubeMeter.class);
        // filter
        q.and(
                q.criteria("cubeID").equal(new ObjectId(req.getCubeID())),
                q.criteria("type").equalIgnoreCase(req.getType()),
                q.criteria("value").notEqual(Double.NaN),
                q.criteria("timestamp").greaterThanOrEq(begin),
                q.criteria("timestamp").lessThan(end)
        );
        // sorting
        q.order(Sort.ascending("timestamp"));
        // ~Step 1 - Prepare query params

        // Step 2 - Convert result of query into array of working intervals
        List<Interval> intervals = new ArrayList<>();

        List<CubeMeter> rc = q.find().toList();
        rc.forEach(o -> {
            intervals.add(Interval.of(o.getTimestamp(), o.getTimestamp().plusSeconds(o.getValue().longValue())));
        });
        // ~Step 2 - Convert result of query into array of working intervals

        // Step 3 - Unwind each 1 minutes between begin & end into array of munutes
        List<Instant> minutes = new ArrayList<>();
        long num = Duration.between(begin, end).toMinutes();
        for (long i = 0; i <= num; i++ ) {
            minutes.add(begin.plus(i, ChronoUnit.MINUTES));
        }
        // ~Step 3 - Unwind each 1 minutes between begin & end into array of munutes

        // Step 4 - add each minutes with level of Power into usage
        double load = req.getLoad() != null ? req.getLoad() : 1000; // by default - 1kW
        final double power = Math.round(load * 100.0) / 100.0;
        List<Power> usagePerMinute = new ArrayList<>();
        minutes.forEach(m -> {
            boolean find = false;
            for (Interval i : intervals) {
                if (m.isAfter(i.getStop())) {
                    continue;
                }
                if (m.isAfter(i.getStart()) && m.isBefore(i.getStop())) {
                    Instant timestamp = roundTimeToMinute(m, roundToMin);
                    usagePerMinute.add(Power.of(timestamp, power));
                    find = true;
                    break;
                }
            }
            if (!find) {
                Instant timestamp = roundTimeToMinute(m, roundToMin);
                usagePerMinute.add(Power.of(timestamp, 0.0d));
            }
        });
        // ~Step 4 - add each minutes with level of Power into usage

        if (roundToMin > 1) {
            // Step 5 - aggregation
            List<Power> usage = new ArrayList<>();
            usagePerMinute
                .parallelStream()
                .collect(groupingBy(Power::getTimestamp, averagingDouble(Power::getValue)))
                .forEach((k, v) -> usage.add(Power.of(k, Math.round(v * 100.0) / 100.0)));
            // ~Step 5 - aggregation

            // Step 6 - make a chronological array
            usage.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
            // ~Step 6 - make a chronological array

            res.setUsage(usage);
        } else {
            res.setUsage(usagePerMinute);
        }
    }

    private Instant roundTimeToMinute(Instant time, int roundToMin) {
        Instant rc;

        if (roundToMin == 1) {
            rc = time;
        } else {
            Instant timestamp = time.truncatedTo(ChronoUnit.HOURS);
            if (roundToMin != 60) {
                float minute_of_hour = (float)time.atZone(ZoneOffset.UTC).getMinute();
                long rounded_minute = (long) (Math.ceil(minute_of_hour / roundToMin) * roundToMin);
                timestamp = timestamp.plus(rounded_minute, ChronoUnit.MINUTES);
            }
            rc = timestamp;
        }

        return rc;
    }
}
