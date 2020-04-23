package eiss.cube.input;

import com.mongodb.BasicDBObject;
import dev.morphia.Datastore;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import eiss.cube.db.Cube;
import eiss.cube.json.messages.report.Power;
import eiss.cube.json.messages.report.ReportRequest;
import eiss.cube.json.messages.report.ReportResponse;
import eiss.models.cubes.AggregatedMeterData;
import eiss.models.cubes.CubeMeter;
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

import static dev.morphia.aggregation.Group.grouping;
import static dev.morphia.aggregation.Group.sum;
import static dev.morphia.aggregation.Projection.expression;
import static dev.morphia.aggregation.Projection.projection;
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
	}

	private void getMinutelyReport(ReportRequest req, ReportResponse res, final int roundToMin, final int periodsPerHour) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();

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

	private void getHourlyReport(ReportRequest req, ReportResponse res, final int periodsPerHour) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();

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
			double power = (o.getPower() * periodsPerHour) / factor;
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
	}

	private void converCyclesToReport(ReportRequest req, ReportResponse res, final int roundToMin) {
		double load = req.getLoad() != null ? req.getLoad() : 1; // by default - 1kW

		// Step 1 - Prepare query params
		Instant from = req.getFrom();
		Instant to = req.getTo();

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

		// Step 2 - Convert result of query into array of working intervals
		List<Interval> intervals = new ArrayList<>();

		List<CubeMeter> rc = q.find().toList();
		rc.forEach(o -> {
			intervals.add(Interval.of(o.getTimestamp(), o.getTimestamp().plusSeconds(o.getValue().longValue())));
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
