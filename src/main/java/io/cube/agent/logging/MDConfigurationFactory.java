package io.cube.agent.logging;

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

@Plugin(name = "MDConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class MDConfigurationFactory extends ConfigurationFactory {

	static Configuration createConfiguration(final String name,
		ConfigurationBuilder<BuiltConfiguration> builder) {
		builder.setConfigurationName(name);
		builder.setStatusLevel(Level.ERROR);
		builder
			.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL).
				addAttribute("level", Level.TRACE));
		AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").
			addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
			.addAttribute("direct" , "true");
		/**
		 *   <CustomJsonLayout complete="false" objectMessageAsJsonObject="true" eventEol="true"
		 *         propertiesAsList="false" compact="true" properties="false" includeStackTrace="true"
		 *         locationInfo="false"/>
		 */
		LayoutComponentBuilder layoutBuilder = builder.newLayout("CustomJsonLayout")
			.addAttribute("complete", "false")
			.addAttribute("objectMessageAsJsonObject", "true")
			.addAttribute("eventEol", "true")
			.addAttribute("propertiesAsList", "false")
			.addAttribute("compact", "true")
			.addAttribute("properties", "false")
			.addAttribute("includeStackTrace", "true")
			.addAttribute("locationInfo", "false");
		appenderBuilder.add(layoutBuilder);
		/*appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
			Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));*/
		builder.add(appenderBuilder);
		builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
			add(builder.newAppenderRef("Stdout")).
			addAttribute("additivity", false));
		builder.add(builder.newAsyncRootLogger(Level.INFO).add(builder.newAppenderRef("Stdout")));
		System.out.println("Create configuration called");
		return builder.build();
	}

	@Override
	public Configuration getConfiguration(final LoggerContext loggerContext,
		final ConfigurationSource source) {
		return getConfiguration(loggerContext, source.toString(), null);
	}

	@Override
	public Configuration getConfiguration(final LoggerContext loggerContext, final String name,
		final URI configLocation) {
		ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
		return createConfiguration(name, builder);
	}

	@Override
	protected String[] getSupportedTypes() {
		return new String[]{"*"};
	}
}