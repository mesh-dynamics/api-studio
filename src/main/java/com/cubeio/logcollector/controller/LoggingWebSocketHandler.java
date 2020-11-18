package com.cubeio.logcollector.controller;

import com.cubeio.logcollector.data.BinaryMsgPackConverter;
import com.cubeio.logcollector.data.TextConverter;
import com.cubeio.logcollector.domain.DTO.LogStoreDTO;
import com.cubeio.logcollector.utils.LogUtils;
import org.apache.logging.log4j.message.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.Optional;

@Component
public class LoggingWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingWebSocketHandler.class);

    private final TextConverter textConverter = new TextConverter();
    private final BinaryMsgPackConverter msgPackConverter = new BinaryMsgPackConverter();

    private void print(String ctx , Map map){

        System.out.println(ctx);
        map.forEach((key , val)->{
            System.out.println(key + " "+val);
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {

        LOGGER.info("Connection established {} remote {}" , webSocketSession.getId(), webSocketSession.getRemoteAddress() );
        //print( "headers" , webSocketSession.getHandshakeHeaders());

    }

    @Override
    public void handleTextMessage(WebSocketSession webSocketSession, TextMessage message) throws Exception {

        Optional<LogStoreDTO> logDTO = textConverter.toLogStore(message);
        logDTO.ifPresentOrElse(dto->{this.logDTO(dto, webSocketSession);} , ()->{
            this.logString(message.getPayload() , webSocketSession);
        });
    }

    @Override
    public void handleBinaryMessage(WebSocketSession webSocketSession, BinaryMessage message) throws Exception {

        Optional<LogStoreDTO> logDTO = msgPackConverter.toLogStore(message);

        logDTO.ifPresentOrElse( dto->{this.logDTO(dto, webSocketSession);} , ()->{
            this.logString(new String(message.getPayload().array()) , webSocketSession);
        });
    }

    private void logDTO(LogStoreDTO dto , WebSocketSession session){

        Map<String , Object> meta = Map.of("sessionId" , session.getId() , "remote" , session.getRemoteAddress().toString());
        LogUtils.log(LOGGER , dto , Optional.of(meta));
    }

    private void logString(String message , WebSocketSession session ) {
        //LOGGER.error(String.format("logMessage:%s sessionId:%s" , message , session.getId()));
        LOGGER.error("logMessage:{} sessionId:{}" , message , session.getId());
    }


    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {

        LOGGER.error("TransportError "+webSocketSession.getId() ,  throwable);

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {

        //LOGGER.warn( String.format("Connection closed %s %s" , ));
        LOGGER.warn("Connection closed {} {}" , webSocketSession.getId() , closeStatus.toString());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}



