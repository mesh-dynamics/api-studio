package io.cube.agent.samplers;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"type",
	"accuracy",
	"rate",
	"fieldCategory",
	"attributes"
})
public class SamplerConfig {

	@JsonProperty("type")
	private String type;
	@JsonProperty("accuracy")
	private Optional<Integer> accuracy;
	//Upper level sampling rate for samplers
	//like Simple/Boundary/Counting as they all have
	//single sampling rate.
	@JsonProperty("rate")
	private Optional<Float> rate;
	//This field will identify which request/response
	//parameter will be used in sampling [headers/queryParams/apiPath].
	@JsonProperty("fieldCategory")
	private Optional<String> fieldCategory;
	@JsonProperty("attributes")
	private Optional<List<Attributes>> attributes = null;

	@JsonProperty("type")
	public String getType() {
		return type;
	}

	@JsonProperty("type")
	public void setType(String type) {
		this.type = type;
	}

	@JsonProperty("accuracy")
	public Optional<Integer> getAccuracy() {
		return accuracy;
	}

	@JsonProperty("accuracy")
	public void setAccuracy(Integer accuracy) {
		this.accuracy = Optional.of(accuracy);
	}

	@JsonProperty("rate")
	public Optional<Float> getRate() {
		return rate;
	}

	@JsonProperty("rate")
	public void setRate(Float rate) {
		this.rate = Optional.of(rate);
	}

	@JsonProperty("fieldCategory")
	public Optional<String> getFieldCategory() {
		return fieldCategory;
	}

	@JsonProperty("fieldCategory")
	public void setFieldCategory(String fieldCategory) {
		this.fieldCategory = Optional.of(fieldCategory);
	}

	@JsonProperty("attributes")
	public Optional<List<Attributes>> getAttributes() {
		return attributes;
	}

	@JsonProperty("attributes")
	public void setAttributes(List<Attributes> attributes) {
		this.attributes = Optional.of(attributes);
	}
}
