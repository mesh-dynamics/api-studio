package io.cube.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.logging.MDConfigurationFactory;
import io.cube.agent.samplers.AdaptiveSampler;
import io.cube.agent.samplers.BoundarySampler;
import io.cube.agent.samplers.CountingSampler;
import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SamplerAttributes;
import io.cube.agent.samplers.SamplerConfig;
import io.cube.agent.samplers.SimpleSampler;
import io.md.constants.Constants;
import io.md.tracer.MDGlobalTracer;
import io.md.utils.CommonUtils;
import io.opentracing.Tracer;

//import javax.ws.rs.client.Client;

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

	public String customerId, app, instance, serviceName, samplerType;
	public final Optional<EncryptionConfig> encryptionConfig;
	public final Optional<SamplerConfig> samplerConfig;

	//Sampling
	//public String samplingConfFilePath;
	public Number samplerRate, samplerAccuracy;
	public boolean samplerVeto;
	public List<String> headerParams;
	public Sampler sampler;
	public final boolean performanceTest;

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

	static {
		ConfigurationFactory.setConfigurationFactory(new MDConfigurationFactory());
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
		sampler = initSampler();
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

		performanceTest = BooleanUtils.toBoolean(
			fromDynamicOREnvORStaticProperties(io.cube.agent.Constants.MD_PERFORMANCE_TEST
				, dynamicProperties).orElse("false"));

		ClientConfig clientConfig = new ClientConfig()
			.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT)
			.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		Client restClient = ClientBuilder.newClient(clientConfig);
		cubeRecordService = restClient.target(CUBE_RECORD_SERVICE_URI);
		cubeMockService = restClient.target(CUBE_MOCK_SERVICE_URI);

		LOGGER.info(new ObjectMessage(
			Map.of(Constants.MESSAGE, "PROPERTIES POLLED :: " + this.toString())));
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

	private static Optional<Number> getPropertyAsNum(String value) {
		if (value != null) {
			try {
				return Optional.of(NumberFormat.getInstance().parse(value));
			} catch (ParseException e) {
				LOGGER.error(
					"Failed to parse number for property samplerRate with value '" + value + "'",
					e.getMessage());
			}
		}
		return Optional.empty();
	}

	private List<String> getPropertyAsList(String headerParams) {
		return Arrays.asList(headerParams.split(","));
	}

	Sampler initSampler() {
		if (samplerConfig.isEmpty()) {
			LOGGER.debug(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Invalid config file, recording all requests!"
				)));
			return Sampler.ALWAYS_SAMPLE;
		}
		return createSampler(samplerConfig.get());

	}

	Sampler createSampler(SamplerConfig samplerConfig) {
		String samplerType = samplerConfig.getSamplerType();
		int samplingAccuracy = samplerConfig.getSamplerAccuracy();
		String samplingID = samplerConfig.getSamplingID();
		List<SamplerAttributes> samplerAttributes = samplerConfig.getSamplerAttributes();

		if (samplerAttributes == null || samplerAttributes.isEmpty()) {
			LOGGER.error(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Missing sampler Attributes, using default sampler",
					Constants.MD_SAMPLER_TYPE, samplerType
				)));
			return SimpleSampler
				.create(SimpleSampler.DEFAULT_SAMPLING_RATE,
					SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
		}

		if (SimpleSampler.TYPE.equalsIgnoreCase(samplerType)) {
			SamplerAttributes samplerAttribute = samplerAttributes.get(0);
			return SimpleSampler.create(samplerAttribute.getSamplingRate(), samplingAccuracy);
		}

		if (CountingSampler.TYPE.equalsIgnoreCase(samplerType)) {
			SamplerAttributes samplerAttribute = samplerAttributes.get(0);
			return CountingSampler.create(samplerAttribute.getSamplingRate(), samplingAccuracy);
		}

		//This sampler takes only a list of fields on which the sampling is to be done.
		//Specific values are not looked at. Only one sampling probability will be used
		//even if different probabilities are give in the config file and this probability
		//will be the first available probability.
		if (BoundarySampler.TYPE.equalsIgnoreCase(samplerType)) {
			if (!isSamplerIdentifierPresent(samplerType, samplingID)) {
				return SimpleSampler
					.create(SimpleSampler.DEFAULT_SAMPLING_RATE,
						SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
			}

			List<String> samplingParams = new ArrayList<>();
			float samplingRate = -1.0f;
			for (SamplerAttributes attr : samplerAttributes) {
				if (attr.getSamplingField().isEmpty() || attr.getSamplingField().isBlank()) {
					samplingParams.clear();
					return SimpleSampler
						.create(SimpleSampler.DEFAULT_SAMPLING_RATE,
							SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
				}
				samplingParams.add(attr.getSamplingField());
				if (samplingRate < 0f) {
					samplingRate = attr.getSamplingRate();
				}
			}
			return BoundarySampler
				.create(samplingRate, samplingAccuracy, samplingID, samplingParams);
		}

		if (AdaptiveSampler.TYPE.equalsIgnoreCase(samplerType)) {
			if (isSamplerIdentifierPresent(samplerType, samplingID)) {
				return SimpleSampler
					.create(SimpleSampler.DEFAULT_SAMPLING_RATE,
						SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
			}

			MultivaluedMap<String, Pair<String, Float>> samplingParams = new MultivaluedHashMap<>();
			for (SamplerAttributes attr : samplerAttributes) {
				if (attr.getSamplingField() == null || attr.getSamplingField().isEmpty() || attr
					.getSamplingField()
					.isBlank() || attr.getSamplingValue() == null || attr.getSamplingValue()
					.isEmpty()
					|| attr.getSamplingValue().isBlank()) {
					return SimpleSampler
						.create(SimpleSampler.DEFAULT_SAMPLING_RATE,
							SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
				}
				Optional<Sampler> sampler = Utils
					.getSampler(attr.getSamplingRate(), samplingAccuracy);
				if (sampler.isPresent()) {
					LOGGER.error(new ObjectMessage(
						Map.of(
							Constants.MESSAGE,
							"Invalid sampling rate/accuracy, using default sampler",
							Constants.MD_SAMPLER_TYPE, samplerType
						)));
					samplingParams.clear();
					return sampler.get();
				}
				samplingParams.add(attr.getSamplingField(),
					new ImmutablePair<>(attr.getSamplingValue(), attr.getSamplingRate()));
			}
			return AdaptiveSampler.create(samplingID, samplingAccuracy, samplingParams);
		}

		LOGGER.error(new ObjectMessage(
			Map.of(
				Constants.MESSAGE, "Invalid sampling strategy, using default values",
				Constants.MD_SAMPLER_TYPE, samplerType
			)));
		return SimpleSampler
			.create(SimpleSampler.DEFAULT_SAMPLING_RATE, SimpleSampler.DEFAULT_SAMPLING_ACCURACY);
	}

	private boolean isSamplerIdentifierPresent(String samplerType, String samplerIdentifier) {
		if (samplerIdentifier == null || samplerIdentifier.isEmpty() || samplerIdentifier
			.isBlank()) {
			LOGGER.error(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Missing Sampler Identifier, using default sampler",
					Constants.MD_SAMPLER_TYPE, samplerType
				)));
			return false;
		}
		return true;
	}

}
