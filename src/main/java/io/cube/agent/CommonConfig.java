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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

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

	private WebTarget cubeRecordService;
	private WebTarget cubeMockService;

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
			'}';
	}

	public static CommonConfig getInstance() {
		return singleInstance.get();
	}

	static {
		CommonConfig config = null;
		try {
			config = new CommonConfig();
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(
				Constants.MESSAGE, "Error in initialising common config object")), e);
		}
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

		jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


		try {
			staticProperties.load(Class.forName("io.cube.agent.CommonConfig").getClassLoader().
				getResourceAsStream(STATIC_CONFFILE));
			intent = fromEnvOrProperties(Constants.MD_INTENT_PROP)
				.orElseThrow(() -> new Exception("Mesh-D Intent Not Specified"));
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,"Error while initializing config")),e);
		}

	}

	private CommonConfig() throws Exception {
		this(new Properties());
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


		ClientConfig clientConfig = new ClientConfig()
			.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT)
			.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		Client restClient = ClientBuilder.newClient(clientConfig);
		cubeRecordService = restClient.target(CUBE_RECORD_SERVICE_URI);
		cubeMockService = restClient.target(CUBE_MOCK_SERVICE_URI);


		Tracer tracer = CommonUtils.init("tracer");
		try {
			GlobalTracer.register(tracer);
		} catch (IllegalStateException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,"Trying to register a tracer when one is already registered")),e);
		}

		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE,"CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI)));

	}

	public WebTarget getCubeRecordService() {
		return cubeRecordService;
	}

	public WebTarget getCubeMockService() {
		return cubeMockService;
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

	private static Optional<String> getCurrentIntentFromScope() {
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
