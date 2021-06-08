package io.cube.agent;

import java.lang.reflect.Constructor;
import java.util.Optional;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.dao.Event;
import io.md.dao.JsonByteArrayPayload;

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
		// TODO: change 2nd param below to lowerBound to support multiple matches for same function call
		Event event = cubeClient.getMockResponseEvent(thriftRequestEvent, Optional.empty())
				.flatMap(mr -> mr.response).orElseThrow(() ->
			new RuntimeException("Unable to get a response from the mock server"));
		String className = extractThriftClass(event.apiPath);
		TDeserializer tDeserializer = new TDeserializer();
		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor();
		Object obj1 = constructor.newInstance();
		tDeserializer.deserialize((TBase) obj1, ((JsonByteArrayPayload)event.payload).jsonBinary);
		return (TBase) obj1;
	}

}
