package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity("cubetests")
public class CubeTest {

    @Id ObjectId id;

    @Indexed(options = @IndexOptions(name = "CubeIDIndex", unique = false))
    @Property ObjectId cubeID; // reference to EISScube.id

    @Property Instant timestamp;

    @Property Integer r;
    @Property Integer i;

}
