package io.cube.agent.logging;


import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.clapper.util.io.RollingFileWriter;
import org.clapper.util.io.RollingFileWriter.Compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;

import io.cube.agent.CommonConfig;
import io.cube.agent.Utils;
import io.md.utils.CubeObjectMapperProvider;

public class SingleEventPrintConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SingleEventPrintConsumer.class);


	private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	private PrintWriter eventWriter;

	public SingleEventPrintConsumer() {
		System.out.println("SingleEventPrintConsumer Constructor Called");
		if (CommonConfig.getInstance().disruptorOutputLocation.equals("stdout")) {
			eventWriter = new PrintWriter(System.out);
		} else {
			try {
				eventWriter = new RollingFileWriter(
					CommonConfig.getInstance().disruptorFileOutName
						+ "-" + UUID.randomUUID().toString() + ".log${n}"
					, CommonConfig.getInstance().disruptorLogFileMaxSize
					, CommonConfig.getInstance().disruptorLogMaxBackup, Compression.DONT_COMPRESS_BACKUPS);
			} catch (Exception e) {
				eventWriter = Utils.nullPrintWriter(LOGGER);
				LOGGER.debug("Unable to find outstream file. Setting outstream as nullOutputStream ", e);
			}
		}
	}

	public EventHandler<ValueEvent> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			jsonMapper.writeValue(eventWriter, event);
			eventWriter.println();
		};
	}

}
