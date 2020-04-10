package eiss.cube.service.http.process.meters;

import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data(staticConstructor="of")
public class Interval {

	@NonNull private Instant start;
	@NonNull private Instant stop;

}
