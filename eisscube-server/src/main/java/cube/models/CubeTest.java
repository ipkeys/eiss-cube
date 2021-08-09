package cube.models;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity(value = "cubetests", useDiscriminator = false)
public class CubeTest {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex"))
    @Property ObjectId cubeID; // reference to EISScube.id

    @Property Instant timestamp;

    @Property Integer r;
    @Property Integer i;

}
