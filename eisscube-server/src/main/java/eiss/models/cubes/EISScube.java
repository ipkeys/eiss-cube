package eiss.models.cubes;

import com.mongodb.BasicDBObject;
import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.time.Instant;

import static java.lang.Boolean.FALSE;

@Data
@Entity(value = "eisscubes", noClassnameStored = true)
public class EISScube {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIDIndex", unique = true))
    @Property String deviceID; // SIM Card number provided by device
    @Property String name;
    @Property String socket;

    @Indexed(options = @IndexOptions(name="CubeGroupIndex"))
    @Property String group;

    @Indexed(options = @IndexOptions(name="CubeGroupIdIndex"))
    @Property String group_id;

    @Property String address;
    @Property String city;
    @Property String zipCode;
    @Embedded CubePoint location;

    @Property String customerID;
    @Property String zone;
    @Property String subZone;

    @Property Boolean online = FALSE;
    @Property Instant lastPing;
    @Property Instant timeStarted;

    @Property Integer signalStrength;
    @Embedded BasicDBObject settings;

}
