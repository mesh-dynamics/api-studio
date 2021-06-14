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

package io.md.core;

import java.util.HashMap;
import java.util.Map;

public class ConfigApplicationAcknowledge {

	public final String customerId;
	public final String app;
	public final String service;
	public final String instanceId;

	// Here you can send
	public final Map<String, String> acknowledgeInfo;

	public ConfigApplicationAcknowledge() {
		this.customerId = null;
		this.app = null;
		this.service = null;
		this.instanceId = null;
		acknowledgeInfo = new HashMap<>();
	}

	public ConfigApplicationAcknowledge(String customerId, String app, String service
		, String instanceId, Map<String, String> extraInfo) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.acknowledgeInfo = extraInfo;
	}


}
