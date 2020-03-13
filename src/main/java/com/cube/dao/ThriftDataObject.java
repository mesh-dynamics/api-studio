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
