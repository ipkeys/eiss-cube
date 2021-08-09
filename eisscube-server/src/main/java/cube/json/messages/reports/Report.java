package cube.json.messages.reports;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data
@Builder
public class Report {

    private Instant t;
    private Double v;

}
