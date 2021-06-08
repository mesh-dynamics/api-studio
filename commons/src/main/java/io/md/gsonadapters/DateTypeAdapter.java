package io.md.gsonadapters;

import java.lang.reflect.Type;
import java.sql.Date;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

	@Override
	public JsonElement serialize(Date date, Type type,
		JsonSerializationContext jsonSerializationContext) {
		return new JsonPrimitive(date.getTime());
	}

	@Override
	public Date deserialize(JsonElement json, Type type,
		JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		if (!(json instanceof JsonPrimitive)) {
			throw new JsonParseException("The date should be a long value");
		}

		return new Date(json.getAsLong());
	}
}
