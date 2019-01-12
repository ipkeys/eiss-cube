package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubetests")
public class CubeTest {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIDIndex", unique = true))
    @Property String deviceID; // reference to EISScube.deviceID

    @Property Integer ss;

    @Property Integer r1;
    @Property Integer r2;

    @Property Integer i1;
    @Property Integer i2;

}
