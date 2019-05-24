package io.cube.agent;

import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO make this config file singleton and inject it
public class Config {

    private static final String CONFFILE = "agent.conf";

    final Properties properties;
    public final String CUBE_RECORD_SERVICE_URI;
    public final String CUBE_MOCK_SERVICE_URI;
    public final int READ_TIMEOUT;
    public final int CONNECT_TIMEOUT;
    public final int RETRIES;

    private static final Logger LOGGER = LogManager.getLogger(Config.class);
    private static final String CUBE_RECORD_SERVICE_PROP = "cube_record_service";
    private static final String CUBE_MOCK_SERVICE_PROP = "cube_mock_service";
    private static final String READ_TIMEOUT_PROP = "read_timeout";
    private static final String CONNECT_TIMEOUT_PROP = "connect_timeout";
    private static final String RETRIES_PROP = "retries";

    public Config() {
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
        LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
    }

    private String fromEnvOrProperties(String propertyName, String defaultValue) {
        String fromEnv =  System.getenv(propertyName);
        if (fromEnv != null) {
            return fromEnv;
        }
        return  properties.getProperty(propertyName , defaultValue);
    }

}
