package io.cube.apachecxf.ingress;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;

public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final CommonConfig commonConfig = CommonConfig.getInstance();

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

}
