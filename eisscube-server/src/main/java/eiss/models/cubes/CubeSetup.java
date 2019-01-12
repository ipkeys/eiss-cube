package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubesetups")
public class CubeSetup {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIDIndex", unique = true))
    @Property String deviceID; // reference to EISScube.deviceID

    @Embedded CubeRelay relay1;
    @Embedded CubeRelay relay2;

    @Embedded CubeInput input1;
    @Embedded CubeInput input2;

}
