package eiss.models.eiss;

import dev.morphia.annotations.*;
import eiss.utils.PasswordHash;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;

import static com.mongodb.client.model.CollationStrength.SECONDARY;

@Slf4j
@Data
@Entity(value = "users", noClassnameStored = true)
@Indexes({
    @Index(fields = @Field("name"), options = @IndexOptions(name = "UserNameIndex", unique = true)),
    @Index(fields = @Field("displayName"), options = @IndexOptions(name = "UserDisplayNameSortedIndex",
        collation = @Collation(locale = "en", strength = SECONDARY))),
})
public final class User implements DbEntity {

    @Id ObjectId id;
    @Property String name;
    @Property String displayName;
    @Property String role;

    @Property String authType;
    @Property String fingerprint;
    @Property String hashedPassword;
    @Property ArrayList<String> oldPasswords = new ArrayList<>();

    @Property Instant passwordChanged;

    @Indexed(options = @IndexOptions(name="UserGroupIndex"))
    @Property String group;

    @Indexed(options = @IndexOptions(name="UserGroupIdIndex"))
    @Property String group_id;

    @Indexed(options = @IndexOptions(name="UserEmailIndex", unique=true, sparse=true))
    @Property String email;

    @Property String mfaSecret;

    @Property boolean eulaAccepted = Boolean.FALSE;
    @Property boolean accountLocked = Boolean.FALSE;

    public static int minimumResetDays = 1;

    public static int expirePasswordDays = 90;

    public static String encryptPassword(String pwd) {
        String rc = "";

        try {
            rc = PasswordHash.createHash(pwd);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error(e.getMessage());
        }
        return rc;
    }

    public static boolean comparePassword(String pass, String hash) {
        boolean rc = Boolean.FALSE;

        try {
            rc = PasswordHash.validatePassword(pass, hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {}

        return rc;
    }

    public static String stripFingerprint(String fingerprint) {
        return fingerprint
            .replace(".", "")
            .replace(":", "")
            .replace("-", "");
    }
}
