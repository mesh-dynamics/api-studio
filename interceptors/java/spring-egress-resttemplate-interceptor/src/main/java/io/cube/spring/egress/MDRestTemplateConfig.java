package io.cube.spring.egress;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;

@Component
public class MDRestTemplateConfig {

	public IntentResolver intentResolver = new TraceIntentResolver();
	
	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	//Needed to initialize, so that the  Recorder (ConsoleRecorder/ProxyBatchRecorder) is also init,
	//and the disruptor is already ready
	private final CommonConfig configInstance = CommonConfig.getInstance();

}
