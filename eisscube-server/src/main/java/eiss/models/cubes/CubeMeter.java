package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

import java.time.Instant;

@Data
@Entity("cubemeters")
public class CubeMeter {

    @Id ObjectId id;

    @Property ObjectId cubeID; // reference to EISScube.id

    @Indexed(options = @IndexOptions(name = "CubeMeterTimestampIndex"))
    @Property Instant timestamp;
    @Property Double value;

    @Property String type;

}
