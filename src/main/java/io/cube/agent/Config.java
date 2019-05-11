package io.cube.agent;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO make this config file singleton and inject it
public class Config {

    private static final String CONFFILE = "agent.conf";

    final Properties properties;
    public final String CUBE_RECORD_SERVICE_URI;
    public final String CUBE_MOCK_SERVICE_URI;

    private static final Logger LOGGER = LogManager.getLogger(Config.class);

    public Config() {
        properties = new java.util.Properties();
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
        } catch (Exception e) {
            LOGGER.error("Error while initializing config :: " + e.getMessage() );
        }
        CUBE_RECORD_SERVICE_URI = properties.getProperty("cube_record_service" , "http://cubews:9080");
        CUBE_MOCK_SERVICE_URI = properties.getProperty("cube_mock_service" , "http://cubews:9080");
        LOGGER.info("CUBE MOCK SERVICE :: " + CUBE_MOCK_SERVICE_URI);
    }


}
