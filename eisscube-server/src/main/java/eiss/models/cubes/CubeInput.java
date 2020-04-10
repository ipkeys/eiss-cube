package eiss.models.cubes;

import lombok.Data;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

@Data
@Embedded
public class CubeInput {

    @Property Boolean connected;
    @Property String signal;
    @Property Float factor;
    @Property String watch;
    @Property Float load;
    @Property String label;
    @Property String description;

}
