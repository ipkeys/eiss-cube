package eiss.models.cubes;

import com.mongodb.BasicDBObject;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Property;
import lombok.Data;
import org.bson.types.ObjectId;

import java.time.Instant;

import static java.lang.Boolean.FALSE;

@Data
@Entity(value = "loracubes", noClassnameStored = true)
public class LORAcube {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIDIndex", unique = true))
    @Property String deviceID; // SIM Card number provided by device
    @Property String deviceType = "l";
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
