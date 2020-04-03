package io.cube.agent.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lmax.disruptor.EventFactory;

import io.md.dao.Event;

public class ValueEvent {

	@JsonProperty("CubeEvent")
	private Event value;

	@JsonIgnore
	public final static EventFactory EVENT_FACTORY = () -> new ValueEvent();

	public Event getValue() {
		return value;
	}

	public void setValue(Event event) {this.value = event;}

}
