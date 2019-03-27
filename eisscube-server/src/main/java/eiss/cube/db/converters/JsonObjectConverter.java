package eiss.cube.db.converters;

import com.mongodb.BasicDBObject;
import io.vertx.core.json.JsonObject;
import xyz.morphia.converters.SimpleValueConverter;
import xyz.morphia.converters.TypeConverter;
import xyz.morphia.mapping.MappedField;

public class JsonObjectConverter extends TypeConverter implements SimpleValueConverter {

    public JsonObjectConverter() {
        super(JsonObject.class);
    }

    @Override
    public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
        if (val == null) {
            return null;
        }

        if (val instanceof JsonObject) {
            return val;
        }

        if (val instanceof BasicDBObject) {
            return new JsonObject(((BasicDBObject) val).toJson());
        }

        throw new IllegalArgumentException("Can't convert to JsonObject from " + val);
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
        if (value == null) {
            return null;
        }
        JsonObject o = (JsonObject) value;
        return BasicDBObject.parse(o.toString());
    }
}