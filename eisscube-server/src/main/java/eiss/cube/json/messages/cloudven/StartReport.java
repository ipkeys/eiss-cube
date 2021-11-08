package eiss.cube.json.messages.cloudven;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StartReport {

    private String ven;
    private String resource;
    private Integer sampleRateSeconds;

}
