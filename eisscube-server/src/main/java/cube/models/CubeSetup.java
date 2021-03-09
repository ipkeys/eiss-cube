package cube.models;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

@Data
@Entity(value = "cubesetups", useDiscriminator = false)
public class CubeSetup {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex", unique = true))
    @Property ObjectId cubeID; // reference to EISScube.id

    @Property CubeRelay relay;
    @Property CubeInput input;

}
