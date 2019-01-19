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

    @Indexed(options = @IndexOptions(name = "CubeDeviceIndex", unique = true))
    @Property String deviceID;
    @Property String password;
    @Property String simCard;

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

}
