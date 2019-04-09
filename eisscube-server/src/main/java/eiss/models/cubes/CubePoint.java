package eiss.models.cubes;

import lombok.Data;
import dev.morphia.annotations.*;

@Data
@Embedded
public class CubePoint {

    @Property Double lat;
    @Property Double lng;

}
