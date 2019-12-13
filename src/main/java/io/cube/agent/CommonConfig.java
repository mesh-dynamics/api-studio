package io.cube.agent;

import java.util.Optional;
import java.util.Properties;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public CommonConfig() {
        properties = new java.util.Properties();
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
        } catch (Exception e) {
            LOGGER.error("Error while initializing config :: " + e.getMessage() );
        }
        CUBE_RECORD_SERVICE_URI = fromEnvOrProperties(Constants.MD_RECORD_SERVICE_PROP);
        CUBE_MOCK_SERVICE_URI = fromEnvOrProperties(Constants.CUBE_MOCK_SERVICE_PROP);
        READ_TIMEOUT = Integer.parseInt(fromEnvOrProperties(Constants.READ_TIMEOUT_PROP));
        CONNECT_TIMEOUT = Integer.parseInt(fromEnvOrProperties(Constants.CONNECT_TIMEOUT_PROP));
        RETRIES = Integer.parseInt(fromEnvOrProperties(Constants.RETRIES_PROP));
        customerId =fromEnvOrProperties(Constants.MD_CUSTOMER_PROP);
        app = fromEnvOrProperties(Constants.MD_APP_PROP);
        instance = fromEnvOrProperties(Constants.MD_INSTANCE_PROP);
        serviceName = fromEnvOrProperties(Constants.MD_SERVICE_PROP);
        intent = fromEnvOrProperties(Constants.MD_INTENT_PROP);

        Tracer tracer = CommonUtils.init("tracer");
        try {
            GlobalTracer.register(tracer);
        } catch (IllegalStateException e) {
            LOGGER.error("Trying to register a tracer when one is already registered");
        }

        LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
    }

    private String fromEnvOrProperties(String propertyName) {
        return CommonUtils.fromEnvOrSystemProperties(propertyName)
            .orElse(properties.getProperty(propertyName));
    }


}
