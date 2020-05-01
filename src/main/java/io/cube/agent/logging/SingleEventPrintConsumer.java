package io.cube.agent.logging;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;

import io.cube.agent.CommonConfig;
import io.cube.agent.Utils;
import io.md.constants.Constants;
import io.md.utils.CubeObjectMapperProvider;

public class SingleEventPrintConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SingleEventPrintConsumer.class);


	private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	private OutputStream eventOutputStream;

	public SingleEventPrintConsumer() {
		System.out.println("SingleEventPrintConsumer Constructor Called");
		if (CommonConfig.getInstance().disruptorOutputLocation.equals("stdout")) {
			eventOutputStream = System.out;
		} else {
			try {
				eventOutputStream = new
					FileOutputStream(CommonConfig.getInstance().disruptorFileOutName);
			} catch (FileNotFoundException e) {
				eventOutputStream = Utils.nullOutputStream(LOGGER);
				LOGGER.debug("Unable to find outstream file. Setting outstream as nullOutputStream ", e);
			}
		}
	}

	public EventHandler<ValueEvent> getEventHandler() {
		return (event, sequence, endOfBatch)
			-> {
			jsonMapper.writeValue(eventOutputStream, event);
			eventOutputStream.write('\n');
		};
	}

}
