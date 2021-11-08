package eiss.cube.input;

import lombok.Data;
import lombok.NonNull;

import java.time.Instant;

@Data(staticConstructor="of")
public class Interval {

	@NonNull private Instant start;
	@NonNull private Instant stop;

}
