package io.cube.agent.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CubeLoggerFactory {

    private static Map<String , Logger> classLoggers = new HashMap<>();
    public static Logger getLogger(Class<?> clazz){
        Logger slf4jLogger = LoggerFactory.getLogger(clazz);
        Logger cubeLogger = getCubeLogger(clazz.getName());
        return new LogWrapper(slf4jLogger , cubeLogger);
    }

    private static Logger getCubeLogger(String className){

        Logger logger = classLoggers.get(className);
        if(logger==null){
            synchronized (CubeLoggerFactory.class){
                logger = classLoggers.get(className);
                if(logger ==null){
                    logger = CubeLogMgr.getLogger(className);
                    classLoggers.put(className , logger);
                }
            }
        }
        return logger;
    }

}
