package cube.service.http.process.meters;

import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data(staticConstructor="of")
public class Meter {

    @NonNull private Instant t;
    @NonNull private Double v;

}
