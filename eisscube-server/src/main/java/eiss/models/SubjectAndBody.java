package eiss.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Entity(useDiscriminator = false)
public class SubjectAndBody {

    @Property String primarySubject;
    @Property String primaryBody;

}
