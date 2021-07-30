package cube.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Entity(useDiscriminator = false)
public class CubeRelay {

    @Property Boolean connected;
    @Property String contacts;
    @Property String label;
    @Property String description;

}
