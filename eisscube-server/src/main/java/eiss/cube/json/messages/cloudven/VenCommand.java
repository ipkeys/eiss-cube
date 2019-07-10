package eiss.cube.json.messages.cloudven;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class VenCommand {

    private String ven;
    private String resource;
    private Instant start;
    private Instant end;
    private String command;

}
