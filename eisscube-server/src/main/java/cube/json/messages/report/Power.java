package cube.json.messages.report;

import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data(staticConstructor="of")
public class Power {

    @NonNull private Instant t;
    @NonNull private Double v;

}
