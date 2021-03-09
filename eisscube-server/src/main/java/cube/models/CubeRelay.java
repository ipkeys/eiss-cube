package cube.models;

import lombok.Data;
import dev.morphia.annotations.*;

@Data
@Embedded
public class CubeRelay {

    @Property Boolean connected;
    @Property String contacts;
    @Property String label;
    @Property String description;

}
