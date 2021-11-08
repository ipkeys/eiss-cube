package eiss.cube.json.messages.cloudven;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class VenReport {

    private String ven;
    private String resource;
    private Instant from;
    private Instant to;
    private String aggregation; // 1m, 5m, 15m, 30m, 1h

}
