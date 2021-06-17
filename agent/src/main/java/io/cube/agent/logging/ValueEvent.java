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

package io.cube.agent.logging;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lmax.disruptor.EventFactory;

import io.md.dao.Event;

public class ValueEvent {

	@JsonProperty("CubeEvent")
	private Event value;

	@JsonIgnore
	public final static EventFactory<ValueEvent> EVENT_FACTORY = ValueEvent::new;

	public Event getValue() {
		return value;
	}

	public void setValue(Event event) {this.value = event;}

}
