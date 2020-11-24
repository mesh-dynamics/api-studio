package com.cubeio.logcollector.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.logger.LogStoreDTO;
import io.md.utils.CubeObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.Optional;

@Component
public class TextConverter {

    private static Logger LOGGER = LoggerFactory.getLogger(TextConverter.class);

    private ObjectMapper mapper = CubeObjectMapperProvider.getInstance();

    public Optional<LogStoreDTO> toLogStore(TextMessage message) {
        try{
            return Optional.ofNullable(mapper.readValue(message.getPayload() , LogStoreDTO.class)) ;
        }catch (Exception e){
            LOGGER.error("Text Message Parsing Failed ", e);
            return Optional.empty();
        }
    }
}
