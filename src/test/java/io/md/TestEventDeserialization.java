package io.md;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.cryptography.JcaEncryption;
import io.md.dao.Event;
import io.md.dao.HTTPResponsePayload;
import io.md.utils.LazyParseAbstractPayloadSerializerModifier;

public class TestEventDeserialization {

	public static void main(String[] args) {
		try {
			ObjectMapper jsonMapper = new ObjectMapper();
			jsonMapper.registerModule(new Jdk8Module());
			jsonMapper.registerModule(new JavaTimeModule());
			jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			SimpleModule module = new SimpleModule();
			module.setSerializerModifier(new LazyParseAbstractPayloadSerializerModifier());
			jsonMapper.registerModule(module);
			String cubeEventJson = "{\"customerId\":\"cube\",\"app\":\"app\""
				+ ",\"service\":\"wrapper\",\"instanceId\":\"prod\",\"collection\":\"NA\","
				+ "\"traceId\":\"e287ff135896fe37\",\"runType\":\"Record\""
				+ ",\"reqId\":\"wrapper-e287ff135896fe37-6e1ec451-4082-4ba4-86e1-21b358e1a703\""
				+ ",\"apiPath\":\"/minfo/health\",\"eventType\":\"HTTPResponse\""
				+ ",\"payload\":[\"HTTPResponsePayload\", {"
				+ "\"hdrs\":{\"content-type\":[\"application/json\"]},\"status\":200"
				+ ",\"body\":\"eyJNSVJlc3Qgc3RhdHVzIjogIk1vdmllSW5mbyBpcyBoZWFsdGh5In0=\"}]}";
			Event event = jsonMapper.readValue(cubeEventJson, Event.class);

			HTTPResponsePayload map =  (HTTPResponsePayload) event.payload;
			map.setObjectMapper(jsonMapper);
			System.out.println(map.getValAsString("/body/MIRest status"));
			map.encryptField("/body/MIRest status" , new JcaEncryption());
			System.out.println(new String(map.body));

			//map.syncFromDataObj();
			String reSerialized = jsonMapper.writeValueAsString(event);
			System.out.println(reSerialized);
			Event encrypted = jsonMapper.readValue(reSerialized , Event.class);
			HTTPResponsePayload encryptedPayload  = (HTTPResponsePayload) encrypted.payload;
			System.out.println(new String(encryptedPayload.body, StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
