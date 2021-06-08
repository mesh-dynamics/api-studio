package io.cube.agent.samplers;


import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"field",
	"value",
	"rate"
})
public class Attributes {

	@JsonProperty("field")
	private String field;
	@JsonProperty("value")
	private Optional<String> value;
	@JsonProperty("rate")
	private Optional<Float> rate;

	@JsonProperty("field")
	public String getField() {
		return field;
	}

	@JsonProperty("field")
	public void setField(String field) {
		this.field = field;
	}

	@JsonProperty("value")
	public Optional<String> getValue() {
		return value;
	}

	@JsonProperty("value")
	public void setValue(String value) {
		this.value = Optional.of(value);
	}

	@JsonProperty("rate")
	public Optional<Float> getRate() {
		return rate;
	}

	@JsonProperty("rate")
	public void setRate(Float rate) {
		this.rate = Optional.of(rate);
	}
}
