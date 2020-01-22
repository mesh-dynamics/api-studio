package io.cube.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

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

	private static final Logger LOGGER = LogManager.getLogger(CommonConfig.class);

	public static String intent;

	public String customerId, app, instance, serviceName;
	public final Optional<EncryptionConfig> encryptionConfig;

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
				singleInstance.compareAndSet(singleInstance.get(), new CommonConfig(dynamicProperties));
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
			'}';
	}

		public static CommonConfig getInstance() throws Exception {

		if (singleInstance == null) {
			CommonConfig config = new CommonConfig();
			singleInstance = new AtomicReference<>();
			singleInstance.set(config);

			CommonUtils.fromEnvOrSystemProperties(io.cube.agent.Constants.MD_COMMON_CONF_FILE_PROP)
				.ifPresent(dynamicConfigFilePath -> {

				    int delay = Integer.parseInt(
				        CommonUtils.fromEnvOrSystemProperties(io.cube.agent.Constants.MD_CONFIG_POLL_DELAY_PROP)
                            .orElse(io.cube.agent.Constants.DEFAULT_CONFIG_POLL_DELAY));
					serviceExecutor = Executors.newScheduledThreadPool(1);
					serviceExecutor.scheduleWithFixedDelay(new Updater(dynamicConfigFilePath), 0, delay,
						TimeUnit.SECONDS);
					// TODO Where to call shutdown from - End of agent code ?
					// serviceExecutor.shutdown();
				});
		}

		return singleInstance.get();
	}

	static {
		jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			staticProperties.load(Class.forName("io.cube.agent.CommonConfig").getClassLoader().
				getResourceAsStream(STATIC_CONFFILE));
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,"Error while initializing config")),e);
		}

	}

	private CommonConfig() throws Exception {

		CUBE_RECORD_SERVICE_URI = fromEnvOrProperties(Constants.MD_RECORD_SERVICE_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Record Endpoint Not Specified"));
		CUBE_MOCK_SERVICE_URI = fromEnvOrProperties(Constants.MD_MOCK_SERVICE_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Mock Endpoint Not Specified"));
		READ_TIMEOUT = Integer.parseInt(fromEnvOrProperties(Constants.MD_READ_TIMEOUT_PROP).
			orElseThrow(() -> new Exception("Mesh-D Read Timeout Not Specified")));
		CONNECT_TIMEOUT = Integer.parseInt(fromEnvOrProperties(Constants.MD_CONNECT_TIMEOUT_PROP).
			orElseThrow(() -> new Exception("Mesh-D Connection Timeout Not Specified")));
		RETRIES = Integer.parseInt(fromEnvOrProperties(Constants.MD_RETRIES_PROP).
			orElseThrow(() -> new Exception("Mesh-D Connection Retry Limit Not Specified")));
		customerId = fromEnvOrProperties(Constants.MD_CUSTOMER_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Customer Id Not Specified"));
		app = fromEnvOrProperties(Constants.MD_APP_PROP)
			.orElseThrow(() -> new Exception("Mesh-D App Name Not Specified"));
		instance = fromEnvOrProperties(Constants.MD_INSTANCE_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Instance Not Specified"));
		serviceName = fromEnvOrProperties(Constants.MD_SERVICE_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Service Name Not Specified"));
		intent = fromEnvOrProperties(Constants.MD_INTENT_PROP)
			.orElseThrow(() -> new Exception("Mesh-D Intent Not Specified"));
		// TODO Replace with constants once it comes in commons
//        fromEnvOrProperties(Constants.MD_ENCRYPTION_CONFIG_PATH).map(ecf -> {

		encryptionConfig = fromEnvOrProperties("io.md.encryptionconfig.path").flatMap(ecf -> {
			try {
				return Optional.of(jsonMapper.readValue(new File(ecf), EncryptionConfig.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in reading encryption config file",
					Constants.EXCEPTION_STACK, Arrays.toString(e.getStackTrace())
				)));
			}
			return Optional.empty();
		});

		Tracer tracer = CommonUtils.init("tracer");
		try {
			GlobalTracer.register(tracer);
		} catch (IllegalStateException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,"Trying to register a tracer when one is already registered")),e);
		}
		LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
	}

	//This constructor is called only for updates.
	// Assumption - Called after argument less constructor
	// So the static fields jsonMapper, staticProperties are all instantiated
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
				.
					orElseThrow(() -> new Exception("Mesh-D Connection Timeout Not Specified")));
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
		// TODO Replace with constants once it comes in commons
//        fromEnvOrProperties(Constants.MD_ENCRYPTION_CONFIG_PATH).map(ecf -> {

		// TODO Should encryptionConfig be made static and not updated every time ?
		encryptionConfig = fromDynamicOREnvORStaticProperties("io.md.encryptionconfig.path",
			dynamicProperties).flatMap(ecf -> {
			try {
				return Optional.of(jsonMapper.readValue(new File(ecf), EncryptionConfig.class));
			} catch (Exception e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in reading encryption config file")), e);
			}
			return Optional.empty();
		});

		LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
	}

	private Optional<String> fromDynamicOREnvORStaticProperties(String propertyName,
		Properties dynamicProperty) {
		return Optional.ofNullable(dynamicProperty.getProperty(propertyName)).or(() ->
			CommonUtils.fromEnvOrSystemProperties(propertyName)).or(() ->
			Optional.ofNullable(staticProperties.getProperty(propertyName)));
	}

	private Optional<String> fromEnvOrProperties(String propertyName) {
		return CommonUtils.fromEnvOrSystemProperties(propertyName)
			.or(() -> Optional.ofNullable(staticProperties.getProperty(propertyName)));
	}

	public static String getConfigIntent() {
		return CommonConfig.intent;
	}

	public static String getCurrentIntent() {
		return getCurrentIntentFromScope().orElse(getConfigIntent());
	}

	public static Optional<String> getCurrentIntentFromScope() {
		Optional<String> currentIntent = CommonUtils.getCurrentSpan().flatMap(span -> Optional.
			ofNullable(span.getBaggageItem(Constants.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY))).or(() ->
			CommonUtils.fromEnvOrSystemProperties(Constants.MD_INTENT_PROP));
		LOGGER.info("Got intent from trace (in agent) :: " +
			currentIntent.orElse(" N/A"));
		return currentIntent;
	}

	public static boolean isIntentToRecord() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_RECORD);
	}

	public static boolean isIntentToMock() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_MOCK);
	}

}
