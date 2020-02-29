package io.md;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.CubeMetaInfo;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.MDTraceInfo;
import io.md.utils.CubeObjectMapperProvider;

public class EventPayloadTests {

		private Event httpRequestEvent;
		private Event httpHtmlResponseEvent;
		private Event httpJsonResponseEvent;
		private Event stringEvent;
		private Event byteArrayEvent;
		private ObjectMapper objectMapper;

		@Before
		/**
		 * public EventBuilder(CubeMetaInfo cubeMetaInfo, MDTraceInfo mdTraceInfo,
		 * 			Event.RunType runType, String apiPath, EventType eventType, Optional<Instant> timestamp,
		 * 			String reqId, String collection)
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

			queryParams.add("filmName" , "Beverly Outlaw");

			eventBuilder.setPayload(new HTTPRequestPayload(headers, queryParams,
				null, "GET", null));

			httpRequestEvent = eventBuilder.createEvent();

			objectMapper = CubeObjectMapperProvider.getInstance();

		}

		@Test
		public void testHttpRequestEvent() throws IOException {
			String fromInitialEvent = objectMapper.writeValueAsString(httpRequestEvent);
			System.out.println(fromInitialEvent);
			Event fromSerialized = objectMapper.readValue(fromInitialEvent, Event.class);
			Assert.assertEquals(fromSerialized.customerId, "random-user");
			Assert.assertNotNull(fromSerialized.payload);
			Assert.assertEquals(fromSerialized.payload.getClass(), HTTPRequestPayload.class);
			HTTPRequestPayload payloadFromSerialized = (HTTPRequestPayload) fromSerialized.payload;
			Assert.assertEquals(payloadFromSerialized.
				hdrs.getFirst("content-type"), MediaType.APPLICATION_JSON);
			Assert.assertTrue(payloadFromSerialized.body == null
				|| payloadFromSerialized.body.length == 0);
			Assert.assertNull(payloadFromSerialized.formParams);
			Assert
				.assertEquals("Beverly Outlaw", payloadFromSerialized.queryParams.getFirst("filmName"));
		}



}
