package cube.json.messages.commands;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Command {

    private String id;

    private String deviceID;
    private String command;

    private Integer completeCycle;
    private Integer dutyCycle;
    private String transition;

    private Instant startTime;
    private Instant endTime;

    private Instant sent;
    private Instant created;
    private Instant received;

    private String status;

}
