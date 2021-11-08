package eiss.cube.utils;

import eiss.models.cubes.CubeCommand;
import io.vertx.core.json.JsonObject;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CubeCommandToJson {

	public static String convert(CubeCommand cmd) {
		JsonObject o = new JsonObject();

		String command = cmd.getCommand();
		if (command != null && !command.isEmpty()) {
			o.put("CMD", command.toUpperCase());
		}

		Instant startTime = cmd.getStartTime();
		Instant endTime = cmd.getEndTime();
		if (startTime != null) {
			o.put("ST", startTime.toString());
		}
		if (startTime != null && endTime != null) {
			long start = startTime.getEpochSecond();
			long end = endTime.getEpochSecond();

			o.put("DUR", end - start);
		}
		if (startTime == null && endTime != null) {
			Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
			long start = now.getEpochSecond();
			long end = endTime.getEpochSecond();

			o.put("ST", now.toString());
			o.put("DUR", end - start); // duration need start point!!!;
		}

		Integer completeCycle = cmd.getCompleteCycle();
		if (completeCycle != null && completeCycle != 0) {
			o.put("EACH", completeCycle);
		}

		Integer dutyCycle = cmd.getDutyCycle();
		if (dutyCycle != null && dutyCycle != 0) {
			o.put("PCT", dutyCycle);
		}

		String transition = cmd.getTransition();
		if (transition != null && !transition.isEmpty()) {
			o.put("EDGE", transition);
		}
		ObjectId id = cmd.getId();
		if (id != null) {
			o.put("ID", id.toString());
		}

		return o.encode(); //.encodePrettily() is not needed
	}

}
