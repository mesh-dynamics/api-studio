package com.cubeio.logcollector.utils;

import com.cubeio.logcollector.domain.DTO.LogStoreDTO;
import org.apache.logging.log4j.message.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.Map;

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
    public static void log(Logger LOGGER , LogStoreDTO dto){
        Object obj = new ObjectMessage(Map.of("customerId", dto.customerId ,
                "app", dto.app, "instance", dto.instance, "service", dto.service,
                "version", dto.version, "sourceType", dto.sourceType,
                "logMessage", dto.logMessage, "clientTimeStamp", dto.clientTimeStamp));
        log(LOGGER , dto.level , obj);
    }
}
