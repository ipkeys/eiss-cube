package eiss.models;

import com.mongodb.BasicDBObject;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.CollationStrength.SECONDARY;

@Data
@Entity(value = "groups", useDiscriminator = false)
@Indexes({
        @Index(fields = @Field("name"), options = @IndexOptions(name = "GroupNameSortedIndex", unique = true,
            collation = @Collation(locale = "en", strength = SECONDARY))),
        @Index(fields = @Field("displayName"), options = @IndexOptions(name = "GroupDisplayNameSortedIndex",
                collation = @Collation(locale = "en", strength = SECONDARY))
        )
})
public final class Group {

    @Id ObjectId id;
    @Property String name;
    @Property String displayName;

    @Property Map<String, String> properties = new HashMap<>();
    @Property Map<EventState, SubjectAndBody> templates = new HashMap<>();
    @Property SubjectAndBody notificationTemplates;

    @Property String primaryNotificationList;
    @Property String secondaryNotificationList;

    @Property BasicDBObject settings;
    long numUsers = 0;

}
