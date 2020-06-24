package io.cube.agent;

import static io.cube.agent.Constants.APPLICATION_JSON;
import static io.cube.agent.HttpUtils.getResponse;
import static io.md.constants.Constants.NO_INTENT;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.samplers.Sampler;
import io.md.core.ConfigApplicationAcknowledge;

public class ClientUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientUtils.class);

	private static ObjectMapper jsonMapper = new ObjectMapper();

	public static void initialize(Map<String, String> attributeMap) {
		try {

			addNodeSelectionDecision(attributeMap);

			CommonConfig.clientMetaDataMap.putAll(attributeMap);

			//call cube server API to send the attributeMap and sampling decision
			sendAckToCubeServer();
		}
		catch (Exception e) {
			LOGGER.error("Error in initialising client ", e);
		}
	}

	public static void sendAckToCubeServer() {
		CommonConfig commonConfig = CommonConfig.getInstance();

		URI ackURI = URI.create(CommonConfig.ackConfigApiURI);
		HttpPost ackReqBuilder = new HttpPost(ackURI);
		commonConfig.authToken.ifPresent(
			val -> ackReqBuilder.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, val));
		ackReqBuilder.setHeader(io.md.constants.Constants.CONTENT_TYPE, APPLICATION_JSON);

		Map<String, String> acknowledgeInfoMap = new HashMap();
		acknowledgeInfoMap.putAll(commonConfig.clientMetaDataMap);
		acknowledgeInfoMap.put(Constants.CONFIG_VERSION_FIELD, commonConfig.version);
		acknowledgeInfoMap.put(Constants.CONFIG_TAG_FIELD, commonConfig.tag);

		ConfigApplicationAcknowledge caa = new ConfigApplicationAcknowledge(commonConfig.customerId,
			commonConfig.app, commonConfig.serviceName, commonConfig.instance, acknowledgeInfoMap);

		try {
			String requestBody = jsonMapper.writeValueAsString(caa);
			LOGGER.debug("ConfigApplicationAcknowledge : ".concat(requestBody));

			StringEntity requestEntity = new StringEntity(requestBody, Consts.UTF_8);
			requestEntity.setContentType(APPLICATION_JSON);
			ackReqBuilder.setEntity(requestEntity);

			Optional<CloseableHttpResponse> response = getResponse(ackReqBuilder);
			if (!response.isPresent() || !Response.Status.Family
				.familyOf(response.get().getStatusLine().getStatusCode())
				.equals(Response.Status.Family.SUCCESSFUL)) {
				LOGGER.error("Could not send acknowledgement to cube");
			}
		} catch (JsonProcessingException e) {
			LOGGER.error("Sending Config Ack resulted in exception", e);
		}
	}

	public static void addNodeSelectionDecision(Map<String, String> attributeMap) {
		CommonConfig commonConfig = CommonConfig.getInstance();

		Sampler nodeSelector = commonConfig.nodeSelector;

		MultivaluedMap<String, String> paramsMap = new MultivaluedHashMap<>();
		attributeMap.forEach(paramsMap::add);

		if (!nodeSelector.isSampled(paramsMap)) {
			CommonConfig.intent = NO_INTENT; // we are not sampling in this node
			attributeMap.put(io.md.constants.Constants.IS_NODE_SELECTED, String.valueOf(false));
		} else {
			//we are sampling in this node
			attributeMap.put(io.md.constants.Constants.IS_NODE_SELECTED, String.valueOf(true));
		}
	}

}
