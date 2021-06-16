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

package com.cube.pubsub;

import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.md.cache.Constants.PubSubContext;

public class ChannelMsg {

	public PubSubContext  context;

	@JsonInclude(Include.NON_ABSENT)
	public Map data = Collections.EMPTY_MAP;
	public ChannelMsg(){}

	public ChannelMsg(PubSubContext ctx , Map obj ){
		this.context = ctx;
		this.data = obj;
	}

}
