package io.cube.agent.logger;

import io.cube.agent.CommonConfig;
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
    public static boolean msgPackTransport = false;

    static {

        Optional<Boolean> logEnabled =  CommonConfig.loggingEnabled;
        loggingEnabled = !logEnabled.isPresent() || logEnabled.get();

        if(!CommonConfig.loggerWsUri.isPresent()){
            loggingEnabled = false;
            LOGGER.error("Logging websocket Url not set");
        }

        CommonConfig singleton = CommonConfig.getInstance();
        Optional<String> authToken = singleton.authToken;
        if(authToken==null || !authToken.isPresent()){
            loggingEnabled = false;
            LOGGER.error("auth token missing. disabling logging");
        }

        if(loggingEnabled){
            String uri = CommonConfig.loggerWsUri.get();
            token = authToken.get();
            try{
                client = CubeWsClient.create(uri , token);
                client.connect();

            }catch (Exception e){
                loggingEnabled = false;
                LOGGER.error("CubeWsClient create error ", e);
            }
            //Set the logging level
            CommonConfig.loggingLevel.flatMap(l->LogUtils.safeExe(Level::valueOf , l)).ifPresent(level->{logLevel = level;});
            cubeDeployment = new CubeDeployment(CommonConfig.app , CommonConfig.instance , CommonConfig.serviceName , CommonConfig.customerId , CommonConfig.version);
        }
    }

    protected static Logger getLogger(String className){

        if(!loggingEnabled) {
            LOGGER.error("Logging is disabled");
            return NOPLogger.NOP_LOGGER;
        }

        return new CubeLogger(className , logLevel, client);
    }
}
