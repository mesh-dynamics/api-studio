package io.md;


import com.fasterxml.jackson.databind.JsonNode;

import io.md.cache.ProtoDescriptorCache.ProtoDescriptorKey;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey.Type;
import io.md.dao.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.Recording.RecordingType;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors.DescriptorValidationException;

import io.md.cryptography.JcaEncryption;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.injection.DynamicInjectionConfig;
import io.md.services.DSResult;
import io.md.services.DataStore;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.ProtoDescriptorCacheProvider;

public class EventPayloadTests {

		private Event httpRequestEvent;
		private Event httpHtmlResponseEvent;
		private Event httpJsonResponseEvent;
		private Event stringEvent;
		private Event byteArrayEvent;
		private Event fnReqRespEvent;
		private ObjectMapper objectMapper;
		private Event httpResponseEvent;
		private Event grpcRequestEvent;
		private Event grpcResponseEvent;

		private Event httpMultipartRequestEvent1;
		private Event httpMultipartRequestEvent2;
		private Event httpMultipartRequestEvent3;
		private Optional<ProtoDescriptorDAO> protoDescriptor;

		private void setUpProtoDescirptorCache() {
			DataStore dataStoreExp = new DataStore() {

				/**
				 * @param customerId
				 * @param app
				 * @param instanceId
				 * @return
				 */
				@Override
				public Optional<RecordOrReplay> getCurrentRecordOrReplay(String customerId,
					String app, String instanceId) {
					return Optional.empty();
				}

				@Override
				public DSResult<Event> getEvents(EventQuery eventQuery) {
					return null;
				}

				@Override
				public Optional<Event> getSingleEvent(EventQuery eventQuery) {
					return Optional.empty();
				}

				@Override
				public Optional<Event> getRespEventForReqEvent(Event reqEvent) {
					return Optional.empty();
				}

				/**
				 * @param reqId
				 * @return the matching response on the reqId
				 */
				@Override
				public Optional<Event> getResponseEvent(String reqId) {
					return Optional.empty();
				}

				@Override
				public CompareTemplate getTemplate(String customerId, String app, String service,
					String apiPath, String templateVersion, Type templateType,
					Optional<EventType> eventType, Optional<String> method, String recordingId)
					throws TemplateNotFoundException {
					return null;
				}

				/**
				 * @param customerId
				 * @param app
				 * @param version
				 * @return
				 */
				@Override
				public Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String customerId,
					String app, String version) {
					return Optional.empty();
				}

				@Override
				public Optional<Replay> getReplay(String replayId) {
					return Optional.empty();
				}

				@Override
				public Optional<Recording> getRecording(String recordingId) {
					return Optional.empty();
				}

				@Override
				public Optional<CustomerAppConfig> getAppConfiguration(String customer, String app) {
					return Optional.empty();
				}

				/**
				 * @param res
				 * @return
				 */
				@Override
				public boolean saveResult(ReqRespMatchResult res) {
					return false;
				}

				@Override
				public boolean save(Event event) {
					return false;
				}

				@Override
				public boolean saveReplay(Replay replay) {
					return false;
				}

				/**
				 * @param replay
				 * @return
				 */
				@Override
				public boolean deferredDelete(Replay replay) {
					return false;
				}

				@Override
				public Optional<ProtoDescriptorDAO> getLatestProtoDescriptorDAO(String customer, String app) {
					if ("CubeCorp".equals(customer) && "grpc".equals(app)) {
						String filePath = "src/test/resources/route_guide.desc";
						try {
							String content = new String(Base64.getEncoder().encode( Files.readAllBytes(Paths.get(filePath))));
							return Optional.of(new ProtoDescriptorDAO(customer, app, content));
						} catch (IOException e) {
							e.printStackTrace();
						} catch (DescriptorValidationException e) {
							e.printStackTrace();
						}
					}
					return Optional.empty();
				}
			};

			ProtoDescriptorCacheProvider.instantiateCache(dataStoreExp);

		}


		@Before
		/**
		 * public EventBuilder(CubeMetaInfo cubeMetaInfo, MDTraceInfo mdTraceInfo,
		 * 			Event.RunType runType, String apiPath, EventType eventType,
		 * 			Optional<Instant> timestamp, String reqId, String collection)
		 */
		public void setUp() throws InvalidEventException {
			setUpProtoDescirptorCache();
			protoDescriptor = ProtoDescriptorCacheProvider.getInstance().flatMap(protoDescriptorCache ->
				protoDescriptorCache.get(new ProtoDescriptorKey("CubeCorp",
					"grpc",  UUID.randomUUID().toString())));
			CubeMetaInfo cubeMetaInfo = new CubeMetaInfo("random-user"
				, "test", "movie-info", "mi-rest");
			MDTraceInfo traceInfo = new MDTraceInfo("random-trace"
				, null , null);
			EventBuilder eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPRequest
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);

			MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
			headers.add("content-type" , MediaType.APPLICATION_JSON);
			MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
			queryParams.add("filmName" , "Beverly%20Outlaw");
			eventBuilder.setPayload(new HTTPRequestPayload(headers, queryParams,
				null, "GET", null, "a/b/c"));
			httpRequestEvent = eventBuilder.createEvent();


			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);
			eventBuilder.setPayload(new HTTPResponsePayload(headers, 200
				,"{\"MIRest status\":\"MovieInfo is healthy\"}".getBytes()));
			httpJsonResponseEvent = eventBuilder.createEvent();

			headers = new MultivaluedHashMap<>();
			headers.add("content-type" , MediaType.TEXT_HTML);
			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);
			eventBuilder.setPayload(new HTTPResponsePayload(headers, 200
				,"<html><meta></meta><body>Sample Body</body></html>".getBytes()));
			httpHtmlResponseEvent = eventBuilder.createEvent();

			String sampleJson = "{\"name\" : \"foo\" , \"age\" : { \"bar\" : 2} }";

			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, "/minfo/health", EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);
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

			try {
				Payload payload = objectMapper
					.readValue(new File("src/test/resources/grpcRequestPayload.json"), Payload.class);
				GRPCRequestPayload grpcRequestPayload = (GRPCRequestPayload) payload;
				eventBuilder.setPayload(grpcRequestPayload);
				grpcRequestEvent = eventBuilder.createEvent();
			}  catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Payload payload = objectMapper
					.readValue(new File("src/test/resources/grpcResponsePayload.json"), Payload.class);
				GRPCResponsePayload grpcResponsePayload = (GRPCResponsePayload) payload;
				eventBuilder.setPayload(grpcResponsePayload);
				grpcResponseEvent = eventBuilder.createEvent();
			}  catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Payload payload = objectMapper
					.readValue(new File("src/test/resources/httpMultipart_1.json"), Payload.class);
				eventBuilder.setPayload(payload);
				httpMultipartRequestEvent1 = eventBuilder.createEvent();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Payload payload = objectMapper
					.readValue(new File("src/test/resources/httpMultipart_2.json"), Payload.class);
				eventBuilder.setPayload(payload);
				httpMultipartRequestEvent2 = eventBuilder.createEvent();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Payload payload = objectMapper
				.readValue(new File("src/test/resources/httpMultipart_3.json"), Payload.class);
				eventBuilder.setPayload(payload);
				httpMultipartRequestEvent3 = eventBuilder.createEvent();
			} catch (Exception e) {
				e.printStackTrace();
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
				getHdrs().getFirst("content-type"), MediaType.APPLICATION_JSON);
			Assert.assertTrue(payloadFromSerialized.getBody() == null
				|| payloadFromSerialized.getBody().length == 0);
			Assert.assertTrue(payloadFromSerialized.getFormParams() == null ||
				 payloadFromSerialized.getFormParams().isEmpty());
			Assert
				.assertEquals("Beverly%20Outlaw", payloadFromSerialized
					.getQueryParams().getFirst("filmName"));
			payloadFromSerialized.encryptField("/method" , new JcaEncryption());
			String postEncryption = objectMapper.writeValueAsString(fromSerialized);
			System.out.println("POST ENCRYPTION :: " + postEncryption);
			Event postEncryptionEvent = objectMapper.readValue(postEncryption, Event.class);
			assert postEncryptionEvent.payload != null;
			Assert.assertNotEquals( ((HTTPRequestPayload)postEncryptionEvent.payload)
				.getMethod() , "GET");
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
			ConvertEventPayloadResponse resp = httpResponseEvent.payload.checkAndConvertResponseToString
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
		ConvertEventPayloadResponse resp = httpResponseEvent.payload.checkAndConvertResponseToString
				(true, Arrays.asList("/body/0"), 20, "/body");
		JsonNode node = objectMapper.readTree(resp.getResponse());
		JsonNode body = node.get("body");
		Assert.assertTrue(body.isArray());
		Assert.assertEquals(1, body.size());
		Assert.assertTrue(resp.isTruncated());
		Assert.assertEquals("bookinfo", body.at("/0/app_s").asText());
		Assert.assertEquals("", body.at("/0/id").asText());
	}

	@Test
	public void testGRPCRequestEvent() throws IOException, PathNotFoundException {
		String serialized = objectMapper.writeValueAsString(grpcRequestEvent);
		Event reRead = objectMapper.readValue(serialized, Event.class);
		GRPCRequestPayload payload = (GRPCRequestPayload) reRead.payload;
		payload.setProtoDescriptor(protoDescriptor);
		Assert.assertEquals(payload.getValAsString("/path"), "routeguide.RouteGuide/GetFeature");
		Assert.assertEquals(payload.getValAsString("/body/latitude"), "409146138");
		System.out.println("GRPC (POST UNWRAP) :: " +objectMapper.writeValueAsString(payload));
		payload.wrapBodyAndEncode();
		System.out.println("GRPC (POST WRAP) :: " +objectMapper.writeValueAsString(payload));
		System.out.println(payload.getValAsString("/body"));
		Assert.assertEquals(payload.getValAsString("/body"), "CJqmjMMBEJafmJz9/////wE=");
	}

	@Test
	public void testGRPCResponseEvent() throws IOException, PathNotFoundException {
		String serialized = objectMapper.writeValueAsString(grpcResponseEvent);
		Event reRead = objectMapper.readValue(serialized, Event.class);
		GRPCResponsePayload payload = (GRPCResponsePayload) reRead.payload;
		payload.setProtoDescriptor(protoDescriptor);
		Assert.assertEquals(payload.getValAsString("/path"), "routeguide.RouteGuide/GetFeature");
		Assert.assertEquals(payload.getValAsString("/body/name"), "Berkshire Valley Management Area Trail, Jefferson, NJ, USA");
		System.out.println("GRPC (POST UNWRAP) :: " +objectMapper.writeValueAsString(payload));
		payload.wrapBodyAndEncode();
		System.out.println("GRPC (POST WRAP) :: " +objectMapper.writeValueAsString(payload));
		System.out.println(payload.getValAsString("/body"));
		Assert.assertEquals(payload.getValAsString("/body"), "CjpCZXJrc2hpcmUgVmFsbGV5IE1hbmFnZW1lbnQgQXJlYSBUcmFpbCwgSmVmZmVyc29uLCBOSiwgVVNBEhEImqaMwwEQlp+YnP3/////AQ==");
	}


	@Test
	public void testHttpMultipartRequestEvent() throws IOException, PathNotFoundException {
		String serialized = objectMapper.writeValueAsString(httpMultipartRequestEvent1);
		//System.out.println(serialized);
		Event reRead = objectMapper.readValue(serialized, Event.class);
		HTTPRequestPayload payload = (HTTPRequestPayload) reRead.payload;
		Assert.assertEquals(payload.getValAsString("/body/tp/value") , "as");
		payload.wrapAsString("/body" , payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		payload.dataObj.unwrapAsJson("/body",payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));
		payload.wrapAsString("/body" , payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));

		serialized = objectMapper.writeValueAsString(httpMultipartRequestEvent2);
		//System.out.println(serialized);
		reRead = objectMapper.readValue(serialized, Event.class);
		payload = (HTTPRequestPayload) reRead.payload;
		Assert.assertEquals(payload.getValAsString("/body/link/value") , "RELATIONSHIP_AGREEMENT");
		payload.wrapAsString("/body" , payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));
		payload.dataObj.unwrapAsJson("/body",payload.getValAsString("/hdrs/content-type/0"), Optional.empty());


		serialized = objectMapper.writeValueAsString(httpMultipartRequestEvent3);
		//System.out.println(serialized);
		reRead = objectMapper.readValue(serialized, Event.class);
		payload = (HTTPRequestPayload) reRead.payload;
		Assert.assertEquals(payload.getValAsString("/body/link/value") , "RELATIONSHIP_AGREEMENT");
		payload.wrapAsString("/body" , payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));
		payload.dataObj.unwrapAsJson("/body",payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));
		payload.dataObj.wrapAsString("/body" , payload.getValAsString("/hdrs/content-type/0"), Optional.empty());
		//System.out.println(payload.getValAsString("/body"));




	}
}
