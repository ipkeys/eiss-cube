package eiss.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity(useDiscriminator = false)
public class LoginAttempts {

    @Property int attempts;
    @Property Instant last;

}
