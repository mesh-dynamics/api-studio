package io.cube.jaxrs.egress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.CommonConfig;
import io.cube.agent.ConsoleRecorder;
import io.cube.agent.IntentResolver;
import io.cube.agent.Recorder;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final Recorder recorder;

	public static CommonConfig commonConfig = null;

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	static {
		try {
			commonConfig = CommonConfig.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Config() {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
			.create();
		recorder = new ConsoleRecorder(gson);
	}
}
