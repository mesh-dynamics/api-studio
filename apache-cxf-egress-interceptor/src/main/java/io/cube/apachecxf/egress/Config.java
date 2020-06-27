package io.cube.apachecxf.egress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.CommonConfig;
import io.cube.agent.ConsoleRecorder;
import io.cube.agent.IntentResolver;
import io.cube.agent.ProxyBatchRecorder;
import io.cube.agent.Recorder;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	public final CommonConfig commonConfig = CommonConfig.getInstance();

}
