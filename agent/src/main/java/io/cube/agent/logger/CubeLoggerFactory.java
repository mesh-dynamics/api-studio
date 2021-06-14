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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class CubeLoggerFactory implements io.md.logger.LoggerFactory {

    private static Map<String , Logger> classLoggers = new HashMap<>();
    public static final CubeLoggerFactory singleton = new CubeLoggerFactory();

    public  Logger getLogger(Class<?> clazz){
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
