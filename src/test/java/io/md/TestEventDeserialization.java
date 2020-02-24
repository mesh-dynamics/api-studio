package io.md;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.dao.AbstractMDPayload;
import io.md.dao.Event;
import io.md.dao.HTTPResponsePayload;

public class TestEventDeserialization {

	public static void main(String[] args) {
		try {
			ObjectMapper jsonMapper = new ObjectMapper();
			jsonMapper.registerModule(new Jdk8Module());
			jsonMapper.registerModule(new JavaTimeModule());
			//String cubeEventJson = "{\"customerId\":\"cube\",\"app\":\"app\",\"service\":\"wrapper\",\"instanceId\":\"prod\",\"collection\":\"NA\",\"traceId\":\"f0fc021df2da4049\",\"runType\":\"Record\",\"timestamp\":{\"nano\":467668000,\"epochSecond\":1582512103},\"reqId\":\"wrapper-f0fc021df2da4049-45843449-5f80-49e1-b329-22e85cd115bc\",\"apiPath\":\"/minfo/listmovies\",\"eventType\":\"HTTPResponse\",\"rawPayloadObject\":{\"hdrs\":{\"content-type\":[\"application/json\"]},\"status\":200,\"body\":\"W3siYWN0b3JzX2xhc3RuYW1lcyI6WyJTVVZBUkkiLCJQRUNLIiwiREVHRU5FUkVTIiwiQ1JVWiJdLCJkaXNwbGF5X2FjdG9ycyI6WyJERUdFTkVSRVMsSk9ESUUiLCJTVVZBUklKT0hOIiwiQ1JVWixSQUxQSCJdLCJmaWxtX2lkIjo2OSwidGl0bGUiOiJCRVZFUkxZIE9VVExBVyIsImZpbG1fY291bnRzIjpbIjI5IiwiMTkiLCIyOSIsIjI4Il0sInRpbWVzdGFtcCI6ODkyNDg0NjYxNTE5NzIsImJvb2tfaW5mbyI6eyJyZXZpZXdzIjp7InJldmlld3MiOlt7InJldmlld2VyIjoiUmV2aWV3ZXIxIiwidGV4dCI6IkFuIGV4dHJlbWVseSBlbnRlcnRhaW5pbmcgcGxheSBieSBTaGFrZXNwZWFyZS4gVGhlIHNsYXBzdGljayBodW1vdXIgaXMgcmVmcmVzaGluZyEifSx7InJldmlld2VyIjoiUmV2aWV3ZXIyIiwidGV4dCI6IkFic29sdXRlbHkgZnVuIGFuZCBlbnRlcnRhaW5pbmcuIFRoZSBwbGF5IGxhY2tzIHRoZW1hdGljIGRlcHRoIHdoZW4gY29tcGFyZWQgdG8gb3RoZXIgcGxheXMgYnkgU2hha2VzcGVhcmUuIn1dLCJpZCI6IjY5In0sInJhdGluZ3MiOnsicmF0aW5ncyI6eyJSZXZpZXdlcjIiOjQsIlJldmlld2VyMSI6NX0sImlkIjo2OX0sImRldGFpbHMiOnsicGFnZXMiOjIwMCwieWVhciI6MTU5NSwiYXV0aG9yIjoiV2lsbGlhbSBTaGFrZXNwZWFyZSIsIklTQk4tMTMiOiIxMjMtMTIzNDU2Nzg5MCIsInB1Ymxpc2hlciI6IlB1Ymxpc2hlckEiLCJJU0JOLTEwIjoiMTIzNDU2Nzg5MCIsImxhbmd1YWdlIjoiRW5nbGlzaCIsImlkIjo2OSwidHlwZSI6InBhcGVyYmFjayJ9fX1d\"}}";
			String cubeEventJson = "{\"customerId\":\"cube\",\"app\":\"app\",\"service\":\"wrapper\",\"instanceId\":\"prod\",\"collection\":\"NA\",\"traceId\":\"e287ff135896fe37\",\"runType\":\"Record\",\"reqId\":\"wrapper-e287ff135896fe37-6e1ec451-4082-4ba4-86e1-21b358e1a703\",\"apiPath\":\"/minfo/health\",\"eventType\":\"HTTPResponse\",\"rawPayloadObject\":{\"type\":\"HTTPResponsePayload\",\"hdrs\":{\"content-type\":[\"application/json\"]},\"status\":200,\"body\":\"eyJNSVJlc3Qgc3RhdHVzIjogIk1vdmllSW5mbyBpcyBoZWFsdGh5In0=\"}}";
			Event event = jsonMapper.readValue(cubeEventJson, Event.class);

			HTTPResponsePayload map =  (HTTPResponsePayload) event.rawPayloadObject;

			System.out.println(new String(map.body));

			//HTTPResponsePayload responsePayload =  (HTTPResponsePayload) event.rawPayloadObject;
			//System.out.println(new String(responsePayload.body));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}
