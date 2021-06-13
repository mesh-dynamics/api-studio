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
import io.md.dao.Recording.RecordingType;

public class TestEventRecord {

	public static void main(String[] args) {
		try {
			CommonConfig commonConfig =  CommonConfig.getInstance();
			ConsoleRecorder.init();
			ConsoleRecorder consoleRecorder = ConsoleRecorder.getInstance();

			CubeMetaInfo cubeMetaInfo = new CubeMetaInfo("random-user"
				, "test", "movie-info", "mi-rest");
			MDTraceInfo traceInfo = new MDTraceInfo("random-trace"
				, null , null);
			String apiPath = "/minfo/health";
			EventBuilder eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, apiPath, EventType.HTTPRequest
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden);

			MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
			headers.add("content-type" , MediaType.APPLICATION_JSON);
			MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
			queryParams.add("filmName" , "Beverly Outlaw");
			eventBuilder.setPayload(new HTTPRequestPayload(headers, queryParams,
				null, "GET", null, apiPath));
			Event httpRequestEvent = eventBuilder.createEvent();
			for (int i = 0 ; i < 1000 ; i++) {
				consoleRecorder.record(httpRequestEvent);
			}
			Thread.sleep(10*1000);
			//consoleRecorder.record(httpRequestEvent);
			//Thread.sleep(5*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
