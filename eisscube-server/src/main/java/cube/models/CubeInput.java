package cube.models;

import lombok.Data;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;

@Data
@Embedded(useDiscriminator = false)
public class CubeInput {

    @Property Boolean connected;
    @Property String signal;
    @Property String meter;
    @Property String unit;
    @Property Float factor;
    @Property String watch;
    @Property Float load;
    @Property String label;
    @Property String description;

}
