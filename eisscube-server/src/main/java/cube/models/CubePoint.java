package cube.models;

import lombok.Data;
import dev.morphia.annotations.*;

@Data
@Embedded(useDiscriminator = false)
public class CubePoint {

    @Property Double lat;
    @Property Double lng;

}
