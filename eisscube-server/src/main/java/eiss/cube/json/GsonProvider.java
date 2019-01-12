package eiss.cube.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provider;
import eiss.cube.json.converters.ObjectIdConverter;
import org.bson.types.ObjectId;

public class GsonProvider implements Provider<Gson> {

    private static Gson gson;

    public Gson get() {
        if (gson == null) {
            // if you need a customized Gson object - do it here
            gson = new GsonBuilder()
                .registerTypeAdapter(ObjectId.class, new ObjectIdConverter())
                .create();
        }
        return gson;
    }

}
