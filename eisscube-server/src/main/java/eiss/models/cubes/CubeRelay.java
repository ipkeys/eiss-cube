package eiss.models.cubes;

import lombok.Data;
import dev.morphia.annotations.*;

@Data
@Embedded
public class CubeRelay {

    @Property Boolean connected;
    @Property String contacts;
    @Property String label;
    @Property Integer load;
    @Property String description;

}
