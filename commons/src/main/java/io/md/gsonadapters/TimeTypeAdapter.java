package io.md.gsonadapters;

import java.lang.reflect.Type;
import java.sql.Time;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TimeTypeAdapter implements JsonSerializer<Time>, JsonDeserializer<Time> {

	@Override
	public JsonElement serialize(Time time, Type type,
		JsonSerializationContext jsonSerializationContext) {
		return new JsonPrimitive(time.getTime());
	}

	@Override
	public Time deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
		throws JsonParseException {
		if (!(json instanceof JsonPrimitive)) {
			throw new JsonParseException("The time should be a long value");
		}

		return new Time(json.getAsLong());
	}
}
