package com.cube.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lmax.disruptor.EventFactory;

import io.md.dao.MDStorable;

public class DisruptorValue {

	@JsonProperty("CubeEvent")
	private MDStorable value;

	@JsonIgnore
	public static EventFactory<DisruptorValue> getEventFactory() {
		return DisruptorValue::new;
	}

	public MDStorable getValue() {
		return value;
	}

	public void setValue(MDStorable event) {this.value = event;}

}
