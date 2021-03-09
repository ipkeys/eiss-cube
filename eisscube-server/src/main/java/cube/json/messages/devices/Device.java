package cube.json.messages.devices;

import com.mongodb.BasicDBObject;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Device {

    private String id;
    private String ICCID;
    private String name;
    private Boolean online;
    private Instant timeStarted;
    private Instant lastPing;
    private Integer signalStrength;

    private String address;
    private String city;
    private String zipCode;

    private String customerID;
    private String zone;
    private String subZone;

    private Location location;
    private BasicDBObject settings;

}
