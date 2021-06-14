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

package io.md.logger;

import org.slf4j.Logger;

public class LogMgr {

    /*
     Default Slf4J Log Factory
     */
    private volatile LoggerFactory factory = new Slf4jLoggerFactory();
    private static final LogMgr singleton = new LogMgr();
    private static volatile boolean loggerCalled = false;

    public static LogMgr getInstance(){
        return singleton;
    }

    //set different log factory. called by agent to set
    public void setFactory(LoggerFactory loggerFactory){
        if(loggerCalled) {
            loggerFactory.getLogger(LogMgr.class).error("LogMgr setFactory is called after Logger Instantiation. Should not happen !!!!");
        }
        this.factory = loggerFactory;
    }

    /*
      Direct Utility method to get Logger from current LoggerFactory
     */
    public static Logger getLogger(Class<?> clazz){
        loggerCalled = true;
        return singleton.factory.getLogger(clazz);
    }
}
