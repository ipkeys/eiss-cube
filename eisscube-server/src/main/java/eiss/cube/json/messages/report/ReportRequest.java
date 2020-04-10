package eiss.cube.json.messages.report;

import lombok.Data;

import java.time.Instant;

@Data
public class ReportRequest {

    private String cubeID;
    private String type;
    private Instant day;
    private Long utcOffset;
    private String aggregation;
    private Float factor;
    private String watch;
    private Float load;

}
