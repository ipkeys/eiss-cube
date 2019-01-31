package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubesetups")
public class CubeSetup {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex", unique = true))
    @Property String cubeID; // reference to EISScube.deviceID

    @Embedded CubeRelay relay;
    @Embedded CubeInput input;

}
