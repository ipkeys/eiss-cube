package cube.json.messages.report;

import lombok.Data;

import java.time.Instant;

@Data
public class ReportRequest {

    private String cubeID;
    private String type;
    private Instant from;
    private Instant to;
    private Long utcOffset;
    private String aggregation;
    private Float factor;
    private String watch;
    private Float load;

}
