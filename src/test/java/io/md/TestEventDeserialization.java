package io.md;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cryptography.JcaEncryption;
import io.md.dao.Event;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.JsonPayload;
import io.md.utils.CubeObjectMapperProvider;

public class TestEventDeserialization {

	public static void main(String[] args) {
		try {
			ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
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
			System.out.println(map.getValAsString("/body"));
			map.encryptField("/body" , new JcaEncryption());
			//System.out.println(new String(map.body));
			String reSerialized = jsonMapper.writeValueAsString(event);
			System.out.println(reSerialized);
			Event encrypted = jsonMapper.readValue(reSerialized , Event.class);
			HTTPResponsePayload encryptedPayload  = (HTTPResponsePayload) encrypted.payload;
			System.out.println(encryptedPayload);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
