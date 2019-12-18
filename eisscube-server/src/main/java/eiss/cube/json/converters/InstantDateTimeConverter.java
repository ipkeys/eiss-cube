package eiss.cube.json.converters;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

public class InstantDateTimeConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

    @Override
    public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(ISO_INSTANT.format(src));
    }

    @Override
    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return ISO_INSTANT.parse(json.getAsString(), Instant::from);
    }

}