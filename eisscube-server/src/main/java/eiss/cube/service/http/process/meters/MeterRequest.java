package eiss.cube.service.http.process.meters;

import lombok.Data;

import java.time.Instant;

@Data
public class MeterRequest {

    private String cubeID;
    private String type;
    private Instant from;
    private Instant to;
    private Long utcOffset;
    private String aggregation;
    private Float factor;
    private String watch;
    private Float load;
    private String meter;
    private String unit;

}
