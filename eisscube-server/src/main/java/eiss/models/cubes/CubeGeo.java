package eiss.models.cubes;

import com.mongodb.BasicDBObject;
import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubegeometry")
public class CubeGeo {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIndex", unique = false))
    @Property String deviceID; // reference to EISScube.deviceID

    @Embedded BasicDBObject geo;

}
