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

package com.cube.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ThriftDataObject extends io.md.dao.JsonDataObj {

	public ThriftDataObject(JsonNode root,
		ObjectMapper jsonMapper) {
		super(root, jsonMapper);
	}


	/*public final String traceId;

	public static class ThriftDataObjectBuilder {
		public ThriftDataObject build(byte[] payloadBin, Config config, Map<String, Object> params)  {
			try {
				TDeserializer tDeserializer = new TDeserializer();
				ClassLoader loader = (URLClassLoader) params.get(Constants.CLASS_LOADER);
				Class<?> clazz = loader
					.loadClass((String) params.get(Constants.THRIFT_CLASS_NAME));
				Constructor<?> constructor = clazz.getConstructor();
				Object obj1 = constructor.newInstance();
				tDeserializer.deserialize((TBase)obj1, payloadBin);
				String jsonSerialized = config.gson.toJson(obj1);
				// TODO this will come from md commons thrift
				//String traceId = CommonUtils.traceIdFromThriftSpan((TBase)obj1);
				return new ThriftDataObject(jsonSerialized, config, *//*traceId*//*null);
			} catch (Exception e) {
				throw new DataObjectCreationException(e);
			}
		}
	}

	private ThriftDataObject(String jsonSerialized, Config config, String traceId) {
		super(jsonSerialized, config.jsonMapper);
		this.traceId = traceId;
	}*/
}
