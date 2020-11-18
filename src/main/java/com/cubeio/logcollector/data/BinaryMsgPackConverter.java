package com.cubeio.logcollector.data;

import com.cubeio.logcollector.domain.DTO.LogStoreDTO;
import com.cubeio.logcollector.utils.CubeObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;

import java.nio.ByteBuffer;
import java.util.Optional;

public class BinaryMsgPackConverter {

    private static Logger LOGGER = LoggerFactory.getLogger(BinaryMsgPackConverter.class);

    private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
    private ObjectMapper msgPacker = CubeObjectMapperProvider.msgPacker;

    public Optional<LogStoreDTO> toLogStore(BinaryMessage message) {

        try{
            ByteBuffer bb = message.getPayload();
            byte[] buff = bb.array();
            ObjectMapper om = isMessagePack(buff) ? msgPacker : jsonMapper;
            return Optional.ofNullable(om.readValue(bb.array() , LogStoreDTO.class)) ;
        }catch (Exception e){
            LOGGER.error("Binary Message Parsing Failed ", e);
            return Optional.empty();
        }
    }

    private boolean isMessagePack(byte[] buff){
        for(byte b : buff){
            if(b<0) return true;
        }
        return false;
    }
}
