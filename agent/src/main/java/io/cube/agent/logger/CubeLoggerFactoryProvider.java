package io.cube.agent.logger;

import io.md.logger.LoggerFactory;

public class CubeLoggerFactoryProvider {
    public static LoggerFactory getLoggerFactory(){
        return CubeLoggerFactory.singleton;
    }
}
