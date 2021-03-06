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

package com.cube.ws;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.ResponsePayload;
import io.md.utils.Utils;

public class SanitizationFilters {

	private static final Logger LOGGER = LogManager.getLogger(SanitizationFilters.class);

	public static class BadStatuses implements SanitizationFilter{

		private final Set<String> badRes = new HashSet<>();
		private final Set<String> badStatusCodes;
		public BadStatuses(Set<String> badStatusCodes){this.badStatusCodes = badStatusCodes;}
		@Override
		public boolean consume(Event e) {
			if (e.payload instanceof ResponsePayload) {
				if (badStatusCodes.contains(((ResponsePayload) e.payload).getStatusCode())) {
					badRes.add(e.reqId);
					return false;
				}
			}
			return true;
		}

		@Override
		public Set<String> getBadReqIds() {
			return badRes;
		}
	}

	public static class ReqRespMissing implements SanitizationFilter{

		private Set<String> seenReqIds = new HashSet<>();
		private Set<String> seenRespIds = new HashSet<>();

		@Override
		public boolean consume(Event e) {
			if (e.payload instanceof HTTPRequestPayload) {
				seenReqIds.add(e.reqId);

			} else if (e.payload instanceof HTTPResponsePayload) {
				seenRespIds.add(e.reqId);
			}
			return true;
		}

		@Override
		public Set<String> getBadReqIds() {
			return Sets.symmetricDifference(seenReqIds, seenRespIds);
		}
	}

	public static class IgnoreStaticContent implements SanitizationFilter {

		public static final HashSet<String> staticContentMimes = new HashSet<>(List.of("gif" , "html" , "css" , "javascript" , "ttf" , "svg" , "png" , "text"));
		private final Set<String> badReqResp = new HashSet<>();

		private static boolean isStaticContentPath(String path){
			return staticContentMimes.stream().anyMatch(mime-> Utils.endsWithIgnoreCase(path , mime));
		}

		private static boolean staticHeader(MultivaluedMap<String, String> headers , String key){
			return Optional.ofNullable(headers.get(key)).map(contentTypes-> contentTypes.stream().anyMatch(type->staticContentMimes.stream().anyMatch(mime->type.toLowerCase().indexOf(mime)!=-1))).orElse(false);
		}

		@Override
		public boolean consume(Event e) {
			if (e.payload instanceof HTTPRequestPayload) {
				if(isStaticContentPath(e.apiPath) || staticHeader(((HTTPRequestPayload) e.payload).getHdrs() , "accept")){
					badReqResp.add(e.reqId);
					return false;
				}
			}else if (e.payload instanceof HTTPResponsePayload) {
				HTTPResponsePayload payload = (HTTPResponsePayload) e.payload;
				if(staticHeader(payload.getHdrs() , "content-type")){
					badReqResp.add(e.reqId);
					return false;
				}
			}
			return true;
		}

		@Override
		public Set<String> getBadReqIds() {
			return badReqResp;
		}
	}


	/*
	  Returns Set of Bad Request Ids (to be filtered)
	 */
	public static Set<String> getBadRequests(Stream<Event> input , List<SanitizationFilter> filters ){

		for(var f : filters){
			input = input.filter(f::consume);
		}
		input.forEach(event -> {
			// terminal Operation
		});

		Set<String> badReqIds = new HashSet<>();
		filters.stream().forEach(f->badReqIds.addAll(f.getBadReqIds()));

		LOGGER.debug("Total Bad Requests for sanitization "+badReqIds.size());
		return badReqIds;
	}

}
