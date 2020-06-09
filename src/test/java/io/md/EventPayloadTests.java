package io.md;


import com.fasterxml.jackson.databind.JsonNode;
import io.md.dao.ConvertEventPayloadResponse;
import io.md.dao.Payload;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.cryptography.JcaEncryption;
import io.md.dao.CubeMetaInfo;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.JsonByteArrayPayload;
import io.md.dao.JsonPayload;
import io.md.dao.MDTraceInfo;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.Utils;

public class EventPayloadTests {

		private Event httpRequestEvent;
		private Event httpHtmlResponseEvent;
		private Event httpJsonResponseEvent;
		private Event stringEvent;
		private Event byteArrayEvent;
		private Event fnReqRespEvent;
		private ObjectMapper objectMapper;
		private Event httpResponseEvent;

		@Before
		/**
		 * public EventBuilder(CubeMetaInfo cubeMetaInfo, MDTraceInfo mdTraceInfo,
		 * 			Event.RunType runType, String apiPath, EventType eventType,
		 * 			Optional<Instant> timestamp, String reqId, String collection)
		 */
		public void setUp() throws InvalidEventException {
			CubeMetaInfo cubeMetaInfo = new CubeMetaInfo("random-user"
				, "test", "movie-info", "mi-rest");
			MDTraceInfo traceInfo = new MDTraceInfo("random-trace"
				, null , null);
			EventBuilder eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPRequest
				, Optional.empty(), "random-req-id", "random-collection");

			MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
			headers.add("content-type" , MediaType.APPLICATION_JSON);
			MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
			queryParams.add("filmName" , "Beverly%20Outlaw");
			eventBuilder.setPayload(new HTTPRequestPayload(headers, queryParams,
				null, "GET", null, "a/b/c"));
			httpRequestEvent = eventBuilder.createEvent();


			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection");
			eventBuilder.setPayload(new HTTPResponsePayload(headers, 200
				,"{\"MIRest status\":\"MovieInfo is healthy\"}".getBytes()));
			httpJsonResponseEvent = eventBuilder.createEvent();

			headers = new MultivaluedHashMap<>();
			headers.add("content-type" , MediaType.TEXT_HTML);
			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection");
			eventBuilder.setPayload(new HTTPResponsePayload(headers, 200
				,"<html><meta></meta><body>Sample Body</body></html>".getBytes()));
			httpHtmlResponseEvent = eventBuilder.createEvent();

			String sampleJson = "{\"name\" : \"foo\" , \"age\" : { \"bar\" : 2} }";

			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection");
			eventBuilder.setPayload(new JsonPayload("{\"name\" : \"foo\" , \"age\" : "
				+ "{ \"bar\" : 2} }"));
			stringEvent = eventBuilder.createEvent();


			eventBuilder.setPayload(new JsonByteArrayPayload(sampleJson.getBytes()));
			byteArrayEvent = eventBuilder.createEvent();

			FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()) ,
				new Object[] {"firstArg" , Instant.now(), MediaType.TEXT_HTML, cubeMetaInfo},
				true , RetStatus.Success , Optional.empty());
			eventBuilder.setPayload(fnReqRespPayload);
			fnReqRespEvent = eventBuilder.createEvent();
			objectMapper = CubeObjectMapperProvider.getInstance();
			try {
				 Payload payload = objectMapper.readValue
						(new File("src/test/resources/jsonResponsePayloadData.json"), Payload.class);
				HTTPResponsePayload httpResponsePayload = (HTTPResponsePayload)payload;
				eventBuilder.setPayload(httpResponsePayload);
				httpResponseEvent = eventBuilder.createEvent();
			} catch (IOException e) {
			}
		}

		@Test
		public void testHttpRequestEvent() throws IOException {
			String fromInitialEvent = objectMapper.writeValueAsString(httpRequestEvent);
			System.out.println("INITIAL :: " + fromInitialEvent);
			Event fromSerialized = objectMapper.readValue(fromInitialEvent, Event.class);
			Assert.assertEquals(fromSerialized.customerId, "random-user");
			Assert.assertNotNull(fromSerialized.payload);
			Assert.assertEquals(fromSerialized.payload.getClass(), HTTPRequestPayload.class);
			HTTPRequestPayload payloadFromSerialized = (HTTPRequestPayload) fromSerialized.payload;
			Assert.assertEquals(payloadFromSerialized.
				hdrs.getFirst("content-type"), MediaType.APPLICATION_JSON);
			Assert.assertTrue(payloadFromSerialized.getBody() == null
				|| payloadFromSerialized.getBody().length == 0);
			Assert.assertTrue(payloadFromSerialized.formParams == null ||
				 payloadFromSerialized.formParams.isEmpty());
			Assert
				.assertEquals("Beverly%20Outlaw", payloadFromSerialized
					.queryParams.getFirst("filmName"));
			payloadFromSerialized.encryptField("/method" , new JcaEncryption());
			String postEncryption = objectMapper.writeValueAsString(fromSerialized);
			System.out.println("POST ENCRYPTION :: " + postEncryption);
			Event postEncryptionEvent = objectMapper.readValue(postEncryption, Event.class);
			Assert.assertNotEquals( ((HTTPRequestPayload)postEncryptionEvent.payload)
				.method , "GET");
		}

		@Test
		public void testHttpRequestTransform() throws IOException, PathNotFoundException {
			HTTPRequestPayload requestPayload = (HTTPRequestPayload) httpRequestEvent.payload;
			assert requestPayload != null;
			requestPayload.transformSubTree("/queryParams" , URLDecoder::decode);
			System.out.println("After transform :: " + objectMapper.writeValueAsString(httpRequestEvent));
			assert requestPayload.dataObj.getValAsString("/queryParams/filmName/0").equals("Beverly Outlaw");


		}

		@Test
		public void testHttpJsonResponseEvent() throws IOException, PathNotFoundException {
			String jsonResponseEventSerialized = objectMapper
				.writeValueAsString(httpJsonResponseEvent);
			System.out.println("INITIAL :: " + jsonResponseEventSerialized);
			Event event = objectMapper.readValue(jsonResponseEventSerialized, Event.class);
			HTTPResponsePayload payload =  (HTTPResponsePayload) event.payload;
			System.out.println(payload.getValAsString("/body"));
			Assert.assertEquals(payload.getValAsString("/body/MIRest status")
				, "MovieInfo is healthy");
			payload.encryptField("/body/MIRest status" , new JcaEncryption());
			String reSerialized = objectMapper.writeValueAsString(event);
			System.out.println("POST ENCRYPTION :: " + reSerialized);
			Event encrypted = objectMapper.readValue(reSerialized , Event.class);
			HTTPResponsePayload encryptedPayload  = (HTTPResponsePayload) encrypted.payload;
			//The value has been encrypted now
			Assert.assertNotEquals(encryptedPayload.getValAsString("/body/MIRest status")
				, "MovieInfo is healthy");
			System.out.println(encryptedPayload.getValAsString("/body"));
		}

		@Test
		public void testHttpHtmlResponseEvent() throws  IOException, PathNotFoundException {
			String jsonRespEventSerialized = objectMapper.writeValueAsString(httpHtmlResponseEvent);
			System.out.println("ONE SERIALIZATION :: " + jsonRespEventSerialized);
			Event event = objectMapper.readValue(jsonRespEventSerialized, Event.class);
			HTTPResponsePayload payload =  (HTTPResponsePayload) event.payload;
			System.out.println(new String(payload.getValAsString("/body")));
			System.out.println("SECOND SERIALIZATION :: " + objectMapper
				.writeValueAsString(payload));
			Assert.assertEquals(new String(payload.getValAsString("/body")),
				"<html><meta></meta><body>Sample Body</body></html>");
			//Assert.assertEquals(payload.getValAsString("/body"));
			payload.encryptField("/body" , new JcaEncryption());
			String reSerialized = objectMapper.writeValueAsString(event);
			System.out.println("POST ENCRYPTION :: " + reSerialized);
			Event encrypted = objectMapper.readValue(reSerialized , Event.class);
			HTTPResponsePayload encryptedPayload  = (HTTPResponsePayload) encrypted.payload;
			Assert.assertNotEquals(new String(encryptedPayload.getValAsString("/body"))
				, "<html><meta></meta><body>Sample Body</body></html>");
			System.out.println(new String( new JcaEncryption().decrypt(encryptedPayload
				.getValAsByteArray("/body")).orElse(new byte[]{0}) , "UTF-8"));
		}

		@Test
		public void testJsonResponseEvent() throws IOException, PathNotFoundException {
			String jsonRespEventSerialized = objectMapper.writeValueAsString(stringEvent);
			System.out.println("INITIAL :: " + jsonRespEventSerialized);
			Event event = objectMapper.readValue(jsonRespEventSerialized, Event.class);
			JsonPayload payload =  (JsonPayload) event.payload;
			Assert.assertEquals(payload.getValAsString("/name"),
				"foo");
			Assert.assertEquals(payload.getValAsString("/age/bar"),
				"2");
			payload.encryptField("/name" , new JcaEncryption());
			String reSerialized = objectMapper.writeValueAsString(event);
			System.out.println("POST ENCRYPTION :: " + reSerialized);
			Event encrypted = objectMapper.readValue(reSerialized , Event.class);
			JsonPayload encryptedPayload  = (JsonPayload) encrypted.payload;
			Assert.assertNotEquals(encryptedPayload.getValAsString("/name")
				, "foo");
			Assert.assertEquals(payload.getValAsString("/age/bar"),
				"2");
		}

		@Test
		public void testJsonByteArrayResponseEvent() throws IOException, PathNotFoundException {
			String jsonRespEventSerialized = objectMapper.writeValueAsString(byteArrayEvent);
			System.out.println("INITIAL :: " + jsonRespEventSerialized);
			Event event = objectMapper.readValue(jsonRespEventSerialized, Event.class);
			JsonByteArrayPayload payload =  (JsonByteArrayPayload) event.payload;
			Assert.assertEquals(payload.getValAsString("/name"),
				"foo");
			Assert.assertEquals(payload.getValAsString("/age/bar"),
				"2");
			payload.encryptField("/name" , new JcaEncryption());
			String reSerialized = objectMapper.writeValueAsString(event);
			System.out.println("POST ENCRYPTION :: " + reSerialized);
			Event encrypted = objectMapper.readValue(reSerialized , Event.class);
			JsonByteArrayPayload encryptedPayload  = (JsonByteArrayPayload) encrypted.payload;
			Assert.assertNotEquals(encryptedPayload.getValAsString("/name")
				, "foo");
			Assert.assertEquals(payload.getValAsString("/age/bar"),
				"2");
		}


		public void testFunctionReqRespEvent() throws IOException, PathNotFoundException {
			String eventJsonSerialized = objectMapper.writeValueAsString(fnReqRespEvent);
			System.out.println(eventJsonSerialized);
			Event event = objectMapper.readValue(eventJsonSerialized, Event.class);
			FnReqRespPayload paylaod = (FnReqRespPayload) event.payload;
			Assert.assertEquals(paylaod.argVals[0], "firstArg");
		}

		@Test
		public void testSizeOfPayload() throws IOException, PathNotFoundException {
			ConvertEventPayloadResponse resp = httpResponseEvent.checkAndConvertResponseToString
					(true, Arrays.asList("/body/0"), 100, "/body");
			JsonNode node = objectMapper.readTree(resp.getResponse());
			JsonNode body = node.get("body");
			Assert.assertTrue(body.isArray());
			Assert.assertEquals(1, body.size());
			Assert.assertTrue(resp.isTruncated());
			Assert.assertEquals("bookinfo", body.at("/0/app_s").asText());
			Assert.assertEquals("0f0de88e-1dae-4a19-af80-5f3cb0c3cffe", body.at("/0/id").asText());
		}

	@Test
	public void testPayloadForSmallSize() throws IOException, PathNotFoundException {
		ConvertEventPayloadResponse resp = httpResponseEvent.checkAndConvertResponseToString
				(true, Arrays.asList("/body/0"), 20, "/body");
		JsonNode node = objectMapper.readTree(resp.getResponse());
		JsonNode body = node.get("body");
		Assert.assertTrue(body.isArray());
		Assert.assertEquals(1, body.size());
		Assert.assertTrue(resp.isTruncated());
		Assert.assertEquals("bookinfo", body.at("/0/app_s").asText());
		Assert.assertEquals("", body.at("/0/id").asText());
	}
}
