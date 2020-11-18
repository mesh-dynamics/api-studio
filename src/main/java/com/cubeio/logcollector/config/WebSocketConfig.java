package com.cubeio.logcollector.config;

import com.cubeio.logcollector.controller.LoggingWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {

		WebSocketHandlerRegistration reg =  webSocketHandlerRegistry.addHandler(loggingWSHandler() , "/api/logStore");

		reg.setAllowedOrigins("*");
	}

	@Bean
	public WebSocketHandler loggingWSHandler() {
		return new LoggingWebSocketHandler();
	}
}

