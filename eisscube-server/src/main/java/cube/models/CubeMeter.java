package cube.models;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity(value = "cubemeters", useDiscriminator = false)
public class CubeMeter {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property ObjectId cubeID; // reference to EISScube.id

    @Indexed(options = @IndexOptions(name = "CubeMeterTimestampIndex"))
    @Property Instant timestamp;
    @Property Double value;

    @Property String type;

}
