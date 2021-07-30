package cube.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Entity(useDiscriminator = false)
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
