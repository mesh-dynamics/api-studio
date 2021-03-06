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

import static io.cube.agent.Constants.APPLICATION_FORM_URL_ENCODED;
import static io.cube.agent.Constants.APPLICATION_JSON;
import static io.cube.agent.Constants.TEXT_PLAIN;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;
import io.md.core.CollectionKey;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingType;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.logger.LogMgr;
import io.md.services.MockResponse;
import io.md.utils.CommonUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Client to connect to cube service
 */
public class CubeClient {

	private final ObjectMapper jsonMapper;
	private Optional<String> authToken;

	private static final Logger LOGGER = LogMgr.getLogger(CubeClient.class);


	public CubeClient(ObjectMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
		authToken = Optional.empty();
	}

	public void setAuthToken(String authToken) {
		this.authToken = Optional.of(authToken);
	}

	public Optional<String> getResponse(HttpRequestBase request) {
		Optional<CloseableHttpResponse> response = getCloseableResponse(request);
		try {
			return response.flatMap(UtilException.rethrowFunction(resp -> {
				// response can be NOTFOUND so check for it
				if (Response.Status.Family.familyOf(resp.getStatusLine().getStatusCode())
						.equals(Response.Status.Family.SUCCESSFUL)) {
					return Optional.of(new BasicResponseHandler().handleResponse(resp));
				} else {
					return Optional.empty();
				}
			}));
		} catch (IOException e) {
			LOGGER.error("Error while reading response from cube service", e);
		} finally {
			try {
				response.ifPresent(UtilException.rethrowConsumer(Closeable::close));
			} catch (IOException e) {
					LOGGER.error("Error while closing the connection", e);
			}
		}
		LOGGER.error("Error while sending request to cube service");
		return Optional.empty();
	}

	public Optional<CloseableHttpResponse> getCloseableResponse(HttpRequestBase request) {
		Optional<String> token =
			authToken.isPresent() ? authToken : CommonConfig.getInstance().authToken;
		token
			.ifPresent(val -> request.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, val));

		return HttpUtils.getResponse(request, Optional.empty());
	}

	private Optional<String> getResponse(URI uri, Object reqBody,
		String contentType) {
		try {
			HttpPost httpPost = new HttpPost(uri);
			String requestBody = jsonMapper.writeValueAsString(reqBody);
			CommonUtils.addTraceHeaders(httpPost, "POST");
			StringEntity requestEntity = new StringEntity(requestBody, Consts.UTF_8);
			requestEntity.setContentType(contentType);
			httpPost.setEntity(requestEntity);
			//httpPost.setHeader("Content-Type", contentType);
			return getResponse(httpPost);
		} catch (JsonProcessingException ex) {
			LOGGER.error("Error while serializing request body", ex);
		} catch (UnsupportedCharsetException ex2) {
			LOGGER.error("Invalid string entity", ex2);
		}
		return Optional.empty();
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeFunctionReqResp(FnReqResponse fnReqResponse) {
		UriBuilder uriBuilder = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "fr");

		URI recordURI = uriBuilder.build();
		return getResponse(recordURI, fnReqResponse, TEXT_PLAIN);
	}

	//TODO: Cleanup - phase this out
	public Optional<String> storeSingleReqResp(ReqResp reqResp) {
		UriBuilder uriBuilder = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "rr");

		URI recordURI = uriBuilder.build();
		return getResponse(recordURI, reqResp, TEXT_PLAIN);
	}


	public Optional<MockResponse> getMockResponseEvent(Event event, Optional<Instant> lowerBoundForMatching) {
		UriBuilder uriBuilder = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_MOCK_SERVICE_URI)
				.segment("ms", "mockEvent");
		lowerBoundForMatching.ifPresent(lb -> uriBuilder.queryParam(Constants.LOWER_BOUND, lb.toEpochMilli()));
		return getResponse(uriBuilder.build(), event, APPLICATION_JSON).flatMap(response -> {
			try {
				LOGGER.debug("Response : ".concat(response));
				return Optional.of(jsonMapper.readValue(response, MockResponse.class));
			} catch (Exception e) {
				LOGGER.error("Error while parsing json response from mock server", e);
				return Optional.empty();
			}
		});
	}


	public Optional<String> startRecording(String customerid, String app, String instanceid,
		String goldenName, String userId, String label, String templateSetVersion) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "start", customerid, app, instanceid, templateSetVersion)
				.build();
		HttpPost reqBuilder = createPostRequest(recordURI, "name", goldenName, "userId", userId,
				"label", label);
		return getResponse(reqBuilder);
	}

	public Optional<String> stopRecording(String customerid, String app, String goldenName, String label) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs").segment("stopRecordingByNameLabel")
				.build();
		UriBuilder uriBuilder = UriBuilder.fromUri(recordURI)
				.queryParam("customerId", customerid)
				.queryParam("app", app)
				.queryParam("golden_name", goldenName)
				.queryParam("label", label);

		return getPostResponse(uriBuilder.build());
	}

	static public HttpPost createPostRequest(URI uri, String ... nameVals) {
		HttpPost recordReqbuilder = new HttpPost(uri);
		List<NameValuePair> form = new ArrayList<>();
		for (int i=1; i<nameVals.length; i+=2) {
			form.add(new BasicNameValuePair(nameVals[i-1], nameVals[i]));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		recordReqbuilder.setEntity(entity);
		CommonUtils.addTraceHeaders(recordReqbuilder, "POST");
		recordReqbuilder.setHeader("Content-Type", APPLICATION_FORM_URL_ENCODED);
		return recordReqbuilder;
	}


	public Optional<String> initReplay(String customerid, String app, String instanceid,
									   String goldenName, String endpoint, String user) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("rs").segment("start", "byGoldenName")
				.segment(customerid).segment(app)
				.segment(goldenName)
				.build();

		HttpPost recordReqbuilder = createPostRequest(recordURI, "instanceId", instanceid,
				"endPoint", endpoint, "sampleRate", "0", "userId", user,
				"startReplay", "false", "analyze", "false");

		return getResponse(recordReqbuilder);
	}

	public Optional<String> forceStartReplay(String replayid) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("rs").segment("forcestart")
				.segment(replayid)
				.build();
		return getPostResponse(recordURI);
	}


	public Optional<String> forceCompleteReplay(String replayid) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("rs").segment("forcecomplete")
				.segment(replayid)
				.build();
		return getPostResponse(recordURI);
	}

	public Optional<String> storeEvent(Event event) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs").segment("storeEvent")
				.build();

		return getResponse(recordURI, event, MediaType.APPLICATION_JSON);
	}

	public Optional<String> storeEvents(Event... events) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.segment("cs").segment("storeEventBatch")
			.build();

		/*
		String eventsJsons = Arrays.stream(events).map(e->{
			try{
				return jsonMapper.writeValueAsString(e);
			}catch (Exception ex){
				return null;
			}
		}).collect(Collectors.joining("\r\n"));
		return getResponse(recordURI, events, Constants.APPLICATION_X_NDJSON);

		 */
		return getResponse(recordURI, events, MediaType.APPLICATION_JSON);
	}

	public Optional<String> getEvents(EventQuery eventQuery) {
		URI recordURI = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs").segment("getEvents")
				.build();
		return getResponse(recordURI, eventQuery, MediaType.APPLICATION_JSON);
	}

	public Optional<String> getTemplate(String customerId, String app, String service, String apiPath,
										String templateVersion, TemplateKey.Type templateType,
										Optional<Event.EventType> eventType, String recordingId) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("as", "getTemplate", customerId, app, templateVersion, service,
						templateType.toString())
				.queryParam("apiPath", apiPath)
				.queryParam("eventType", eventType.toString())
				.queryParam("recordingId", recordingId)
				.build();

		return getGetResponse(uri);
	}

	public Optional<String> saveResult(ReqRespMatchResult result) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "saveResult")
				.build();

		return getResponse(uri, result, MediaType.APPLICATION_JSON);
	}

	public Optional<String> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "getCurrentRecordOrReplay", customerId, app, instanceId)
				.build();

		return getGetResponse(uri);
	}

	public Optional<String> getAppConfiguration(String customerId, String app) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "getAppConfiguration", customerId, app)
				.build();

		return getGetResponse(uri);
	}


	public Optional<String> getDynamicInjectionConfig(String customerId, String app, String version) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_REPLAY_SERVICE_URI)
				.segment("rs", "getDynamicInjectionConfig", customerId, app, version)
				.build();

		return getGetResponse(uri);
	}

	public Optional<String> getReplay(String replayId) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_REPLAY_SERVICE_URI)
				.segment("rs", "status", replayId)
				.build();

		return getGetResponse(uri);
	}

	public Optional<String> getRecording(String recordingId) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
				.segment("cs", "status", recordingId)
				.build();

		return getGetResponse(uri);
	}

	public Optional<String> saveReplay(Replay replay) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_REPLAY_SERVICE_URI)
				.segment("rs", "saveReplay")
				.build();

		return getResponse(uri, replay, MediaType.APPLICATION_JSON);
	}

	public Optional<String> deferredDelete(Replay replay) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_REPLAY_SERVICE_URI)
				.segment("rs", "deferredDeleteReplay", replay.replayId, replay.status.toString())
				.build();

		HttpPost reqBuilder = createPostRequest(uri);
		return getResponse(reqBuilder);
	}

	public Optional<String> analyze(String replayId, Optional<String> templateVersion) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_REPLAY_SERVICE_URI)
				.segment("as", "analyze", replayId)
				.build();
		HttpPost reqBuilder = templateVersion.map(version ->
			createPostRequest(uri, Constants.TEMPLATE_VERSION_FIELD , version)).orElse(createPostRequest(uri));
		return getResponse(reqBuilder);
	}

	private Optional<String> getPostResponse(URI uri) {
		HttpPost httpPost = new HttpPost(uri);
		List<NameValuePair> form = new ArrayList<>();
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);
		httpPost.setEntity(entity);
		httpPost.setHeader("Content-Type", APPLICATION_FORM_URL_ENCODED);
		CommonUtils.addTraceHeaders(httpPost, "POST");

		return getResponse(httpPost);
	}

	private Optional<String> getGetResponse(URI uri) {
		HttpGet httpGet = new HttpGet(uri);
		CommonUtils.addTraceHeaders(httpGet, "GET");

		return getResponse(httpGet);
	}

	public Optional<String> populateCache(CollectionKey collectionKey, RecordOrReplay recordOrReplay) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.segment("cs", "populateCache")
			.build();
		//Collection key can be reconstructed from recordOrReplay
		return getResponse(uri, recordOrReplay, MediaType.APPLICATION_JSON);
	}

	public Optional<String> getLatestTemplateSetLabel(String customerId, String app, String templateSetName) {
		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.segment("cs", "getLatestTemplateSetLabel" , customerId , app , templateSetName).build();
		return getGetResponse(uri);
	}

	public Optional<String> copyRecording(String recordingId, Optional<String> name,
		Optional<String> label, Optional<String> templateSetName, Optional<String> templateSetLabel,
		String userId, RecordingType type, Optional<Predicate<Event>> eventFilter) {
		UriBuilder uriBuilder = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.segment("cs" , "copyRecording" , recordingId , userId);
		name.ifPresent(nameStr -> uriBuilder.queryParam(Constants.GOLDEN_NAME_FIELD , nameStr));
		label.ifPresent(labelStr ->  uriBuilder.queryParam(Constants.GOLDEN_LABEL_FIELD , labelStr));
		templateSetName.ifPresent(templateNameStr -> uriBuilder.queryParam(Constants
			.TEMPLATE_SET_NAME, templateNameStr));
		templateSetLabel.ifPresent(templateLabelStr -> uriBuilder.queryParam(Constants
			.TEMPLATE_SET_LABEL, templateLabelStr));
		uriBuilder.queryParam(Constants.RECORDING_TYPE_FIELD, type.name());
		if (eventFilter.isPresent()) throw new NotImplementedException();
		// TODO leaving eventFilter out of API Request right now
		return getPostResponse(uriBuilder.build());
	}



	public Optional<String> getLatestProtoDescriptorDAO(String customerId, String app) {

		URI uri = UriBuilder.fromPath(CommonConfig.getInstance().CUBE_RECORD_SERVICE_URI)
			.segment("cs", "getProtoDescriptor", customerId, app)
			.queryParam("asDAO", true)
			.build();

		return getGetResponse(uri);
	}
}