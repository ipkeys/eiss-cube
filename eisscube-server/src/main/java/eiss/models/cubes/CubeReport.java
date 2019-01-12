package eiss.models.cubes;

import lombok.Data;
import org.bson.types.ObjectId;
import xyz.morphia.annotations.*;

@Data
@Entity("cubereports")
public class CubeReport {

    @Id ObjectId id;

    @Property String deviceID; // reference to EISScube.deviceID

    @Indexed(options = @IndexOptions(name = "CubeReportIndex", unique = true))
    @Property String reportID;

}
