package io.cube.agent;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonPatternDeserializer implements JsonSerializer<Pattern>, JsonDeserializer<Pattern> {


    @Override
    public Pattern deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return  Pattern.compile(jsonElement.getAsJsonPrimitive().getAsString());
    }

    @Override
    public JsonElement serialize(Pattern pattern, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(pattern.pattern());
    }
}
