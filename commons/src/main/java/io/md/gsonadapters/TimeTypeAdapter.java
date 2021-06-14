/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
