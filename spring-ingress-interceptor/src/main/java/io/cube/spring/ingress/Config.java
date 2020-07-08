package io.cube.spring.ingress;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.Recorder;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;

@Component
public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	//dummy call to instantiate RingBuffer to not miss the initial events
	private final CommonConfig commonConfig = CommonConfig.getInstance();
}
