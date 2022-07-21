package eiss.cube.json.messages.reports;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportRequest {

    private String deviceID;

    //private String type;
    private Instant from;
    private Instant to;
    //private Long utcOffset;

    private String aggregation;
    //private String meter;
    //private String unit;
    //private Float factor;

    //private String watch;
    //private Float load;

}
