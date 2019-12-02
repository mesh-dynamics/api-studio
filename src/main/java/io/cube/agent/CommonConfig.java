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
    private static final String CUBE_RECORD_SERVICE_PROP = "cube_record_service";
    private static final String CUBE_MOCK_SERVICE_PROP = "cube_mock_service";
    private static final String READ_TIMEOUT_PROP = "read_timeout";
    private static final String CONNECT_TIMEOUT_PROP = "connect_timeout";
    private static final String RETRIES_PROP = "retries";
    public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";
    public static final String DEFAULT_SPAN_FIELD = "x-b3-spanid";
    public static final String DEFAULT_PARENT_SPAN_FIELD = "x-b3-parentspanid";
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
        CUBE_RECORD_SERVICE_URI = fromEnvOrProperties(CUBE_RECORD_SERVICE_PROP , "http://cubews:9080");
        CUBE_MOCK_SERVICE_URI = fromEnvOrProperties(CUBE_MOCK_SERVICE_PROP , "http://cubews:9080");
        READ_TIMEOUT = Integer.valueOf(fromEnvOrProperties(READ_TIMEOUT_PROP , "100000"));
        CONNECT_TIMEOUT = Integer.valueOf(fromEnvOrProperties(CONNECT_TIMEOUT_PROP , "100000"));
        RETRIES = Integer.valueOf(fromEnvOrProperties(RETRIES_PROP , "3"));
        customerId = fromEnvOrProperties("customer_dogfood" , "ravivj");
        app = fromEnvOrProperties("app_dogfood" , "cubews");
        instance = fromEnvOrProperties("instance_dogfood" , "dev");
        serviceName = fromEnvOrProperties("service_dogfood" , "cube");
        intent = fromEnvOrProperties("intent" , CommonUtils.NO_INTENT);

        Tracer tracer = CommonUtils.init("tracer");
        try {
            GlobalTracer.register(tracer);
        } catch (IllegalStateException e) {
            LOGGER.error("Trying to register a tracer when one is already registered");
        }

        LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
    }

    private String fromEnvOrProperties(String propertyName, String defaultValue) {
        String fromEnv = Optional.ofNullable(System.getenv(propertyName))
            .orElse(System.getProperty(propertyName));
        if (fromEnv != null) {
            return fromEnv;
        }
        return  properties.getProperty(propertyName , defaultValue);
    }

}
