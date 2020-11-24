package io.cube.agent.logger;

import io.cube.agent.CommonConfig;
import io.md.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.NOPLogger;

import java.util.Optional;

public class CubeLogMgr {

    private static Logger LOGGER = LoggerFactory.getLogger(CubeLogMgr.class);

    public static void setLoggingEnabled(boolean loggingEnabled) {
        CubeLogMgr.loggingEnabled = loggingEnabled;
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    private static volatile boolean loggingEnabled;
    private static CubeWsClient client;
    private static Level logLevel = Level.TRACE;
    private static String token;
    public static CubeDeployment cubeDeployment;
    public static boolean msgPackTransport = true;

    static {

        loggingEnabled = CommonConfig.loggingEnabled.orElse(true);

        if(!CommonConfig.loggerWsUri.isPresent()){
            loggingEnabled = false;
            LOGGER.error("Logging websocket Url not set");
        }

        Optional<String> authToken = Optional.ofNullable(CommonConfig.getEnvSysStaticConf().getString(io.cube.agent.Constants.AUTH_TOKEN_PROP));
        if(!authToken.isPresent()){
            loggingEnabled = false;
            LOGGER.error("auth token missing. disabling logging");
        }

        if(loggingEnabled){
            String uri = CommonConfig.loggerWsUri.get();
            token = authToken.get();
            try{
                client = CubeWsClient.create(uri , token , CommonConfig.customerId);
                client.connect();

            }catch (Exception e){
                loggingEnabled = false;
                LOGGER.error("CubeWsClient create error ", e);
            }

            //Set the logging level
            logLevel = CommonConfig.loggingLevel.map(String::toUpperCase).flatMap(l->Utils.safeFnExecute(l , Level::valueOf)).orElse(logLevel);
            cubeDeployment = new CubeDeployment(CommonConfig.app , CommonConfig.instance , CommonConfig.serviceName , CommonConfig.customerId , CommonConfig.version);
        }
    }

    protected static Logger getLogger(String className){

        if(!loggingEnabled) {
            LOGGER.error("Logging is disabled {}" , className);
            return NOPLogger.NOP_LOGGER;
        }

        return new CubeLogger(className , logLevel, client);
    }
}
