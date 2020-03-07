package io.cube.agent.samplers;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"samplerType",
	"samplerAccuracy",
	"samplerID",
	"samplerAttributes"
})
public class SamplerConfig {

	@JsonProperty("samplerType")
	private String samplerType;
	@JsonProperty("samplerAccuracy")
	private int samplerAccuracy;
	@JsonProperty("samplingID")
	private String samplingID;
	@JsonProperty("samplerAttributes")
	private List<SamplerAttributes> samplerAttributes = null;

	@JsonProperty("samplerType")
	public String getSamplerType() {
		return samplerType;
	}

	@JsonProperty("samplerType")
	public void setSamplerType(String samplerType) {
		this.samplerType = samplerType;
	}

	@JsonProperty("samplerAccuracy")
	public int getSamplerAccuracy() {
		return samplerAccuracy;
	}

	@JsonProperty("samplerAccuracy")
	public void setSamplerAccuracy(int samplerAccuracy) {
		this.samplerAccuracy = samplerAccuracy;
	}

	@JsonProperty("samplingID")
	public String getSamplingID() {
		return samplingID;
	}

	@JsonProperty("samplingID")
	public void setSamplingID(String samplingID) {
		this.samplingID = samplingID;
	}

	@JsonProperty("samplerAttributes")
	public List<SamplerAttributes> getSamplerAttributes() {
		return samplerAttributes;
	}

	@JsonProperty("samplerAttributes")
	public void setSamplerAttributes(List<SamplerAttributes> samplerAttributes) {
		this.samplerAttributes = samplerAttributes;
	}

}
