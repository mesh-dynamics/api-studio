package io.cube.agent;

import static io.cube.agent.Constants.APPLICATION_JSON;
import static io.cube.agent.HttpUtils.getResponse;
import static io.md.constants.Constants.NO_INTENT;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.md.logger.LogMgr;
import org.apache.http.Consts;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig.ServerConfigUpdater;
import io.cube.agent.samplers.Sampler;
import io.md.core.ConfigApplicationAcknowledge;

public class ClientUtils {

	private static final Logger LOGGER = LogMgr.getLogger(ClientUtils.class);

	private static ObjectMapper jsonMapper = new ObjectMapper();

	public static ReadWriteLock lock =new ReentrantReadWriteLock();

	public static void initialize(Map<String, String> attributeMap) {
		try {

			try {
				lock.writeLock().lock();

				//Need to set before Node Selection decision
				CommonConfig.clientMetaDataMap.putAll(attributeMap);

				//set any CommonConfig variables if specified.
				setCustomerAttributesAndSchedulePolling(attributeMap);

				addNodeSelectionDecision(attributeMap);

				//call cube server API to send the attributeMap and sampling decision
				sendAckToCubeServer();

			} finally {
				lock.writeLock().unlock();
			}
		} catch (Exception e) {
			LOGGER.error("Error in initialising client ", e);
		} finally {
			LOGGER.info("INITIALIZE Completed!");
		}
	}

	private static void setCustomerAttributesAndSchedulePolling(Map<String, String> attributeMap) {

		Optional.ofNullable(attributeMap.get(io.md.constants.Constants.MD_CUSTOMER_PROP))
			.ifPresent(val -> CommonConfig.customerId = val);
		Optional.ofNullable(attributeMap.get(io.md.constants.Constants.MD_APP_PROP))
			.ifPresent(val -> CommonConfig.app = val);
		Optional.ofNullable(attributeMap.get(io.md.constants.Constants.MD_INSTANCE_PROP))
			.ifPresent(val -> CommonConfig.instance = val);
		Optional.ofNullable(attributeMap.get(io.md.constants.Constants.MD_SERVICE_PROP))
			.ifPresent(val -> CommonConfig.serviceName = val);
		Optional.ofNullable(attributeMap.get(Constants.MD_POLLINGCONFIG_RETRYCOUNT))
			.ifPresent(val -> CommonConfig.fetchConfigRetryCount = Integer.valueOf(val));
		Optional.ofNullable(attributeMap.get(Constants.AUTH_TOKEN_PROP))
			.ifPresent(val -> CommonConfig.getInstance().authToken = Optional.of(val));
		Optional.ofNullable(attributeMap.get(io.md.constants.Constants.MD_EXTERNAL_ID_FIELD))
			.ifPresent(val -> CommonConfig.externalIdField = val);

		Optional.ofNullable(attributeMap
			.get(io.md.constants.Constants.MD_SERVICE_ENDPOINT_PROP)).ifPresent(val -> {
			String cubeServiceEndPoint = Utils.appendTrailingSlash(val);
			CommonConfig.fetchConfigApiURI = new URIBuilder(
				URI.create(cubeServiceEndPoint)
					.resolve(Constants.MD_FETCH_AGENT_CONFIG_API_PATH)).toString();

			CommonConfig.ackConfigApiURI = new URIBuilder(
				URI.create(cubeServiceEndPoint)
					.resolve(Constants.MD_ACK_CONFIG_API_PATH)).toString();

			//To avoid the default agent config fetch failure
			//corrupt the valid config fetch after setting customer attributes.
			CommonConfig.onPrem = true;

			//stop the already scheduled task through common config
			stopPolling();

			//If the previous polling has already completed fetching,
			//it is possible tag and version are set. This will not update the
			//config values when the following fetch happens. So, setting it here.
			CommonConfig.tag = "NA";
			CommonConfig.version = "NA";

			//schedule with the customer provided URI info
			schedulePolling();
		});
	}

	public static void sendAckToCubeServer() {
		CommonConfig commonConfig = CommonConfig.getInstance();

		URI ackURI = URI.create(CommonConfig.ackConfigApiURI);
		HttpPost ackReqBuilder = new HttpPost(ackURI);
		commonConfig.authToken.ifPresent(
			val -> ackReqBuilder.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, val));
		ackReqBuilder.setHeader(io.md.constants.Constants.CONTENT_TYPE, APPLICATION_JSON);

		Map<String, String> acknowledgeInfoMap = new HashMap<>();
		acknowledgeInfoMap.putAll(CommonConfig.clientMetaDataMap);
		acknowledgeInfoMap.put(Constants.CONFIG_VERSION_FIELD, CommonConfig.version);
		acknowledgeInfoMap.put(Constants.CONFIG_TAG_FIELD, CommonConfig.tag);

		ConfigApplicationAcknowledge caa = new ConfigApplicationAcknowledge(CommonConfig.customerId,
			CommonConfig.app, CommonConfig.serviceName, CommonConfig.instance, acknowledgeInfoMap);

		try {
			String requestBody = jsonMapper.writeValueAsString(caa);
			LOGGER.debug("ConfigApplicationAcknowledge : ".concat(requestBody));

			StringEntity requestEntity = new StringEntity(requestBody, Consts.UTF_8);
			requestEntity.setContentType(APPLICATION_JSON);
			ackReqBuilder.setEntity(requestEntity);

			Optional<CloseableHttpResponse> response = getResponse(ackReqBuilder, Optional.empty());
			if (!response.isPresent() || !Response.Status.Family
				.familyOf(response.get().getStatusLine().getStatusCode())
				.equals(Response.Status.Family.SUCCESSFUL)) {
				LOGGER.error("Could not send acknowledgement to cube");
			}
			response.ifPresent(UtilException.rethrowConsumer(Closeable::close));
		} catch (IOException e) {
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

	private static void stopPolling() {
		if (CommonConfig.isFetchThreadInit) {
			//cancel the existing task
			CommonConfig.fetchConfigFuture.cancel(true);
			LOGGER.info("fetchConfig thread cancelled!!");
		}
	}

	private static void schedulePolling() {
		ServerConfigUpdater updater = new ServerConfigUpdater(CommonConfig.fetchConfigApiURI);
		updater.run(); //This is called to have the config fetched before initialize ends

		CommonConfig.fetchConfigFuture = CommonConfig.serviceExecutor
			.scheduleWithFixedDelay(updater,
				CommonConfig.fetchDelay, CommonConfig.fetchDelay,
				TimeUnit.SECONDS);
		LOGGER.info("Initialize fetchConfig thread scheduled!!");
		CommonConfig.isFetchThreadInit = true;
	}

	public static void changeIntent(String intent) {
		CommonConfig.intent = intent;
	}

}
