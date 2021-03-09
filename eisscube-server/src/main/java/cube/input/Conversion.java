package cube.input;

import com.mongodb.BasicDBObject;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Projection;
import cube.db.Cube;
import cube.json.messages.report.Power;
import cube.json.messages.report.ReportRequest;
import cube.json.messages.report.ReportResponse;
import cube.models.AggregatedMeterData;
import cube.models.CubeMeter;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Sort.on;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static dev.morphia.query.experimental.filters.Filters.ne;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;

public class Conversion {

	private final Datastore datastore;

	@Inject
	public Conversion(@Cube Datastore datastore) {
		this.datastore = datastore;
	}

	public void process(ReportRequest req, ReportResponse res) {
		// Pulses
		if (req.getType().equalsIgnoreCase("p")) {
			switch (req.getAggregation()) {
				case "1m" -> getMinutelyReport(req, res, 1, 60);
				case "5m" -> getMinutelyReport(req, res, 5, 12);
				case "15m" -> getMinutelyReport(req, res, 15, 4);
				case "30m" -> getMinutelyReport(req, res, 30, 2);
				default -> getHourlyReport(req, res);
			}
		}
		// ~Pulses

		// Cycles
		if (req.getType().equalsIgnoreCase("c")) {
			switch (req.getAggregation()) {
				case "1m" -> converCyclesToReport(req, res, 1);
				case "5m" -> converCyclesToReport(req, res, 5);
				case "15m" -> converCyclesToReport(req, res, 15);
				case "30m" -> converCyclesToReport(req, res, 30);
				default -> converCyclesToReport(req, res, 60);
			}
		}
		// ~Cycles
	}

	private void getMinutelyReport(ReportRequest req, ReportResponse res, final int roundToMin, final int periodsPerHour) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();

/*
		Query<CubeMeter> q = datastore.find(CubeMeter.class);
		// filter
		q.filter(
			eq("cubeID", new ObjectId(req.getCubeID())),
			eq("type", req.getType()),
			ne("value", Double.NaN),
			gte("timestamp", from),
			lt("timestamp", to)
		);
		// sorting
		FindOptions o = new FindOptions().sort(Sort.ascending("timestamp"));
		// ~Step 1 - Prepare query params
*/

		// Step 2 - aggregated Meter data by timestamp to minute
		MorphiaCursor<AggregatedMeterData> aggregation = datastore.aggregate(CubeMeter.class)
			.match(
				eq("cubeID", new ObjectId(req.getCubeID())),
				eq("type", req.getType()),
				ne("value", Double.NaN),
				gte("timestamp", from),
				lt("timestamp", to)
			)
			.sort(on().ascending("timestamp"))
			.project(Projection.of()
				.include("value")
				.include("minutely",
						new Expression("$dateFromParts", new Document()
						.append("year", new Document("$year", "$timestamp"))
						.append("month", new Document("$month", "$timestamp"))
						.append("day", new Document("$dayOfMonth", "$timestamp"))
						.append("hour", new Document("$hour", "$timestamp"))
						.append("minute",
							new Document("$multiply", Arrays.asList(
									new Document("$ceil",
											new Document("$divide", Arrays.asList(
												new Document("$minute", "$timestamp"),
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
			.group(
				Group.of(id(field("minutely"))).field("power", sum(field("value")))
			)
			.execute(AggregatedMeterData.class);

//		MorphiaCursor<AggregatedMeterData> aggregation = pipeline.aggregate(AggregatedMeterData.class);
//		Iterator<AggregatedMeterData> aggregation = pipeline.aggregate(AggregatedMeterData.class);

		List<Power> usage = new ArrayList<>();
		aggregation.forEachRemaining(o -> {
			double power = (o.getPower() * periodsPerHour) / factor;
			usage.add(Power.of(o.getId(), Math.round(power * 100.0) / 100.0));
		});
		// ~Step 2 - aggregated Meter data by timestamp to minute

		// Step 3 - Unwind each "roundToMin" minutes between begin & end into array of timestamp
		List<Instant> minutes = new ArrayList<>();
		long num = (long)(Math.ceil(Duration.between(from, to).toMinutes() / (double)roundToMin) * roundToMin);
		for (long i = 0; i < num; i = i + roundToMin ) {
			minutes.add(from.plus(i, ChronoUnit.MINUTES));
		}
		// ~Step 3 - Unwind each "roundToMin" minutes between begin & end into array of timestamp

		// Step 4 - add each hour with level of Power into usage
		List<Power> usagePerRoundToMin = new ArrayList<>();
		minutes.forEach(timestamp -> {
			Optional<Power> p = usage.stream().filter(power -> power.getT().equals(timestamp)).findFirst();
			if (p.isPresent()) {
				usagePerRoundToMin.add(p.get());
			} else {
				usagePerRoundToMin.add(Power.of(timestamp, 0.0d));
			}
		});
		// ~Step 4 - add each minutes with level of Power into usage

		// Step 5 - make a chronological array
		usagePerRoundToMin.sort((p1, p2) -> p1.getT().compareTo(p2.getT()));
		// ~Step 5 - make a chronological array

		res.setUsage(usagePerRoundToMin);
	}

	private void getHourlyReport(ReportRequest req, ReportResponse res) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();
/*
		Query<CubeMeter> q = datastore.createQuery(CubeMeter.class);
		// filter
		q.and(
			q.criteria("cubeID").equal(new ObjectId(req.getCubeID())),
			q.criteria("type").equalIgnoreCase(req.getType()),
			q.criteria("value").notEqual(Double.NaN),
			q.criteria("timestamp").greaterThanOrEq(from),
			q.criteria("timestamp").lessThan(to)
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
			double power = o.getPower() / factor;
			usage.add(Power.of(o.getId(), Math.round(power * 100.0) / 100.0));
		});
		// ~Step 2 - aggregated Meter data by timestamp to hour

		// Step 3 - Unwind each 1 hour between begin & end into array of hours
		List<Instant> hours = new ArrayList<>();
		long num = Duration.between(from, to).toHours();
		for (long i = 0; i < num; i++ ) {
			hours.add(from.plus(i, ChronoUnit.HOURS));
		}
		// ~Step 3 - Unwind each 1 hour between begin & end into array of hours

		// Step 4 - add each hour with level of Power into usage
		List<Power> usagePerHour = new ArrayList<>();
		hours.forEach(timestamp -> {
			Optional<Power> p = usage.stream().filter(power -> power.getT().equals(timestamp)).findFirst();
			if (p.isPresent()) {
				usagePerHour.add(p.get());
			} else {
				usagePerHour.add(Power.of(timestamp, 0.0d));
			}
		});
		// ~Step 4 - add each minutes with level of Power into usage

		// Step 5 - make a chronological array
		usagePerHour.sort((p1, p2) -> p1.getT().compareTo(p2.getT()));
		// ~Step 5 - make a chronological array

		res.setUsage(usagePerHour);
*/

	}

	private void converCyclesToReport(ReportRequest req, ReportResponse res, final int roundToMin) {
		double load = req.getLoad() != null ? req.getLoad() : 1; // by default - 1kW

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();

		Query<CubeMeter> q = datastore.find(CubeMeter.class);
		// filter
		q.filter(
			eq("cubeID", new ObjectId(req.getCubeID())),
			eq("type", req.getType()),
			gte("timestamp", from),
			lt("timestamp", to)
		);
		// sorting
		FindOptions options = new FindOptions().sort(dev.morphia.query.Sort.ascending("timestamp"));
		// ~Step 1 - Prepare query params

		// Step 2 - Convert result of query into array of working intervals
		List<Interval> intervals = new ArrayList<>();

		List<CubeMeter> rc = q.iterator(options).toList();
		rc.forEach(o -> {
			if (o.getValue() == null) { // cycle in progress - duration is empty
				intervals.add(Interval.of(o.getTimestamp(), Instant.now())); // up to current moment
			} else {
				intervals.add(Interval.of(o.getTimestamp(), o.getTimestamp().plusSeconds(o.getValue().longValue())));
			}
		});
		// ~Step 2 - Convert result of query into array of working intervals

		// Step 3 - Unwind each 1 minutes between begin & end into array of munutes
		List<Instant> minutes = new ArrayList<>();
		long num = Duration.between(from, to).toMinutes();
		for (long i = 0; i < num; i++ ) {
			minutes.add(from.plus(i, ChronoUnit.MINUTES));
		}
		// ~Step 3 - Unwind each 1 minutes between begin & end into array of munutes

		// Step 4 - add each minutes with level of Power into usage
		final double power = Math.round(load * 100.0) / 100.0;
		List<Power> usagePerMinute = new ArrayList<>();
		minutes.forEach(m -> {
			boolean find = false;
			for (Interval i : intervals) {
				if (m.isAfter(i.getStop())) {
					continue;
				}
				if (m.equals(i.getStart()) ||
					(m.isAfter(i.getStart()) && m.isBefore(i.getStop()))
				) {
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
				.collect(groupingBy(Power::getT, averagingDouble(Power::getV)))
				.forEach((k, v) -> usage.add(Power.of(k, Math.round(v * 100.0) / 100.0)));
			// ~Step 5 - aggregation

			// Step 6 - make a chronological array
			usage.sort((p1, p2) -> p1.getT().compareTo(p2.getT()));
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
