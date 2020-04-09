package io.md;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cryptography.JcaEncryption;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.Payload;
import io.md.utils.CubeObjectMapperProvider;

public class TestEventDeserialization {

	public static void main(String[] args) {
		try {
			ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
			Payload payload = jsonMapper.readValue
				("[\"HTTPRequestPayload\",{\"hdrs\":{\"sec-fetch-mode\":[\"cors\"],\"x-b3-parentspanid\":[\"e3f8c3e5661028f62f366002d5995b77\"],\"referer\":[\"https://sdlitedev2.strikedeck.com/customerdetails?id=5e7cc90c476ab677853ae387\"],\"content-length\":[\"282\"],\"sec-fetch-site\":[\"same-site\"],\"accept-language\":[\"en-US,en;q=0.9\"],\"origin\":[\"https://sdlitedev2.strikedeck.com\"],\"accept\":[\"*/*\"],\"authorization\":[\"ZUdseVRWUmtVQ3RSTlhSQlF6QjBVbU5tVm5rdlJrUjBURm96ZFhkT2JtdFJjVU5aVXpVNVpVaFJSVEZOT1ZocmQzWnpZbXRCUFQwdExVMXZlbWxzYkdFdk5TNHdJQ2hOWVdOcGJuUnZjMmc3SUVsdWRHVnNJRTFoWXlCUFV5QllJREV3WHpFMFh6WXBJRUZ3Y0d4bFYyVmlTMmwwTHpVek55NHpOaUFvUzBoVVRVd3NJR3hwYTJVZ1IyVmphMjhwSUVOb2NtOXRaUzg0TUM0d0xqTTVPRGN1TVRNeUlGTmhabUZ5YVM4MU16Y3VNelk9\"],\"x-b3-traceid\":[\"e3f8c3e5661028f62f366002d5995b77\"],\"x-b3-spanid\":[\"e3f8c3e5661028f62f366002d5995b77\"],\"host\":[\"sdlitedev2-api.strikedeck.com\"],\"connection\":[\"keep-alive\"],\"content-type\":[\"application/x-www-form-urlencoded; charset=UTF-8\"],\"accept-encoding\":[\"gzip, deflate, br\"],\"user-agent\":[\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36\"],\"sec-fetch-dest\":[\"empty\"]},\"queryParams\":{},\"formParams\":{},\"method\":\"POST\",\"body\":\"its=true&sort=assignedAt&order=false&createBSTable=true&entity=PlaybookStatus&listIdentifier=%23pills-5c46eb2898f90a108c4477f4+%23pod_93_5ce7cef198f90a72d6796d91&podId=5ce7cef198f90a72d6796d91&filter___sCustomerId=5e7cc90c476ab677853ae387&remove_filters%5B%5D=filter_as_pod&sa=false\"}]", Payload.class);

			HTTPRequestPayload payload1 = (HTTPRequestPayload) payload;

			/*String cubeEventJson = "{\"customerId\":\"cube\",\"app\":\"app\""
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
			System.out.println(encryptedPayload);*/
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
