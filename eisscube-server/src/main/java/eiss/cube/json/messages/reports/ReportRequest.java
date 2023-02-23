package eiss.cube.json.messages.reports;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportRequest {

    private String deviceID;
    private Instant from;
    private Instant to;
    private String aggregation;

}
