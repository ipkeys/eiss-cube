package eiss.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provider;
import eiss.json.converters.InstantDateTimeConverter;
import eiss.json.converters.ObjectIdConverter;
import org.bson.types.ObjectId;

import java.time.Instant;

public class GsonProvider implements Provider<Gson> {

    private static Gson gson;

    public Gson get() {
        if (gson == null) {
            // if you need a customized Gson object - do it here
            gson = new GsonBuilder()
                .registerTypeAdapter(ObjectId.class, new ObjectIdConverter())
                .registerTypeAdapter(Instant.class, new InstantDateTimeConverter())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
        }
        return gson;
    }

}
