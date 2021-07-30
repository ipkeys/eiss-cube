package cube.models;

import io.vertx.core.json.JsonObject;
import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Arrays;

@Data
@Entity(value = "cubecommands", useDiscriminator = false)
public class CubeCommand {

    @Id
    ObjectId id;

    @Property
    String status;

    @Indexed(options = @IndexOptions(name = "CubeCommandsCreatedIndex"))
    @Property
    Instant created;

    @Property
    Instant sent;
    @Property
    Instant received;

    @Property
    String deviceType;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property
    ObjectId cubeID; // reference to EISScube.id
    @Property
    String cubeName;

    @Indexed(options = @IndexOptions(name = "CommandGroupIndex"))
    @Property
    String group;

    @Indexed(options = @IndexOptions(name = "CommandGroupIdIndex"))
    @Property
    String group_id;

    @Property
    String command;

    @Property
    Instant startTime;
    @Property
    Instant endTime;

    @Property
    Integer completeCycle;
    @Property
    Integer dutyCycle;

    @Property
    String transition;

    // default
    public CubeCommand() {}

    // from String
    public CubeCommand(String cmd) {
        Arrays.stream(cmd.split("&"))
            .map(this::splitParameter)
            .forEach(entry -> {
                switch (entry.getKey()) {
                    case "c":
                        this.command = entry.getValue();
                        break;
                    case "st":
                        this.startTime = Instant.ofEpochSecond(Long.parseLong(entry.getValue()));
                        break;
                    case "dur":
                        this.endTime = this.startTime.plus(Long.parseLong(entry.getValue()), ChronoUnit.SECONDS);
                        break;
                    case "each":
                        this.completeCycle = Integer.parseInt(entry.getValue());
                        break;
                    case "pct":
                        this.dutyCycle = Integer.parseInt(entry.getValue());
                        break;
                    case "edge":
                        this.transition = entry.getValue();
                        break;
                    default:
                        break;
                }
            });
    }

    public AbstractMap.SimpleImmutableEntry<String, String> splitParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;

        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        if (command != null && !command.isEmpty()) {
            b.append("c=").append(command);
        }

        if (startTime != null) {
            long start = startTime.getEpochSecond();
            b.append("&st=").append(String.format("%d", start));
        }
        if (startTime != null && endTime != null) {
            long start = startTime.getEpochSecond();
            long end = endTime.getEpochSecond();

            b.append("&dur=").append(String.format("%d", end - start));
        }
        if (startTime == null && endTime != null) {
            long start = Instant.now().getEpochSecond();
            long end = endTime.getEpochSecond();

            b.append("&st=").append(String.format("%d", start)); // duration need start point!!!
            b.append("&dur=").append(String.format("%d", end - start));
        }

        if (completeCycle != null && completeCycle != 0) {
            b.append("&each=").append(String.format("%d", completeCycle));
        }

        if (dutyCycle != null && dutyCycle != 0) {
            b.append("&pct=").append(String.format("%d", dutyCycle));
        }

        if (transition != null && !transition.isEmpty()) {
            b.append("&edge=").append(transition);
        }

        if (id != null) {
            b.append("&id=").append(id);
        }

        return b.toString();
    }

    public String toJsonObject() {
        JsonObject o = new JsonObject();

        if (command != null && !command.isEmpty()) {
            o.put("CMD", command.toUpperCase());
        }

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

        if (completeCycle != null && completeCycle != 0) {
            o.put("EACH", completeCycle);
        }

        if (dutyCycle != null && dutyCycle != 0) {
            o.put("PCT", dutyCycle);
        }

        if (transition != null && !transition.isEmpty()) {
            o.put("EDGE", transition);
        }

        if (id != null) {
            o.put("ID", id.toString());
        }

        return o.encode(); //.encodePrettily() is not needed
    }

}
