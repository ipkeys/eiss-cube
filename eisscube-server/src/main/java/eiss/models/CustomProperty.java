package eiss.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@Entity(value = "customproperties", useDiscriminator = false)
public class CustomProperty {

    @Id ObjectId id;
    @Property String name;
    @Property String label;
    @Property String description;
    @Property String type; // group | user

}
