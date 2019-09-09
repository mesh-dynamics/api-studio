package io.cube;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.SimpleRecorder;
import io.cube.agent.TraceIntentResolver;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO make this config file singleton and inject it
public class Config {

    private static final Logger LOGGER = LogManager.getLogger(Config.class);
    private static final String CONFFILE = "jdbc.conf";

    public IntentResolver intentResolver = new TraceIntentResolver();

    public final Recorder recorder;
    public final Mocker mocker;

    public static final CommonConfig commonConfig = new CommonConfig();

    public Config() {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .create();
        mocker = new SimpleMocker(gson);
        recorder = new SimpleRecorder(gson);
    }
}
