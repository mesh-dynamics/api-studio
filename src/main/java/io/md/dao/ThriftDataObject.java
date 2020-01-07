package io.md.dao;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

public class ThriftDataObject extends JsonDataObj implements DataObj {


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
				String traceId = CommonUtils.traceIdFromThriftSpan((TBase)obj1);
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

