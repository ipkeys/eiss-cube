package cube.models;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity(value = "cubereports", useDiscriminator = false)
public class CubeReport {

    @Id ObjectId id;

    @Property String deviceType;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property ObjectId cubeID; // reference to device by id - can be EISScube or LORAcube
    @Property String cubeName;

    @Indexed(options = @IndexOptions(name="ReportGroupIndex"))
    @Property String group;

    @Indexed(options = @IndexOptions(name="reportGroupIdIndex"))
    @Property String group_id;

    @Property String type;
    @Property String edge; // for type == "c" - which edge of start cycle (falling or raising)
    @Property Instant ts; // for type == "c" - timestamp of start cycle
}
