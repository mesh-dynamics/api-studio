package io.cube.agent;

import java.net.URI;


import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.spi.LoggerContextFactory;

public class MDLoggerContextFactory  implements LoggerContextFactory {

	private LoggerContext context;

	MDLoggerContextFactory() {
		this.context = new LoggerContext("MDLoggerContext");
	}

	@Override
	public LoggerContext getContext(String s, ClassLoader classLoader, Object o, boolean b) {
		return context;
	}

	@Override
	public LoggerContext getContext(String s, ClassLoader classLoader, Object o, boolean b, URI uri,
		String s1) {
		return context;
	}

	@Override
	public void removeContext(org.apache.logging.log4j.spi.LoggerContext loggerContext) {

	}


}
