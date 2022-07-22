package eiss.cube.service.http.process.cloudven;

import com.google.gson.Gson;
import eiss.models.cubes.CubeInput;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import eiss.cube.input.Conversion;
import eiss.cube.json.messages.cloudven.VenReport;
import eiss.cube.service.http.process.meters.Meter;
import eiss.cube.service.http.process.meters.MeterRequest;
import eiss.cube.service.http.process.meters.MeterResponse;
import dev.morphia.query.experimental.filters.Filters;
import eiss.api.Api;
import eiss.models.cubes.CubeReport;
import eiss.models.cubes.CubeSetup;
import eiss.models.cubes.EISScube;
import eiss.db.Cubes;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

@Slf4j
@Api
@Path("/cloudven/report")
public class VenMeterReportPostRoute implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final Datastore datastore;
    private final Gson gson;
    private final Conversion conversion;

    @Inject
    public VenMeterReportPostRoute(Vertx vertx, @Cubes Datastore datastore, Gson gson, Conversion conversion) {
        this.vertx = vertx;
        this.datastore = datastore;
        this.gson = gson;
        this.conversion = conversion;
    }

    @POST
    @Override
    public void handle(RoutingContext context) {
        HttpServerResponse response = context.response();

        String json = context.body().asString();

        VenReport req = gson.fromJson(json, VenReport.class);
        if (req == null) {
            response.setStatusCode(SC_BAD_REQUEST)
                    .setStatusMessage("Unable to get a report")
                    .end();
            return;
        }

        final String ven = req.getVen();
        final String resource = req.getResource();

        vertx.executeBlocking(op -> {
            Query<EISScube> qe = datastore.find(EISScube.class);
            qe.filter(
                Filters.eq("settings.VEN", ven),
                Filters.eq("name", resource)
            );

            EISScube eisscube = qe.first();
            if (eisscube != null) {
                MeterResponse report_res = getMeterResponse(eisscube.getId(), req.getFrom(), req.getTo(), req.getAggregation());
                op.complete(report_res);
            } else {
                op.fail("Report not found");
            }
        }, res -> {
            if (res.succeeded()) {
                response.setStatusCode(SC_OK)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(gson.toJson(res.result()));
            } else {
                response.setStatusCode(SC_BAD_REQUEST)
                        .setStatusMessage(res.cause().getMessage())
                        .end();
            }
        });

    }

    private MeterResponse getMeterResponse(ObjectId cubeId, Instant from, Instant to, String aggregation) {
        MeterResponse report_res = new MeterResponse();

        CubeSetup setup = datastore.find(CubeSetup.class).filter(Filters.eq("cubeID", cubeId)).first();
        if (setup != null && setup.getInput() != null) {
            CubeInput input = setup.getInput();
            CubeReport report = datastore.find(CubeReport.class).filter(Filters.eq("cubeID", cubeId), Filters.eq("type", input.getSignal())).first();
            if (report != null) {
                MeterRequest report_req = new MeterRequest();

                report_req.setUtcOffset(0L);
                report_req.setCubeID(cubeId.toString());
                report_req.setType(report.getType());
                report_req.setMeter(input.getMeter());
                report_req.setUnit(input.getUnit());
                report_req.setFactor(input.getFactor());
                report_req.setLoad(input.getLoad());
                report_req.setWatch(input.getWatch());

                Instant beginOfDay = from.truncatedTo(ChronoUnit.DAYS);
                Instant endOfDay = beginOfDay.plus(1440, ChronoUnit.MINUTES);
                report_req.setFrom(beginOfDay);
                report_req.setTo(endOfDay);
                report_req.setAggregation(aggregation);

                conversion.process(report_req, report_res); // whole day

                // crop for specified time period
                List<Meter> usage = report_res.getUsage().stream().filter(meter -> {
                    Instant t = meter.getT();
                    if (t.isBefore(from)) {
                        return false;
                    } else if (t.isAfter(to)) {
                        return false;
                    } else {
                        return (t.equals(from) || t.isAfter(from) && t.isBefore(to));
                    }
                }).collect(Collectors.toList());

                report_res.setUsage(usage);
            }
        }

        return report_res;
    }

}
