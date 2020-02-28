package io.cube.agent;

import java.lang.reflect.Constructor;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.dao.Event;
import io.md.dao.StringAsByteArrayPayload;

public class ThriftMocker {

	private CubeClient cubeClient;
	private ObjectMapper jsonMapper;

	public ThriftMocker() throws Exception {
		jsonMapper = new ObjectMapper();
		jsonMapper.registerModule(new Jdk8Module());
		jsonMapper.registerModule(new JavaTimeModule());
		jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		cubeClient = new CubeClient(jsonMapper);
	}

	public static String extractThriftClass(String thriftApiPath) {
		String[] splitResult = thriftApiPath.split("::");
		return splitResult[1];
	}

	public TBase mockThriftRequest(Event thriftRequestEvent) throws Exception {
		Event event = cubeClient.getMockThriftResponse(thriftRequestEvent).orElseThrow(() ->
			new RuntimeException("Unable to get a response from the mock server"));
		String className = extractThriftClass(event.apiPath);
		TDeserializer tDeserializer = new TDeserializer();
		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor();
		Object obj1 = constructor.newInstance();
		tDeserializer.deserialize((TBase) obj1, ((StringAsByteArrayPayload)event.payload).payload);
		return (TBase) obj1;
	}

}
