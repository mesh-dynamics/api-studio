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

package com.cubeio.logcollector.utils;

import io.md.logger.LogStoreDTO;
import org.apache.logging.log4j.message.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LogUtils {
    public static void log(Logger LOGGER , Level level , Object obj){
        String message = obj.toString();
        switch (level){
            case INFO: LOGGER.info(message); break;
            case TRACE: LOGGER.trace(message); break;
            case DEBUG: LOGGER.debug(message); break;
            case WARN: LOGGER.warn(message); break;
            case ERROR:
            default: LOGGER.error(message); break;
        }
    }
    public static void log(Logger LOGGER , LogStoreDTO dto , Optional<Map<String , Object>> meta){
        Map<String , Object> data =  Map.of("customerId", dto.customerId ,
                "app", dto.app, "instance", dto.instance, "service", dto.service,
                "version", dto.version, "sourceType", dto.sourceType,
                "logMessage", dto.logMessage, "clientTimeStamp", dto.clientTimeStamp);
        if(meta.isPresent()){
            data = new HashMap<>(data);
            data.putAll(meta.get());
        }
        Object obj = new ObjectMessage(data);
        log(LOGGER , dto.level , obj);
    }
}
