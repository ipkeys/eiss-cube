package eiss.cube.input;

import eiss.cube.service.http.process.meters.Meter;
import dev.morphia.Datastore;
import dev.morphia.aggregation.experimental.expressions.impls.Expression;
import dev.morphia.aggregation.experimental.stages.Group;
import dev.morphia.aggregation.experimental.stages.Projection;
import eiss.cube.service.http.process.meters.MeterRequest;
import eiss.cube.service.http.process.meters.MeterResponse;
import eiss.models.cubes.AggregatedMeterData;
import eiss.models.cubes.CubeMeter;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.MorphiaCursor;
import eiss.db.Cubes;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static dev.morphia.aggregation.experimental.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.experimental.expressions.Expressions.field;
import static dev.morphia.aggregation.experimental.stages.Group.id;
import static dev.morphia.aggregation.experimental.stages.Sort.sort;
import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.gte;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static dev.morphia.query.experimental.filters.Filters.ne;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;

public class Conversion {

	private final Datastore datastore;

	@Inject
	public Conversion(@Cubes Datastore datastore) {
		this.datastore = datastore;
	}

	public void process(MeterRequest req, MeterResponse res) {
		// Pulses
		if (req.getType().equalsIgnoreCase("p")) {
			switch (req.getAggregation()) {
				case "1m" -> getMinutelyReport(req, res, 1);
				case "5m" -> getMinutelyReport(req, res, 5);
				case "15m" -> getMinutelyReport(req, res, 15);
				case "30m" -> getMinutelyReport(req, res, 30);
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

	private void getMinutelyReport(MeterRequest req, MeterResponse res, final int roundToMin) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh
		int periodsPerHour = 60 / roundToMin;

		if (req.getMeter().equalsIgnoreCase("g")) { // for Gas Meter value is not depends on hour!!!
			periodsPerHour = 1;
		}

		Instant from = req.getFrom();
		Instant to = req.getTo();

		// Step 1 - aggregated Meter data by timestamp to minute
		MorphiaCursor<AggregatedMeterData> aggregation = datastore.aggregate(CubeMeter.class)
			.match(
				eq("cubeID", new ObjectId(req.getCubeID())),
				eq("type", req.getType()),
				ne("value", Double.NaN),
				gte("timestamp", from),
				lt("timestamp", to)
			)
			.sort(sort().ascending("timestamp"))
			.project(Projection.project()
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
									), roundToMin
								)
							)
						)
					)
				)
			)
			.group(
				Group.group(id(field("minutely"))).field("value", sum(field("value")))
			)
			.execute(AggregatedMeterData.class);

		List<Meter> usage = new ArrayList<>();
		final int finalPeriodsPerHour = periodsPerHour;
		aggregation.forEachRemaining(o -> {
			double value = (o.getValue() * finalPeriodsPerHour) / factor;
			usage.add(Meter.of(o.getId(), Math.round(value * 100.0) / 100.0));
		});
		// ~Step 1 - aggregated Meter data by timestamp to minute

		// Step 2 - Unwind each "roundToMin" minutes between begin & end into array of timestamp
		List<Instant> minutes = new ArrayList<>();
		long num = (long)(Math.ceil(Duration.between(from, to).toMinutes() / (double)roundToMin) * roundToMin);
		for (long i = 0; i < num; i = i + roundToMin ) {
			minutes.add(from.plus(i, ChronoUnit.MINUTES));
		}
		// ~Step 2 - Unwind each "roundToMin" minutes between begin & end into array of timestamp

		// Step 3 - add each hour with level of Power into usage
		List<Meter> usagePerRoundToMin = new ArrayList<>();
		minutes.forEach(timestamp -> {
			Optional<Meter> p = usage.stream().filter(meter -> meter.getT().equals(timestamp)).findFirst();
			if (p.isPresent()) {
				usagePerRoundToMin.add(p.get());
			} else {
				usagePerRoundToMin.add(Meter.of(timestamp, 0.0d));
			}
		});
		// ~Step 3 - add each minutes with level of Power into usage

		// Step 4 - make a chronological array
		usagePerRoundToMin.sort(Comparator.comparing(Meter::getT));
		// ~Step 4 - make a chronological array

		res.setUsage(usagePerRoundToMin);
	}

	private void getHourlyReport(MeterRequest req, MeterResponse res) {
		final double factor = req.getFactor() != null ? req.getFactor() : 1000; // by default - 1000 pulses per 1kWh
		Instant from = req.getFrom();
		Instant to = req.getTo();

		// Step 1 - aggregated Meter data by timestamp to hour
		MorphiaCursor<AggregatedMeterData> aggregation = datastore.aggregate(CubeMeter.class)
			.match(
				eq("cubeID", new ObjectId(req.getCubeID())),
				eq("type", req.getType()),
				ne("value", Double.NaN),
				gte("timestamp", from),
				lt("timestamp", to)
			)
			.sort(sort().ascending("timestamp"))
			.project(Projection.project()
				.include("value")
				.include("hourly",
					new Expression("$dateFromParts", new Document()
						.append("year", new Document("$year", "$timestamp"))
						.append("month", new Document("$month", "$timestamp"))
						.append("day", new Document("$dayOfMonth", "$timestamp"))
						.append("hour", new Document("$hour", "$timestamp"))
					)
				)
			)
			.group(
				Group.group(id(field("hourly"))).field("value", sum(field("value")))
			)
			.execute(AggregatedMeterData.class);

		List<Meter> usage = new ArrayList<>();
		aggregation.forEachRemaining(o -> {
			double value = o.getValue() / factor;
			usage.add(Meter.of(o.getId(), Math.round(value * 100.0) / 100.0));
		});
		// ~Step 1 - aggregated Meter data by timestamp to hour

		// Step 2 - Unwind each 1 hour between begin & end into array of hours
		List<Instant> hours = new ArrayList<>();
		long num = Duration.between(from, to).toHours();
		for (long i = 0; i < num; i++ ) {
			hours.add(from.plus(i, ChronoUnit.HOURS));
		}
		// ~Step 2 - Unwind each 1 hour between begin & end into array of hours

		// Step 3 - add each hour with level of Power into usage
		List<Meter> usagePerHour = new ArrayList<>();
		hours.forEach(timestamp -> {
			Optional<Meter> p = usage.stream().filter(meter -> meter.getT().equals(timestamp)).findFirst();
			if (p.isPresent()) {
				usagePerHour.add(p.get());
			} else {
				usagePerHour.add(Meter.of(timestamp, 0.0d));
			}
		});
		// ~Step 3 - add each minutes with level of Power into usage

		// Step 4 - make a chronological array
		usagePerHour.sort(Comparator.comparing(Meter::getT));
		// ~Step 4 - make a chronological array

		res.setUsage(usagePerHour);
	}

	private void converCyclesToReport(MeterRequest req, MeterResponse res, final int roundToMin) {
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
		List<Meter> usagePerMinute = new ArrayList<>();
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
					usagePerMinute.add(Meter.of(timestamp, power));
					find = true;
					break;
				}
			}
			if (!find) {
				Instant timestamp = roundTimeToMinute(m, roundToMin);
				usagePerMinute.add(Meter.of(timestamp, 0.0d));
			}
		});
		// ~Step 4 - add each minutes with level of Power into usage

		if (roundToMin > 1) {
			// Step 5 - aggregation
			List<Meter> usage = new ArrayList<>();
			usagePerMinute
				.parallelStream()
				.collect(groupingBy(Meter::getT, averagingDouble(Meter::getV)))
				.forEach((k, v) -> usage.add(Meter.of(k, Math.round(v * 100.0) / 100.0)));
			// ~Step 5 - aggregation

			// Step 6 - make a chronological array
			usage.sort(Comparator.comparing(Meter::getT));
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
