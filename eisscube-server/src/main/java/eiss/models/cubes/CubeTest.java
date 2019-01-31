package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubetests")
public class CubeTest {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex", unique = true))
    @Property String cubeID; // reference to EISScube.deviceID

    @Property Integer ss;
    @Property Integer r;
    @Property Integer i;

}
