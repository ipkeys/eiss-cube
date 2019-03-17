package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Property;

@Data
@Entity("cubeproperties")
public class CubeProperty {

    @Id ObjectId id;

    @Property String name;
    @Property String label;
    @Property String description;

}
