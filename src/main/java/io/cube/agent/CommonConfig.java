package io.cube.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.CustomJsonLayout;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.samplers.AdaptiveSampler;
import io.cube.agent.samplers.Attributes;
import io.cube.agent.samplers.BoundarySampler;
import io.cube.agent.samplers.CountingSampler;
import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SamplerConfig;
import io.cube.agent.samplers.SimpleSampler;
import io.md.constants.Constants;
import io.md.tracer.MDGlobalTracer;
import io.md.utils.CommonUtils;
import io.opentracing.Tracer;

public class CommonConfig {

	private static AtomicReference<CommonConfig> singleInstance = null;
	static ScheduledExecutorService serviceExecutor;

	private static final String STATIC_CONFFILE = "agent.conf";

	static Properties staticProperties = new Properties();
	static protected ObjectMapper jsonMapper = new ObjectMapper();

	public final String CUBE_RECORD_SERVICE_URI;
	public final String CUBE_MOCK_SERVICE_URI;
	public final int READ_TIMEOUT;
	public final int CONNECT_TIMEOUT;
	public final int RETRIES;

	private HttpRequest.Builder cubeRecordService;
	private HttpRequest.Builder cubeMockService;
	private HttpClient httpClient;

	private static final Logger LOGGER = LogManager.getLogger(CommonConfig.class);

	public static String intent;

	public String customerId, app, instance, serviceName;
	public final Optional<String> authToken;
	public final Optional<EncryptionConfig> encryptionConfig;
	public final Optional<SamplerConfig> samplerConfig;

	//Sampling
	public boolean samplerVeto;
	public Sampler sampler;
	public final boolean performanceTest;

	public List servicesToMock;

	private static class Updater implements Runnable {

		String configFilePath;

		public Updater(String configFilePath) {
			this.configFilePath = configFilePath;
		}

		@Override
		public void run() {
			try {
				InputStream input = new FileInputStream(configFilePath);
				Properties dynamicProperties = new Properties();
				dynamicProperties.load(input);
				// Using just set instead of compareAndSet as the value being set is independent of the current value.
				singleInstance.set(new CommonConfig(dynamicProperties));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in updating common config object in thread")), e);
			}
		}
	}

	@Override
	public String toString() {
		return "CommonConfig{" +
			"CUBE_RECORD_SERVICE_URI='" + CUBE_RECORD_SERVICE_URI + '\'' +
			", CUBE_MOCK_SERVICE_URI='" + CUBE_MOCK_SERVICE_URI + '\'' +
			", READ_TIMEOUT=" + READ_TIMEOUT +
			", CONNECT_TIMEOUT=" + CONNECT_TIMEOUT +
			", RETRIES=" + RETRIES +
			", customerId='" + customerId + '\'' +
			", app='" + app + '\'' +
			", instance='" + instance + '\'' +
			", serviceName='" + serviceName + '\'' +
			", encryptionConfig=" + encryptionConfig +
			", samplerConfig=" + samplerConfig +
			", intent=" + intent +
			", performance_test=" + performanceTest +
			'}';
	}

	public static CommonConfig getInstance() {
		return singleInstance.get();
	}

	public static void initializeLogging() {

		LogManager.setFactory(new MDLoggerContextFactory());
		LoggerContext context = (LoggerContext) LogManager.getContext();
		Configuration config = context.getConfiguration();

		CustomJsonLayout layout = CustomJsonLayout.newBuilder().setComplete(false).setEventEol(true)
			.setPropertiesAsList(false).setCompact(true).setProperties(false)
			.setIncludeStacktrace(true)
			.setLocationInfo(false).build();
		Appender appender = ConsoleAppender.newBuilder()
			.setDirect(true).setTarget(ConsoleAppender.Target.SYSTEM_OUT).setName("StdOut")
			.setLayout(layout).build();
		appender.start();

		AppenderRef ref = AppenderRef.createAppenderRef("CONSOLE_APPENDER", null, null);
		AppenderRef[] refs = new AppenderRef[]{ref};

		//public static LoggerConfig createLogger(@PluginAttribute(value = "additivity",defaultBoolean = true)
		// boolean additivity, @PluginAttribute("level") Level level,
		// @Required(message = "Loggers cannot be configured without a name") @PluginAttribute("name") String loggerName
		// , @PluginAttribute("includeLocation") String includeLocation,
		// @PluginElement("AppenderRef") AppenderRef[] refs, @PluginElement("Properties") Property[] properties
		// , @PluginConfiguration Configuration config, @PluginElement("Filter") Filter filter) {
		//
		LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO
			, "io.cube.agent", String.valueOf(false), refs, null, config, null);
		loggerConfig.addAppender(appender, null, null);
		config.addAppender(appender);
		config.addLogger("io.cube.agent", loggerConfig);
		context.updateLoggers(config);

		Logger logger = LogManager.getContext().getLogger("io.cube.agent");
		logger.info("HELLO_WORLD");
	}


	static {

		//ConfigurationFactory.setConfigurationFactory(new MDConfigurationFactory());
		initializeLogging();
		jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			staticProperties.load(Class.forName("io.cube.agent.CommonConfig").getClassLoader().
				getResourceAsStream(STATIC_CONFFILE));
			intent = fromEnvOrProperties(Constants.MD_INTENT_PROP)
				.orElseThrow(() -> new Exception("Mesh-D Intent Not Specified"));
		} catch (Exception e) {
			LOGGER.error(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Error while initializing con fig")),
				e);
		}

		CommonUtils.fromEnvOrSystemProperties(io.cube.agent.Constants.MD_COMMON_CONF_FILE_PROP)
			.ifPresent(dynamicConfigFilePath -> {

				int delay = Integer.parseInt(
					CommonUtils.fromEnvOrSystemProperties(
						io.cube.agent.Constants.MD_CONFIG_POLL_DELAY_PROP)
						.orElse(io.cube.agent.Constants.DEFAULT_CONFIG_POLL_DELAY));
				serviceExecutor = Executors.newScheduledThreadPool(1);
				serviceExecutor.scheduleWithFixedDelay(new Updater(dynamicConfigFilePath), 0, delay,
					TimeUnit.SECONDS);
				// TODO Where to call shutdown from - End of agent code ?
				// serviceExecutor.shutdown();
			});

		CommonConfig config = null;
		try {
			config = new CommonConfig();
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(
				Constants.MESSAGE, "Error in initialising common config object")), e);
		}
		singleInstance = new AtomicReference<>();
		singleInstance.set(config);
	}

	private CommonConfig() throws Exception {
		this(new Properties());
		System.setProperty("JAEGER_AGENT_HOST", "jaeger-agent");
		Tracer tracer = CommonUtils.init("tracer");
		try {
			MDGlobalTracer.register(tracer);
		} catch (IllegalStateException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Trying to register a tracer when one is already registered")), e);
		}
	}

	private CommonConfig(Properties dynamicProperties) throws Exception {
		CUBE_RECORD_SERVICE_URI = fromDynamicOREnvORStaticProperties(
			Constants.MD_RECORD_SERVICE_PROP, dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Record Endpoint Not Specified"));
		CUBE_MOCK_SERVICE_URI = fromDynamicOREnvORStaticProperties(Constants.MD_MOCK_SERVICE_PROP,
			dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Mock Endpoint Not Specified"));
		READ_TIMEOUT = Integer.parseInt(
			fromDynamicOREnvORStaticProperties(Constants.MD_READ_TIMEOUT_PROP, dynamicProperties).
				orElseThrow(() -> new Exception("Mesh-D Read Timeout Not Specified")));
		CONNECT_TIMEOUT = Integer.parseInt(
			fromDynamicOREnvORStaticProperties(Constants.MD_CONNECT_TIMEOUT_PROP, dynamicProperties)
				.orElseThrow(() -> new Exception("Mesh-D Connection Timeout Not Specified")));
		RETRIES = Integer.parseInt(
			fromDynamicOREnvORStaticProperties(Constants.MD_RETRIES_PROP, dynamicProperties).
				orElseThrow(() -> new Exception("Mesh-D Connection Retry Limit Not Specified")));
		customerId = fromDynamicOREnvORStaticProperties(Constants.MD_CUSTOMER_PROP,
			dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Customer Id Not Specified"));
		app = fromDynamicOREnvORStaticProperties(Constants.MD_APP_PROP, dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D App Name Not Specified"));
		instance = fromDynamicOREnvORStaticProperties(Constants.MD_INSTANCE_PROP, dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Instance Not Specified"));
		serviceName = fromDynamicOREnvORStaticProperties(Constants.MD_SERVICE_PROP,
			dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Service Name Not Specified"));
		intent = fromDynamicOREnvORStaticProperties(Constants.MD_INTENT_PROP, dynamicProperties)
			.orElseThrow(() -> new Exception("Mesh-D Intent Not Specified"));
		samplerConfig = fromDynamicOREnvORStaticProperties(
			io.cube.agent.Constants.SAMPLER_CONF_FILE_PATH,
			dynamicProperties).flatMap(scf -> {
			try {
				return Optional.of(jsonMapper.readValue(new File(scf), SamplerConfig.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in reading sampler config file")), e);
			}
			return Optional.empty();
		});
		sampler = initSampler();
		samplerVeto = BooleanUtils.toBoolean(
			fromDynamicOREnvORStaticProperties(Constants.MD_SAMPLER_VETO, dynamicProperties)
				.orElse("false"));
		// TODO Replace with constants once it comes in commons
//        fromEnvOrProperties(Constants.MD_ENCRYPTION_CONFIG_PATH).map(ecf -> {

		// TODO Should encryptionConfig be made static and not updated every time ?
		encryptionConfig = fromDynamicOREnvORStaticProperties(
			io.cube.agent.Constants.ENCRYPTION_CONF_FILE_PATH,
			dynamicProperties).flatMap(ecf -> {
			try {
				return Optional.of(jsonMapper.readValue(new File(ecf), EncryptionConfig.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in reading encryption config file")), e);
			}
			return Optional.empty();
		});

		authToken = fromDynamicOREnvORStaticProperties(
			io.cube.agent.Constants.AUTH_TOKEN_PROP,
			dynamicProperties);

		if (CUBE_RECORD_SERVICE_URI.endsWith("/api") || CUBE_MOCK_SERVICE_URI.endsWith("/api")) {
			authToken.orElseThrow(
				() -> new Exception("Auth token not specified when /api present"));
		}

		servicesToMock = fromDynamicOREnvORStaticProperties(
			io.cube.agent.Constants.SERVICES_TO_MOCK_PROP,
			dynamicProperties).map(serv -> Arrays.asList(serv.split(",")))
			.orElse(Collections.EMPTY_LIST);

		performanceTest = BooleanUtils.toBoolean(
			fromDynamicOREnvORStaticProperties(io.cube.agent.Constants.MD_PERFORMANCE_TEST
				, dynamicProperties).orElse("false"));

		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
			.build();
		cubeRecordService = HttpRequest.newBuilder();
		cubeMockService = HttpRequest.newBuilder();

		LOGGER.info(new ObjectMessage(
			Map.of(Constants.MESSAGE, "PROPERTIES POLLED :: " + this.toString())));
	}

	public HttpRequest getCubeRecordService() {
		return cubeRecordService
			.uri(URI.create(CUBE_RECORD_SERVICE_URI))
			.timeout(Duration.ofMillis(READ_TIMEOUT)).build();
	}

	public HttpRequest getCubeMockService() {
		return cubeRecordService
			.uri(URI.create(CUBE_MOCK_SERVICE_URI))
			.timeout(Duration.ofMillis(READ_TIMEOUT)).build();
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	private Optional<String> fromDynamicOREnvORStaticProperties(String propertyName,
		Properties dynamicProperty) {
		return Optional.ofNullable(dynamicProperty.getProperty(propertyName))
			.or(() -> fromEnvOrProperties(propertyName));
	}

	private static Optional<String> fromEnvOrProperties(String propertyName) {
		return CommonUtils.fromEnvOrSystemProperties(propertyName)
			.or(() -> Optional.ofNullable(staticProperties.getProperty(propertyName)));
	}

	private static String getConfigIntent() {
		return CommonConfig.intent;
	}

	public static String getCurrentIntent() {
		return getCurrentIntentFromScope().orElse(getConfigIntent());
	}

	public static Optional<String> getCurrentIntentFromScope() {
		return CommonUtils.getCurrentSpan().flatMap(span -> Optional.
			ofNullable(span.getBaggageItem(Constants.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY))).or(() ->
			Optional.ofNullable(CommonConfig.intent));
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

	Sampler initSampler() {
		if (samplerConfig.isEmpty()) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Invalid config file, not sampling!"
				)));
			return Sampler.NEVER_SAMPLE;
		}
		SamplerConfig config = samplerConfig.get();
		if (!validateSamplerConfig(config)) {
			return Sampler.NEVER_SAMPLE;
		}
		return createSampler(samplerConfig.get());

	}

	private boolean validateSamplerConfig(SamplerConfig config) {
		String type = config.getType();
		Optional<Integer> accuracy = config.getAccuracy();
		Optional<Float> rate = config.getRate();
		Optional<String> fieldCategory = config.getFieldCategory();
		Optional<List<Attributes>> attributes = config.getAttributes();

		if (type == null) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Sampler Type missing!"
				)));
			return false;
		}

		if ((type.equalsIgnoreCase(SimpleSampler.TYPE)
			|| type.equalsIgnoreCase(CountingSampler.TYPE))
			&& (rate.isEmpty() || accuracy.isEmpty())) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Need sampling rate/accuracy "
						+ "for Simple/Counting Samplers!"
				)));
			return false;
		}

		if (type.equalsIgnoreCase(BoundarySampler.TYPE)
			&& (rate.isEmpty() || accuracy.isEmpty()
			|| fieldCategory.isEmpty() || attributes.isEmpty())) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Need sampling rate/accuracy/"
						+ "fieldCategory/attributes "
						+ "for Boundary Sampler!"
				)));
			return false;
		}

		if (type.equalsIgnoreCase(AdaptiveSampler.TYPE)
			&& (fieldCategory.isEmpty() || attributes.isEmpty())) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Need field category/attributes "
						+ "for Adaptive Sampler!"
				)));
			return false;
		}

		return true;
	}

	Sampler createSampler(SamplerConfig samplerConfig) {
		String type = samplerConfig.getType();
		Optional<Integer> accuracy = samplerConfig.getAccuracy();
		Optional<Float> rate = samplerConfig.getRate();
		Optional<String> fieldCategory = samplerConfig.getFieldCategory();
		Optional<List<Attributes>> attributes = samplerConfig.getAttributes();

		if (SimpleSampler.TYPE.equalsIgnoreCase(type)) {
			return SimpleSampler.create(rate.get(), accuracy.get());
		}

		if (CountingSampler.TYPE.equalsIgnoreCase(type)) {
			return CountingSampler.create(rate.get(), accuracy.get());
		}

		//This sampler takes only a list of fields on which the sampling is to be done.
		//Specific values are not looked at.
		if (BoundarySampler.TYPE.equalsIgnoreCase(type)) {
			List<String> samplingParams = new ArrayList<>();
			for (Attributes attr : attributes.get()) {
				if (attr.getField() == null || attr.getField().isBlank()) {
					LOGGER.debug(new ObjectMessage(
						Map.of(
							Constants.MESSAGE, "Invalid input, using default sampler "
						)));
					samplingParams.clear();
					return Sampler.NEVER_SAMPLE;
				}
				samplingParams.add(attr.getField());
			}
			return BoundarySampler
				.create(rate.get(), accuracy.get(), fieldCategory.get(), samplingParams);
		}

		//This sampler takes fields, values and specific rates. However currently
		//only one field and multiple values are supported. Not multiple fields.
		//`other` is a special value that can be used to specify rate for every other
		//value that are possible for the specified field. Special value should be the
		//last in the rule hierarchy.
		if (AdaptiveSampler.TYPE.equalsIgnoreCase(type)) {
			//ordered map to allow special value `other` at the end.
			Map<Pair<String, String>, Float> samplingParams = new LinkedHashMap<>();
			for (Attributes attr : attributes.get()) {
				if (attr.getField() == null || attr.getField().isBlank()
					|| attr.getValue().isEmpty() || attr.getRate().isEmpty()) {
					LOGGER.debug(new ObjectMessage(
						Map.of(
							Constants.MESSAGE, "Invalid input, using default sampler "
						)));
					samplingParams.clear();
					return Sampler.NEVER_SAMPLE;
				}

				samplingParams.put(new ImmutablePair<>(attr.getField(), attr.getValue().get()),
					attr.getRate().get());
			}
			return AdaptiveSampler.create(fieldCategory.get(), samplingParams);
		}

		LOGGER.error(new ObjectMessage(
			Map.of(
				Constants.MESSAGE, "Invalid sampling strategy, using default sampler",
				Constants.MD_SAMPLER_TYPE, type
			)));
		return Sampler.NEVER_SAMPLE;
	}

}
