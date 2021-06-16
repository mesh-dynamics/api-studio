/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent.logger;

import io.cube.agent.CommonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.NOPLogger;

import java.util.Optional;

public class CubeLogMgr {

    private static final Logger LOGGER = LoggerFactory.getLogger(CubeLogMgr.class);

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
            //LOGGER.info("token is "+token);
            try{
                client = CubeWsClient.create(uri , token , CommonConfig.customerId);
                client.connect();

            }catch (Exception e){
                loggingEnabled = false;
                LOGGER.error("CubeWsClient create error ", e);
            }

            //Set the logging level
            logLevel = CommonConfig.loggingLevel.map(String::toUpperCase).map(l->{
                try{ return Level.valueOf(l); }catch(Exception e){ return null;}
            }).orElse(logLevel);
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
