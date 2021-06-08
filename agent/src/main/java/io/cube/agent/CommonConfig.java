package io.cube.agent;

import static io.cube.agent.Utils.appendTrailingSlash;
import static io.cube.agent.Utils.compAndInitRecorder;
import static io.cube.agent.Utils.initRecorder;
import static io.cube.agent.Utils.savePrevDisruptorData;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.Response;

import io.cube.agent.logger.CubeLogMgr;
import io.cube.agent.logger.CubeLoggerFactoryProvider;
import io.md.logger.LogMgr;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

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

import org.slf4j.LoggerFactory;

import static io.cube.agent.Constants.*;

public class CommonConfig {

	private static Logger LOGGER;

	/******* PROPERTIES HOLDERS ******/
	// Cube essentials
	// Will not be dynamically polled. Will be initialised just once in the beginning
	public static String customerId, app, instance, serviceName;

	//intent
	public static String intent;

	// version
	public static String tag = "NA";
	public static String version = "NA";
	public static String fetchConfigApiURI;
	public static String ackConfigApiURI;
	public static int fetchDelay;
	public static int fetchConfigRetryCount;
	public static Future<?> fetchConfigFuture;
	public static boolean isFetchThreadInit;

	// Mocking
	public Optional<String> authToken;
	public List servicesToMock;

	// Cube Connection
	public final String CUBE_RECORD_SERVICE_URI;
	public final String CUBE_MOCK_SERVICE_URI;
	public final String CUBE_REPLAY_SERVICE_URI;
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

	public String recorderValue;
	public Recorder recorder;

	/******* OTHER OBJECTS ******/
	// Lightbend Config library by default throws exception on property not found
	// We need to ensure that at least NOOP properties are always defined in staticConfFile
	private static final String staticConfFile = "agent_conf.json";

	public static Config getEnvSysStaticConf() {
		return envSysStaticConf;
	}

	// Priority for default conf is envVar > sysProp > static_conf_file
	static Config envSysStaticConf;


	private static AtomicReference<CommonConfig> singleInstance = null;
	static ScheduledExecutorService serviceExecutor;
	private CloseableHttpClient httpClient;
	static protected ObjectMapper jsonMapper = new ObjectMapper();
	public static Map<String, String> clientMetaDataMap = new HashMap<>();

	//Websocket Logger Related
	public static Optional<String> loggerWsUri = Optional.empty();
	public static Optional<Boolean> loggingEnabled = Optional.empty();
	public static Optional<String> loggingLevel = Optional.empty();

	public static boolean onPrem = false;

	public static String externalIdField;

	static {

		try {
			envSysStaticConf = ConfigFactory.systemEnvironment()
					.withFallback(ConfigFactory.systemProperties())
					.withFallback(ConfigFactory.load(staticConfFile));

		} catch (Exception e) {
			LOGGER = LoggerFactory.getLogger(CommonConfig.class);
			LOGGER.error("Error while initializing config", e);
		}

		loggerWsUri = envSysStaticConf.hasPath(MD_LOGGERCONFIG_URI) ? Optional.of(envSysStaticConf.getString(MD_LOGGERCONFIG_URI)) : Optional.empty();
		loggingEnabled = envSysStaticConf.hasPath(MD_LOGGERCONFIG_ENABLE) ? Optional.of(envSysStaticConf.getBoolean(MD_LOGGERCONFIG_ENABLE)) : Optional.empty();
		loggingLevel = envSysStaticConf.hasPath(MD_LOGGERCONFIG_LEVEL) ? Optional.of(envSysStaticConf.getString(MD_LOGGERCONFIG_LEVEL)) : Optional.empty();

		jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		intent = envSysStaticConf.getString(Constants.MD_INTENT_PROP);
		customerId = envSysStaticConf.getString(Constants.MD_CUSTOMER_PROP);
		app = envSysStaticConf.getString(Constants.MD_APP_PROP);
		instance = envSysStaticConf.getString(Constants.MD_INSTANCE_PROP);
		serviceName = envSysStaticConf.getString(Constants.MD_SERVICE_PROP);
		externalIdField = envSysStaticConf.getString(Constants.MD_EXTERNAL_ID_FIELD); //ID to use instead of traceId

		//initialize Logging
		if(CubeLogMgr.isLoggingEnabled()){
			LogMgr.getInstance().setFactory(CubeLoggerFactoryProvider.getLoggerFactory());
		}
		LOGGER =  LogMgr.getLogger(CommonConfig.class);

		//Note: This is deliberately called before the CommonConfig instantiation for df support
		//because the MDTextMapCodec is initialized during CommonConfig initialization so appropriate
		//df suffix for "Cube" has to set before that.
		MDTextMapCodec.suffixKeysWithDF(app);

		CommonConfig config = null;
		try {
			config = new CommonConfig();
		} catch (Exception e) {
			LOGGER.error("Error in initialising common config object", e);
		}
		singleInstance = new AtomicReference<>();
		singleInstance.set(config);

		if (config != null) {
			config.recorder = initRecorder();
		}

		boolean isServerPolling = envSysStaticConf
			.getBoolean(MD_POLLINGCONFIG_POLLSERVER);

		// This is only for developer user case allowing polling properties from file
		// When polling from file the polling from cubeio will not be enabled.
		try {
			String dynamicConfigFilePath = envSysStaticConf
				.getString(MD_POLLINGCONFIG_FILEPATH);
			int delay = envSysStaticConf.getInt(MD_POLLINGCONFIG_DELAY);
			serviceExecutor = Executors.newScheduledThreadPool(1);
			serviceExecutor
				.scheduleWithFixedDelay(new FileConfigUpdater(dynamicConfigFilePath), 0, delay,
					TimeUnit.SECONDS);
			isServerPolling = false;
		} catch (Missing e) {
			LOGGER.info("Dynamic config polling from file is not enabled");
		}

		if (isServerPolling) {

			initClientMetaDataMap();

			String cubeServiceEndPoint = appendTrailingSlash(envSysStaticConf
				.getString(Constants.MD_SERVICE_ENDPOINT_PROP));

			fetchConfigApiURI = new URIBuilder(URI.create(cubeServiceEndPoint)
				.resolve(MD_FETCH_AGENT_CONFIG_API_PATH)).toString();

			ackConfigApiURI = new URIBuilder(URI.create(cubeServiceEndPoint)
				.resolve(MD_ACK_CONFIG_API_PATH)).toString();

			fetchDelay = envSysStaticConf.getInt(MD_POLLINGCONFIG_DELAY);
			fetchConfigRetryCount = envSysStaticConf.getInt(MD_POLLINGCONFIG_RETRYCOUNT);

			serviceExecutor = Executors.newScheduledThreadPool(1);
			fetchConfigFuture = serviceExecutor
				.scheduleWithFixedDelay(new ServerConfigUpdater(fetchConfigApiURI),
					0, fetchDelay,
					TimeUnit.SECONDS);
			LOGGER.info("CommonConfig fetchConfig thread scheduled!!");
			isFetchThreadInit = true;

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


	private static class FileConfigUpdater implements Runnable {

		String configFilePath;

		public FileConfigUpdater(String configFilePath) {
			this.configFilePath = configFilePath;
		}

		@Override
		public void run() {

			Thread.currentThread().setName("md-dynamicfile-thread");

			try {
				File input = new File(configFilePath);
				Config fileConfigPolled = ConfigFactory.load(ConfigFactory.parseFile(input))
					.withFallback(envSysStaticConf);

				//fetch previous disruptor config values and compare with new ones.
				//decision to init the ConsoleRecorder again or not.
				DisruptorData prevDisruptorData = savePrevDisruptorData();

				// Using just set instead of compareAndSet as the value being set is independent of the current value.
				singleInstance.set(new CommonConfig(fileConfigPolled));

				compAndInitRecorder(prevDisruptorData);
			} catch (Exception e) {
				LOGGER.error("Error in updating common config object in thread", e);
			}
		}
	}

	public static class ServerConfigUpdater implements Runnable {

		private String fetchConfigApiURI;

		public ServerConfigUpdater(String fetchConfigApiURI) {
			this.fetchConfigApiURI = fetchConfigApiURI;
		}

		@Override
		public void run() {

			Thread.currentThread().setName("md-polling-thread");

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
					val -> fetchConfigApiReq
						.setHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, val));

				Optional<CloseableHttpResponse> fetchConfigApiRespOpt = HttpUtils
					.getResponse(fetchConfigApiReq, Optional.of(fetchConfigRetryCount));
				CloseableHttpResponse fetchConfigApiResp = fetchConfigApiRespOpt
					.orElseThrow(() -> new Exception("Cannot get config from cube server"));

				try {
					// response can be CLIENT error so check for it
					if (Response.Status.Family
						.familyOf(fetchConfigApiResp.getStatusLine().getStatusCode())
						.equals(Response.Status.Family.CLIENT_ERROR)) {
						throw new Exception("Cannot get config from cube server");
					}

					if (fetchConfigApiResp.getStatusLine().getStatusCode()
						!= HttpStatus.SC_NOT_MODIFIED) {
						String jsonString = new BasicResponseHandler()
							.handleResponse(fetchConfigApiResp);
						JsonNode jsonNode = jsonMapper.readTree(jsonString);
						String configString = jsonNode.get("configJson").get("config")
							.asText();

						Config serverPollConfig = ConfigFactory.parseString(configString)
							.withFallback(envSysStaticConf);

						//fetch previous disruptor config values and compare with new ones.
						//decision to init the ConsoleRecorder again or not.
						DisruptorData prevDisruptorData = savePrevDisruptorData();

						// Using just set instead of compareAndSet as the value being set is independent of the current value.
						singleInstance.set(new CommonConfig(serverPollConfig));

						compAndInitRecorder(prevDisruptorData);

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
				} finally {
					fetchConfigApiResp.close();
				}

			} catch (Exception e) {
				try {
					ClientUtils.lock.readLock().lock();
					if (CommonConfig.onPrem && !fetchConfigApiURI
						.startsWith(CommonConfig.fetchConfigApiURI)) {
						LOGGER.info("On Prem installation, skipping the fetch from demo.dev");
						//For onPrem we shouldn't have to call demo.dev. Could be a delayed response
						return;
					}

					//This exception stack floods the logs, the stack trace does not provide any
					//additional information as this exception is likely thrown within this thread,
					// hence only printing the getMessage
					LOGGER.error(
						"Error in updating common config object in thread : " + e.getMessage());
					LOGGER.info("Resetting client to NORMAL mode!");
					//Not able to fetch config from MD server. Set the intent to normal.
					//Polling still continues, so when the MD server is reachable, it
					//will get the latest config.
					resetClient();
				} finally {
					ClientUtils.lock.readLock().unlock();
				}
			}
		}
	}

	public static void resetClient() {
		//These need to be reset when the MD Sever is not reachable.
		CommonConfig.intent = Constants.NO_INTENT;
		CommonConfig.tag = "NA";
		CommonConfig.version = "NA";
		CommonConfig.getInstance().servicesToMock = new ArrayList();
	}


	@Override
	public String toString() {
		return "CommonConfig{" +
			"CUBE_RECORD_SERVICE_URI='" + CUBE_RECORD_SERVICE_URI + '\'' +
			", CUBE_MOCK_SERVICE_URI='" + CUBE_MOCK_SERVICE_URI + '\'' +
			", CUBE_REPLAY_SERVICE_URI='" + CUBE_REPLAY_SERVICE_URI + '\'' +
			", intent=" + intent +
			", customerId='" + customerId + '\'' +
			", app='" + app + '\'' +
			", instance='" + instance + '\'' +
			", serviceName='" + serviceName + '\'' +
			", encryptionConfig=" + encryptionConfig +
			", sampler=" + sampler +
			", servicesToMock=" + servicesToMock +
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

		CUBE_RECORD_SERVICE_URI = dynamicConfig.getString(
			Constants.MD_SERVICE_ENDPOINT_PROP);
		CUBE_MOCK_SERVICE_URI = dynamicConfig.getString(
			Constants.MD_SERVICE_ENDPOINT_PROP);
		CUBE_REPLAY_SERVICE_URI = dynamicConfig.getString(
				Constants.MD_SERVICE_ENDPOINT_PROP);
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

		recorderValue = dynamicConfig.getString(io.cube.agent.Constants.RECORDER_PROP);

		getEncryptionDetails(dynamicConfig, options);

		authToken = Optional.of(dynamicConfig.getString(io.cube.agent.Constants.AUTH_TOKEN_PROP));

		if (CUBE_RECORD_SERVICE_URI.endsWith("/api/") || CUBE_MOCK_SERVICE_URI.endsWith("/api/")
				|| CUBE_REPLAY_SERVICE_URI.endsWith("/api/")) {
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

	public Recorder getRecorder() {
		return this.recorder;
	}

}
