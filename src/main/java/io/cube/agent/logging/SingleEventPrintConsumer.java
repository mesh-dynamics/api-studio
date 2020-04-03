package io.cube.agent.logging;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;

import io.md.utils.CubeObjectMapperProvider;

public class SingleEventPrintConsumer {

	private ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	public EventHandler<ValueEvent>[] getEventHandler() {
		EventHandler<ValueEvent> eventHandler
			= (event, sequence, endOfBatch)
			-> print(event, sequence);
		return new EventHandler[] { eventHandler };
	}

	private void print(ValueEvent valueEvent, long sequenceId) {
		try {
			jsonMapper.writeValue(System.out, valueEvent);
			//System.out.println("[Cube Event] " + jsonMapper.writeValueAsString(id));
		} catch (IOException e) {
			System.out.println("[Cube Event] " + "Exception while converting event to json "
				+ e.getMessage());
		}
	}

}
