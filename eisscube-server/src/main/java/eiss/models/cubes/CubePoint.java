package eiss.models.cubes;

import lombok.Data;
import xyz.morphia.annotations.*;

@Data
@Embedded
public class CubePoint {

    @Property Double lat;
    @Property Double lng;

}
