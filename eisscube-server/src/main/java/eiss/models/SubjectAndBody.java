package eiss.models;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Embedded(useDiscriminator = false)
public class SubjectAndBody {

    @Property String primarySubject;
    @Property String primaryBody;

}
