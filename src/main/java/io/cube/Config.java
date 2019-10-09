package io.cube;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleHttpRecorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.TraceIntentResolver;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

// TODO make this config file singleton and inject it
public class Config {

    public IntentResolver intentResolver = new TraceIntentResolver();

    public final Recorder recorder;
    public final Mocker mocker;

    public static final CommonConfig commonConfig = new CommonConfig();

    public Config() {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .create();
        mocker = new SimpleMocker(gson);
        recorder = new SimpleHttpRecorder(gson);
    }
}
