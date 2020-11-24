package com.cubeio.logcollector.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.logger.LogStoreDTO;
import io.md.utils.CubeObjectMapperProvider;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;

import java.nio.ByteBuffer;
import java.util.Optional;

public class BinaryMsgPackConverter {

    private static Logger LOGGER = LoggerFactory.getLogger(BinaryMsgPackConverter.class);

    private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
    private ObjectMapper msgPacker = CubeObjectMapperProvider.createMapper(new MessagePackFactory());

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
