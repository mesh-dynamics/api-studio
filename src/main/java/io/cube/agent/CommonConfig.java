package io.cube.agent;

import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

// TODO make this config file singleton and inject it
public class CommonConfig {

    private static final String CONFFILE = "agent.conf";

    final Properties properties;
    public final String CUBE_RECORD_SERVICE_URI;
    public final String CUBE_MOCK_SERVICE_URI;
    public final int READ_TIMEOUT;
    public final int CONNECT_TIMEOUT;
    public final int RETRIES;

    private static final Logger LOGGER = LogManager.getLogger(CommonConfig.class);

    public static String intent;

    public String customerId, app, instance, serviceName;

    public CommonConfig() throws Exception {
        properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
        } catch (Exception e) {
            LOGGER.error("Error while initializing config :: " + e.getMessage() );
        }
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
        customerId =fromEnvOrProperties(Constants.MD_CUSTOMER_PROP)
            .orElseThrow(() -> new Exception("Mesh-D Customer Id Not Specified"));
        app = fromEnvOrProperties(Constants.MD_APP_PROP)
            .orElseThrow(() -> new Exception("Mesh-D App Name Not Specified"));
        instance = fromEnvOrProperties(Constants.MD_INSTANCE_PROP)
            .orElseThrow(() -> new Exception("Mesh-D Instance Not Specified"));
        serviceName = fromEnvOrProperties(Constants.MD_SERVICE_PROP)
            .orElseThrow(() -> new Exception("Mesh-D Service Name Not Specified"));
        intent = fromEnvOrProperties(Constants.MD_INTENT_PROP)
            .orElseThrow(() -> new Exception("Mesh-D Intent Not Specified"));

        Tracer tracer = CommonUtils.init("tracer");
        try {
            GlobalTracer.register(tracer);
        } catch (IllegalStateException e) {
            LOGGER.error("Trying to register a tracer when one is already registered");
        }

        LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
    }

    private Optional<String> fromEnvOrProperties(String propertyName) {
        return CommonUtils.fromEnvOrSystemProperties(propertyName)
            .or(() -> Optional.ofNullable(properties.getProperty(propertyName)));
    }

    public static String getConfigIntent() {
        return CommonConfig.intent;
    }

    public static String getCurrentIntent() {
        return getCurrentIntentFromScope().orElse(getConfigIntent());
    }

    public static Optional<String> getCurrentIntentFromScope() {
        Optional<String> currentIntent =  CommonUtils.getCurrentSpan().flatMap(span -> Optional.
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
