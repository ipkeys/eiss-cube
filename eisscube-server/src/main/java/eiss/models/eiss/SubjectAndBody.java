package eiss.models.eiss;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Property;
import lombok.Data;

@Data
@Embedded
public class SubjectAndBody {

    @Property String primarySubject;
    @Property String primaryBody;

}
