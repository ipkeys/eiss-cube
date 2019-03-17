package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.time.Instant;

import static java.lang.Boolean.FALSE;

@Data
@Entity("eisscubes")
public class EISScube {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeDeviceIDIndex", unique = true))
    @Property String deviceID; // SIM Card number provided by device
    @Property String name;

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

}
