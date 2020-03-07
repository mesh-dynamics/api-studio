package io.cube.agent.samplers;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"samplingField",
	"samplingValue",
	"samplingRate"
})
public class SamplerAttributes {

	@JsonProperty("samplingField")
	private String samplingField;
	@JsonProperty("samplingValue")
	private String samplingValue;
	@JsonProperty("samplingRate")
	private float samplingRate;

	@JsonProperty("samplingField")
	public String getSamplingField() {
		return samplingField;
	}

	@JsonProperty("samplingField")
	public void setSamplingField(String samplingField) {
		this.samplingField = samplingField;
	}

	@JsonProperty("samplingValue")
	public String getSamplingValue() {
		return samplingValue;
	}

	@JsonProperty("samplingValue")
	public void setSamplingValue(String samplingValue) {
		this.samplingValue = samplingValue;
	}

	@JsonProperty("samplingRate")
	public float getSamplingRate() {
		return samplingRate;
	}

	@JsonProperty("samplingRate")
	public void setSamplingRate(float samplingRate) {
		this.samplingRate = samplingRate;
	}
}
