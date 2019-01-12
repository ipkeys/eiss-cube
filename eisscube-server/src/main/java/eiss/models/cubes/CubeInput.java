package eiss.models.cubes;

import lombok.Data;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Property;

@Data
@Embedded
public class CubeInput {

    @Property Boolean connected;
    @Property String signal;
    @Property Integer factor;
    @Property Integer watch;
    @Property String label;
    @Property String description;

}
