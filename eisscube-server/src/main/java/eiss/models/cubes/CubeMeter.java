package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.util.Date;

@Data
@Entity("cubemeters")
public class CubeMeter {

    @Id ObjectId id;

    @Property String reportID; // reference to EISScube.deviceID + _Meter_#

    @Indexed(options = @IndexOptions(name = "CubeMeterTimestampIndex", unique = false))
    @Property Date timestamp;
    @Property Double value;

    @Property String type;

}
