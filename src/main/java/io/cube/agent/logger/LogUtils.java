package io.cube.agent.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

class LogUtils {
    private static Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    private static ObjectMapper mapper = ObjectMapperProvider.getInstance();
    private static ObjectMapper msgPacker = ObjectMapperProvider.msgPacker;

    public static<T,R> Optional<R> safeExe(Function< T , ? extends R >  fn , T val){
        try{
            return Optional.ofNullable(fn.apply(val));
        }catch (Exception e){
            return Optional.empty();
        }
    }

    public static String toJson(LogStoreDTO logStoreDTO){

        try{
            return mapper.writeValueAsString(logStoreDTO);
        }catch (Exception e){
          LOGGER.error("toJson error ", e);
          return logStoreDTO.logMessage;
        }
    }

    public static byte[] toMsgPack(LogStoreDTO logStoreDTO){

        try{
            return msgPacker.writeValueAsBytes(logStoreDTO);
        }catch (Exception e){
            LOGGER.error("toJson error ", e);
            return logStoreDTO.logMessage.getBytes();
        }
    }


}