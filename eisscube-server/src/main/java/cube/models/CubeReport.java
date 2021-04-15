package cube.models;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

@Data
@Entity(value = "cubereports", useDiscriminator = false)
public class CubeReport {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property ObjectId cubeID; // reference to EISScube.id

    @Indexed(options = @IndexOptions(name="ReportGroupIndex"))
    @Property String group;

    @Indexed(options = @IndexOptions(name="reportGroupIdIndex"))
    @Property String group_id;

    String cubeName;
    @Property String type;

}
