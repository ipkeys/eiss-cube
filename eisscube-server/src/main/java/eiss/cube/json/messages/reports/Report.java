package eiss.cube.json.messages.reports;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Report {

    private Instant t;
    private Double v;

}
