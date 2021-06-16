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

package com.cube;

import static io.md.core.TemplateKey.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.Comparator;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.Event.EventType;

import com.cube.ws.Config;

public class EventPayloadKeyTest {

	public static void main(String[] args) {
		try {
			Config config = new Config();
			ObjectMapper jsonMapper = config.jsonMapper;
			String cubeEventJson = "{\"customerId\":\"cube\",\"app\":\"app\""
				+ ",\"service\":\"wrapper\",\"instanceId\":\"prod\",\"collection\":\"NA\","
				+ "\"traceId\":\"e287ff135896fe37\",\"runType\":\"Record\""
				+ ",\"reqId\":\"wrapper-e287ff135896fe37-6e1ec451-4082-4ba4-86e1-21b358e1a703\""
				+ ",\"apiPath\":\"/minfo/health\",\"eventType\":\"HTTPResponse\""
				+ ",\"payload\":[\"HTTPResponsePayload\", {"
				+ "\"hdrs\":{\"content-type\":[\"application/json\"]},\"status\":200"
				+ ",\"body\":\"\"}]}";
			Event event = jsonMapper.readValue(cubeEventJson, Event.class);
			TemplateKey key = new TemplateKey("templateVersion", "customerId", "app", "service", "path", Type.ResponseCompare);
			Comparator comparator = null;
            comparator = config.rrstore.getComparator(key , EventType.HTTPResponse);
			event.parseAndSetKey(comparator.getCompareTemplate());
			System.out.println(event.payloadKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
