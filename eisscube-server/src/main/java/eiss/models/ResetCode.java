package eiss.models;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embedded(useDiscriminator = false)
public class ResetCode {

    @Property String code;
    @Property Instant created;

}
