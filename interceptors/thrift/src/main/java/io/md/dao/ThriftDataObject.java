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

package io.md.dao;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.md.constants.Constants;
import io.md.utils.Utils;

public class ThriftDataObject extends JsonDataObj implements DataObj  {


	public final String traceId;

	public static class ThriftDataObjectBuilder {
		public ThriftDataObject build(byte[] payloadBin, Map<String, Object> params)  {
			try {
				TDeserializer tDeserializer = new TDeserializer();
				ClassLoader loader = (URLClassLoader) params.get(Constants.CLASS_LOADER);
				Class<?> clazz = loader
					.loadClass((String) params.get(Constants.THRIFT_CLASS_NAME));
				Constructor<?> constructor = clazz.getConstructor();
				Object obj1 = constructor.newInstance();
				tDeserializer.deserialize((TBase)obj1, payloadBin);
				Gson gson = (Gson) params.get(Constants.GSON_OBJECT);
				if (gson == null) throw new Exception("gson Object is null");
				ObjectMapper jsonMapper = (ObjectMapper) params.get(Constants.OBJECT_MAPPER);
				if (jsonMapper == null) throw new Exception("object mapper is null");
				String jsonSerialized = gson.toJson(obj1);
				String traceId = Utils.traceIdFromThriftSpan((TBase)obj1);
				return new ThriftDataObject(jsonSerialized, jsonMapper, traceId);
			} catch (Exception e) {
				throw new DataObjCreationException(e);
			}
		}
	}

	private ThriftDataObject(String jsonSerialized, ObjectMapper jsonMapper, String traceId) {
		super(jsonSerialized, jsonMapper);
		this.traceId = traceId;
	}
}

