package com.cubeui.backend.config;

import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 This support for http2 connector is for running cubeui-backend locally using the Intellij ID.
 Deployment docker image (build with tomcat) already has updated server.xml with http2 support.
 */
@Configuration
public class Http2SupportConfig {

	@Bean
	public ConfigurableServletWebServerFactory tomcatCustomizer() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addConnectorCustomizers(connector -> {
			connector.addUpgradeProtocol(new Http2Protocol());
		});


		return factory;
	}
}

