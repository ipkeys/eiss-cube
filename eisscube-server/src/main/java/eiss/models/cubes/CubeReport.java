package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubereports")
public class CubeReport {

    @Id ObjectId id;

    @Property String cubeID; // reference to EISScube.id

    @Indexed(options = @IndexOptions(name = "CubeReportIndex", unique = true))
    @Property String reportID;

}
