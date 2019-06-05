package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import dev.morphia.annotations.*;

@Data
@Entity(value = "cubereports", noClassnameStored = true)
public class CubeReport {

    @Id ObjectId id;

    @Property ObjectId cubeID; // reference to EISScube.id

    @Indexed(options = @IndexOptions(name = "CubeReportIndex", unique = true))
    @Property String reportID;

}
