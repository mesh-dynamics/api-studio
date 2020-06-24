package io.cube.agent;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException.Missing;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SamplerConfig;
import io.md.constants.Constants;
import io.md.tracer.MDGlobalTracer;
import io.md.tracer.MDTextMapCodec;
import io.md.utils.CommonUtils;
import io.opentracing.Tracer;


public class CommonConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonConfig.class);

	/******* PROPERTIES HOLDERS ******/
	// Cube essentials
	// Will not be dynamically polled. Will be initialised just once in the beginning
	public final static String customerId, app, instance, serviceName;

	//intent
	public static String intent;

	// version
	public static String tag = "NA";
	public static String version = "NA";
	public static String ackConfigApiURI;

	// Mocking
	public Optional<String> authToken;
	public List servicesToMock;

	// Cube Connection
	public final String CUBE_RECORD_SERVICE_URI;
	public final String CUBE_MOCK_SERVICE_URI;
	public final int READ_TIMEOUT;
	public final int CONNECT_TIMEOUT;
	public final int RETRIES;

	// Encryption
	public Optional<EncryptionConfig> encryptionConfig;

	// Sampling
	public boolean samplerVeto;
	public Sampler sampler;

	//performance test
	public boolean performanceTest;

	// Disruptor
	public int ringBufferSize;
	public String disruptorOutputLocation;
	public String disruptorFileOutName;
	public long disruptorLogFileMaxSize;
	public int disruptorLogMaxBackup;
	public int disruptorConsumerMemoryBufferSize;

	//Node selection info to record
	public final Sampler nodeSelector;

	/******* OTHER OBJECTS ******/
	// Lightbend Config library by default throws exception on property not found
	// We need to ensure that at least NOOP properties are always defined in staticConfFile
	private static final String staticConfFile = "agent_conf.json";

	// Priority for default conf is envVar > sysProp > static_conf_file
	static Config envSysStaticConf;

	private static AtomicReference<CommonConfig> singleInstance = null;
	static ScheduledExecutorService serviceExecutor;
	private CloseableHttpClient httpClient;
	static protected ObjectMapper jsonMapper = new ObjectMapper();
	public static Map<String, String> clientMetaDataMap = new HashMap<>();

	static {

		//initialize Logging
		jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			envSysStaticConf = ConfigFactory.systemEnvironment()
				.withFallback(ConfigFactory.systemProperties())
				.withFallback(ConfigFactory.load(staticConfFile));

		} catch (Exception e) {
			LOGGER.error("Error while initializing config", e);
		}

		intent = envSysStaticConf.getString(Constants.MD_INTENT_PROP);
		customerId = envSysStaticConf.getString(Constants.MD_CUSTOMER_PROP);
		app = envSysStaticConf.getString(Constants.MD_APP_PROP);
		instance = envSysStaticConf.getString(Constants.MD_INSTANCE_PROP);
		serviceName = envSysStaticConf.getString(Constants.MD_SERVICE_PROP);

		CommonConfig config = null;
		try {
			config = new CommonConfig();
		} catch (Exception e) {
			LOGGER.error("Error in initialising common config object", e);
		}
		singleInstance = new AtomicReference<>();
		singleInstance.set(config);

		boolean isServerPolling = envSysStaticConf
			.getBoolean(io.cube.agent.Constants.MD_POLLINGCONFIG_POLLSERVER);

		// This is only for developer user case allowing polling properties from file
		// When polling from file the polling from cubeio will not be enabled.
		try {
			String dynamicConfigFilePath = envSysStaticConf
				.getString(io.cube.agent.Constants.MD_POLLINGCONFIG_FILEPATH);
			int delay = envSysStaticConf.getInt(io.cube.agent.Constants.MD_POLLINGCONFIG_DELAY);
			serviceExecutor = Executors.newScheduledThreadPool(1);
			serviceExecutor
				.scheduleWithFixedDelay(new fileConfigUpdater(dynamicConfigFilePath), 0, delay,
					TimeUnit.SECONDS);
			isServerPolling = false;
		} catch (Missing e) {
			LOGGER.info("Dynamic config polling from file is not enabled");
		}

		if (isServerPolling) {

			initClientMetaDataMap();

			String fetchConfigApiURI = appendTrailingSlash(envSysStaticConf
				.getString(io.cube.agent.Constants.MD_POLLINGCONFIG_FETCHCONFIGAPIURI));

			ackConfigApiURI = appendTrailingSlash(envSysStaticConf
				.getString(io.cube.agent.Constants.MD_POLLINGCONFIG_ACKCONFIGAPIURI));

			int delay = envSysStaticConf.getInt(io.cube.agent.Constants.MD_POLLINGCONFIG_DELAY);
			serviceExecutor = Executors.newScheduledThreadPool(1);
			serviceExecutor
				.scheduleWithFixedDelay(new serverConfigUpdater(fetchConfigApiURI),
					0, delay,
					TimeUnit.SECONDS);
		}
	}

	private static void initClientMetaDataMap() {
		//This is the default info sent to Cube Server if
		//Customer is not calling the initialize()
		InetAddress ipAddress = null;
		try {
			ipAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			LOGGER.debug("IP Address fetch issue");
		}

		clientMetaDataMap
			.put("ipAddress", ipAddress == null ? "0.0.0.0" : ipAddress.getHostAddress());
		clientMetaDataMap.put("uniqueId", UUID.randomUUID().toString());
		//Default is set to true.
		clientMetaDataMap.put(io.md.constants.Constants.IS_NODE_SELECTED, String.valueOf(true));
	}


	private static class fileConfigUpdater implements Runnable {

		String configFilePath;

		public fileConfigUpdater(String configFilePath) {
			this.configFilePath = configFilePath;
		}

		@Override
		public void run() {
			try {
				File input = new File(configFilePath);
				Config fileConfigPolled = ConfigFactory.load(ConfigFactory.parseFile(input))
					.withFallback(envSysStaticConf);
				// Using just set instead of compareAndSet as the value being set is independent of the current value.
				singleInstance.set(new CommonConfig(fileConfigPolled));
			} catch (Exception e) {
				LOGGER.error("Error in updating common config object in thread", e);
			}
		}
	}

	private static class serverConfigUpdater implements Runnable {

		String fetchConfigApiURI;

		public serverConfigUpdater(String fetchConfigApiURI) {
			this.fetchConfigApiURI = fetchConfigApiURI;
		}

		@Override
		public void run() {
			try {

				URIBuilder fetchConfigUriBuilder = new URIBuilder(
					URI.create(this.fetchConfigApiURI)
						.resolve(CommonConfig.customerId + "/" + CommonConfig.app + "/"
							+ CommonConfig.serviceName + "/" + CommonConfig.instance)
				);
				fetchConfigUriBuilder.setParameter("tag", tag).setParameter("version", version);

				HttpGet fetchConfigApiReq = new HttpGet(fetchConfigUriBuilder.build());

				CommonConfig commonConfig = CommonConfig.getInstance();
				commonConfig.authToken.ifPresent(
					val -> fetchConfigApiReq.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, val));

				Optional<CloseableHttpResponse> fetchConfigApiRespOpt = HttpUtils
					.getResponse(fetchConfigApiReq);
				fetchConfigApiRespOpt
					.orElseThrow(() -> new Exception("Cannot get config from cube server"));
				CloseableHttpResponse fetchConfigApiResp = fetchConfigApiRespOpt.get();

				if (fetchConfigApiResp.getStatusLine().getStatusCode()
					!= HttpStatus.SC_NOT_MODIFIED) {
					String jsonString = new BasicResponseHandler()
						.handleResponse(fetchConfigApiResp);
					JsonNode jsonNode = jsonMapper.readTree(jsonString);
					String configString = jsonNode.get("configJson").get("config")
						.asText();

					Config serverPollConfig = ConfigFactory.parseString(configString)
						.withFallback(envSysStaticConf);
					// Using just set instead of compareAndSet as the value being set is independent of the current value.
					singleInstance.set(new CommonConfig(serverPollConfig));
					tag = jsonNode.get(io.cube.agent.Constants.CONFIG_TAG_FIELD).asText();
					version = jsonNode.get(io.cube.agent.Constants.CONFIG_VERSION_FIELD)
						.asText();
					//run node selection algo again
					ClientUtils.addNodeSelectionDecision(clientMetaDataMap);
				} else {
					LOGGER.info("Server returned same properties. Not applying");
				}

				// Send ack even if the config is not re-applied
				ClientUtils.sendAckToCubeServer();

				fetchConfigApiResp.close();
			} catch (Exception e) {
				LOGGER.error("Error in updating common config object in thread", e);
			}
		}
	}


	@Override
	public String toString() {
		return "CommonConfig{" +
			"CUBE_RECORD_SERVICE_URI='" + CUBE_RECORD_SERVICE_URI + '\'' +
			", CUBE_MOCK_SERVICE_URI='" + CUBE_MOCK_SERVICE_URI + '\'' +
			", intent=" + intent +
			", customerId='" + customerId + '\'' +
			", app='" + app + '\'' +
			", instance='" + instance + '\'' +
			", serviceName='" + serviceName + '\'' +
			", encryptionConfig=" + encryptionConfig +
			", sampler=" + sampler +
			", READ_TIMEOUT=" + READ_TIMEOUT +
			", CONNECT_TIMEOUT=" + CONNECT_TIMEOUT +
			", RETRIES=" + RETRIES +
			", performance_test=" + performanceTest +
			'}';
	}

	public static CommonConfig getInstance() {
		return singleInstance.get();
	}


	private CommonConfig() throws Exception {
		this(envSysStaticConf);
		//System.setProperty("JAEGER_AGENT_HOST", "jaeger-agent");
		Tracer tracer = CommonUtils.init("tracer");
		try {
			MDGlobalTracer.register(tracer);
		} catch (IllegalStateException e) {
			LOGGER.error("Trying to register a tracer when one is already registered", e);
		}
	}

	private CommonConfig(Config dynamicConfig) throws Exception {

		CUBE_RECORD_SERVICE_URI = appendTrailingSlash(dynamicConfig.getString(
			Constants.MD_RECORD_SERVICE_PROP));
		CUBE_MOCK_SERVICE_URI = appendTrailingSlash(dynamicConfig.getString(
			Constants.MD_MOCK_SERVICE_PROP));
		READ_TIMEOUT = dynamicConfig.getInt(
			Constants.MD_READ_TIMEOUT_PROP);
		CONNECT_TIMEOUT = dynamicConfig.getInt(
			Constants.MD_CONNECT_TIMEOUT_PROP);
		RETRIES = dynamicConfig.getInt(
			Constants.MD_RETRIES_PROP);

		intent = dynamicConfig.getString(Constants.MD_INTENT_PROP);

		ConfigRenderOptions options = ConfigRenderOptions.concise();

		nodeSelector = getNodeSelectionDetails(dynamicConfig, options);

		sampler = getSamplerDetails(dynamicConfig, options);
		samplerVeto = dynamicConfig.getBoolean(Constants.MD_SAMPLER_VETO);

		getEncryptionDetails(dynamicConfig, options);

		authToken = Optional.of(dynamicConfig.getString(io.cube.agent.Constants.AUTH_TOKEN_PROP));

		if (CUBE_RECORD_SERVICE_URI.endsWith("/api/") || CUBE_MOCK_SERVICE_URI.endsWith("/api/")) {
			authToken.orElseThrow(
				() -> new Exception("Auth token not specified when /api present"));
		}

		servicesToMock = (List) getValueOrDefault(dynamicConfig,
			io.cube.agent.Constants.SERVICES_TO_MOCK_PROP, Collections.EMPTY_LIST);

		performanceTest = dynamicConfig.getBoolean(io.cube.agent.Constants.MD_PERFORMANCE_TEST);

		getMessageQueueDetails(dynamicConfig);

		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout(READ_TIMEOUT)
			.setConnectTimeout(CONNECT_TIMEOUT)
			.setSocketTimeout(CONNECT_TIMEOUT)
			.build();

		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

		LOGGER.info("PROPERTIES POLLED :: " + this.toString());
	}

	private static Sampler getNodeSelectionDetails(Config dynamicConfig,
		ConfigRenderOptions options)
		throws java.io.IOException {
		Optional<SamplerConfig> nodeSelectionConfigTmp;
		try {
			String samplerConfigJsonString = dynamicConfig
				.getValue(io.cube.agent.Constants.MD_NODE_SELECTION_CONFIG).render(options);
			nodeSelectionConfigTmp = Optional
				.of(jsonMapper.readValue(samplerConfigJsonString, SamplerConfig.class));
		} catch (Missing e) {
			nodeSelectionConfigTmp = Optional.empty();
		}

		return Utils.initSampler(nodeSelectionConfigTmp);
	}

	private static Sampler getSamplerDetails(Config dynamicConfig, ConfigRenderOptions options)
		throws java.io.IOException {
		Optional<SamplerConfig> samplerConfig;
		try {
			String samplerConfigJsonString = dynamicConfig
				.getValue(io.cube.agent.Constants.SAMPLER_CONF_PROP).render(options);
			samplerConfig = Optional
				.of(jsonMapper.readValue(samplerConfigJsonString, SamplerConfig.class));
		} catch (Missing e) {
			samplerConfig = Optional.empty();
		}

		return Utils.initSampler(samplerConfig);
	}

	private void getEncryptionDetails(Config dynamicConfig, ConfigRenderOptions options) {
		try {
			String encryptionJsonString = dynamicConfig
				.getValue(io.cube.agent.Constants.ENCRYPTION_CONF_PROP).render(options);
			encryptionConfig = Optional.of(jsonMapper
				.readValue(encryptionJsonString, EncryptionConfig.class));
		} catch (Missing e) {
			encryptionConfig = Optional.empty();
		} catch (Exception e) {
			LOGGER.error("Error in reading encryption config file", e);
		}
	}

	// Method to append trailing slash to given uris
	// This is done because resolve method in uri create
	// has issues without the slash
	private static String appendTrailingSlash(String uri) {
		if(!uri.endsWith("/")) return uri + "/";
		else return uri;
	}

	private void getMessageQueueDetails(Config dynamicConfig) {
		ringBufferSize = dynamicConfig.getInt(io.cube.agent.Constants.RING_BUFFER_SIZE_PROP);

		disruptorOutputLocation = dynamicConfig
			.getString(io.cube.agent.Constants.RING_BUFFER_OUTPUT_PROP);

		disruptorFileOutName = dynamicConfig
			.getString(io.cube.agent.Constants.RING_BUFFER_OUTPUT_FILE_NAME);

		disruptorLogFileMaxSize = dynamicConfig
			.getLong(io.cube.agent.Constants.DISRUPTOR_LOG_FILE_MAX_SIZE_PROP);

		disruptorLogMaxBackup = dynamicConfig
			.getInt(io.cube.agent.Constants.DISRUPTOR_LOG_FILE_MAX_BACKUPS_PROP);

		disruptorConsumerMemoryBufferSize = dynamicConfig
			.getInt(io.cube.agent.Constants.DISRUPTOR_CONSUMER_MEMORY_BUFFER_SIZE);
	}

	public CloseableHttpClient getHttpClient() {
		return httpClient;
	}

	private static String getConfigIntent() {
		return CommonConfig.intent;
	}

	public static String getCurrentIntent() {
		return getCurrentIntentFromScope().orElse(getConfigIntent());
	}

	public static Optional<String> getCurrentIntentFromScope() {
		return Optional.ofNullable(CommonUtils.getCurrentSpan().flatMap(span -> Optional.
			ofNullable(span.getBaggageItem(Constants.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY))).orElse(
			CommonConfig.intent));
	}

	public static boolean isIntentToRecord() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_RECORD);
	}

	public static boolean isIntentToMock() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_MOCK);
	}

	public boolean shouldMockService(String serviceName) {
		return servicesToMock.contains(serviceName);
	}

	// Avoid using this method for default so that the source of truth remains single.
	// Ideally all defaults should be set in the staticConfFile file.
	// This method should be used for setting properties which maybe Optional.
	private Object getValueOrDefault(Config conf, String property, Object defaultVal) {
		Object toReturn;
		try {
			toReturn = conf.getAnyRef(property);
		} catch (Missing e) {
			toReturn = defaultVal;
		}
		return toReturn;
	}

	public Optional<URI> getMockingURI(URI originalURI, String serviceName)
		throws URISyntaxException {
		if (!shouldMockService(serviceName)) {
			return Optional.empty();
		} else {
			URIBuilder uriBuilder = new URIBuilder(originalURI);
			URI cubeMockURI = new URI(CUBE_MOCK_SERVICE_URI);
			uriBuilder.setHost(cubeMockURI.getHost());
			uriBuilder.setPort(cubeMockURI.getPort());
			uriBuilder.setScheme(cubeMockURI.getScheme());
			String origPath = uriBuilder.getPath();
			String pathToSet = cubeMockURI.getPath() + "/ms" +
				"/" + customerId +
				"/" + app +
				"/" + instance +
				"/" + serviceName +
				"/" + origPath;
			uriBuilder.setPath(pathToSet);
			return Optional.of(uriBuilder.build().normalize());
		}
	}


}
