package eiss.models.eiss;

import dev.morphia.annotations.*;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.CollationStrength.SECONDARY;

@Data
@Entity(value = "groups", noClassnameStored = true)
@Indexes({
        @Index(fields = @Field("name"), options = @IndexOptions(name = "GroupNameSortedIndex", unique = true,
            collation = @Collation(locale = "en", strength = SECONDARY))),
        @Index(fields = @Field("displayName"), options = @IndexOptions(name = "GroupDisplayNameSortedIndex",
                collation = @Collation(locale = "en", strength = SECONDARY))
        )
})
public final class Group implements DbEntity {

    @Id ObjectId id;
    @Property String name;
    @Property String displayName;
    @Property String role = "user"; // or "administrator"

    @Property Map<String, String> properties = new HashMap<>();
    @Embedded Map<EventState, SubjectAndBody> templates = new HashMap<>();
    @Embedded SubjectAndBody notificationTemplates;

    @Property String primaryNotificationList;
    @Property String secondaryNotificationList;

}
