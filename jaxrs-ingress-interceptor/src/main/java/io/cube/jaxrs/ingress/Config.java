package io.cube.jaxrs.ingress;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.ConsoleRecorder;
import io.cube.agent.IntentResolver;
import io.cube.agent.Recorder;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;

public class Config {

	public IntentResolver intentResolver = new TraceIntentResolver();

	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();
}
