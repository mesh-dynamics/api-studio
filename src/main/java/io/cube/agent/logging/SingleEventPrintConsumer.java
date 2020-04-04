package io.cube.agent.logging;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;

import io.cube.agent.CommonConfig;
import io.md.utils.CubeObjectMapperProvider;

public class SingleEventPrintConsumer {

	private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	private OutputStream eventOutputStream;

	public SingleEventPrintConsumer() throws FileNotFoundException {
		System.out.println("Constructor Called");
		if (CommonConfig.getInstance().disruptorOutputLocation.equals("stdout")) {
			eventOutputStream = System.out;
		} else {
			eventOutputStream = new
				FileOutputStream(CommonConfig.getInstance().disruptorFileOutName);
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
