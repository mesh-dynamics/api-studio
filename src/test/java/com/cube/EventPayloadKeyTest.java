package com.cube;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.Comparator;
import io.md.dao.Event;
import io.md.dao.Event.EventType;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.cache.TemplateKey.Type;
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
			ComparatorCache comparatorCache = config.comparatorCache;
			comparator = comparatorCache.getComparator(key , EventType.HTTPResponse);
			event.parseAndSetKey(comparator.getCompareTemplate());
			System.out.println(event.payloadKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
