/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
