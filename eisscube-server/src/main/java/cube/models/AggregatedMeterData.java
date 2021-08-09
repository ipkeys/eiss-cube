package cube.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
public class AggregatedMeterData {

    @Id Instant id;
    @Property Double value;

}
