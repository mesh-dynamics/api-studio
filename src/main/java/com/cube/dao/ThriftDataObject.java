package com.cube.dao;

import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import io.cube.agent.CommonUtils;

import com.cube.utils.Constants;
import com.cube.ws.Config;

public class ThriftDataObject extends JsonDataObj implements DataObj  {


	public final String traceId;

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
				String traceId = CommonUtils.traceIdFromThriftSpan((TBase)obj1);
				return new ThriftDataObject(jsonSerialized, config, traceId);
			} catch (Exception e) {
				throw new DataObjCreationException(e);
			}
		}
	}

	private ThriftDataObject(String jsonSerialized, Config config, String traceId) {
		super(jsonSerialized, config.jsonMapper);
		this.traceId = traceId;
	}
}
