package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Entity("cubecommands")
public class CubeCommand {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeCommandsCreatedIndex", unique = true))
    @Property Instant created;

    @Property String cubeID; // reference to EISScube.deviceID

    @Property String command;

    //@Property Boolean target1;
    //@Property Boolean target2;

    @Property Instant startTime;
    @Property Instant endTime;

    @Property Integer completeCycle;
    @Property Integer dutyCycle;

    @Property String transition;

    @Property String status = "pending";
    @Property Instant updated;

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();

        if (command != null && !command.isEmpty()) {
            b.append("c=").append(command);
        }

/*
        List<String> targets = new ArrayList<>();
        if (target1 != null && target1) {
            targets.add("1");
        }
        if (target2 != null && target2) {
            targets.add("2");
        }

        if (targets.size() > 0) {
            String commaSeparatedNumbers = targets.stream().map(Object::toString).collect(Collectors.joining(","));
            b.append("&t=").append(commaSeparatedNumbers);
        }
*/

        if (startTime != null) {
            long starttime = startTime.toEpochMilli() / 1000;
            b.append("&st=").append(String.format("%d", starttime));
        }

        if (endTime != null) {
            long starttime;
            long endtime = endTime.toEpochMilli() / 1000;
            if (startTime != null) {
                starttime = startTime.toEpochMilli() / 1000;
            } else {
                starttime = new Date().getTime() / 1000;
            }

            b.append("&dur=").append(String.format("%d", endtime - starttime));
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

        return b.toString();
    }

}
