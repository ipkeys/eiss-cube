package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

@Data
@Entity(value = "cubereports", noClassnameStored = true)
public class CubeReport {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property ObjectId cubeID; // reference to EISScube.id

    String cubeName;
    @Property String type;

}
