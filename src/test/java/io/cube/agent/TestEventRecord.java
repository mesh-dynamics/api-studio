package io.cube.agent;

import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.google.gson.GsonBuilder;

import io.md.dao.CubeMetaInfo;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.MDTraceInfo;

public class TestEventRecord {

	public static void main(String[] args) {
		try {
			CommonConfig commonConfig =  CommonConfig.getInstance();
			ConsoleRecorder consoleRecorder = new ConsoleRecorder(new GsonBuilder().create());

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
			Event httpRequestEvent = eventBuilder.createEvent();
			consoleRecorder.record(httpRequestEvent);
			Thread.sleep(5*1000);
			consoleRecorder.record(httpRequestEvent);
			Thread.sleep(5*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
