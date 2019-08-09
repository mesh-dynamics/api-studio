package io.cube;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.cube.agent.CommonConfig;
import io.cube.agent.CommonUtils;
import io.cube.agent.IntentResolver;
import io.cube.agent.Mocker;
import io.cube.agent.Recorder;
import io.cube.agent.SimpleMocker;
import io.cube.agent.SimpleRecorder;
import io.cube.agent.TraceIntentResolver;
import io.opentracing.Scope;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Properties;

// TODO make this config file singleton and inject it
public class Config {

    private static final String CONFFILE = "jdbc.conf";

    final Properties properties;

    private static final Logger LOGGER = LogManager.getLogger(Config.class);

    public IntentResolver intentResolver = new TraceIntentResolver();

    public final Recorder recorder;
    public final Mocker mocker;

    public final CommonConfig commonConfig = new CommonConfig();

    public Config() {
//        if (!GlobalTracer.isRegistered()) {
//            Tracer tracer = Utils.init("tracer");
//            try {
//                GlobalTracer.register(tracer);
//            } catch (IllegalStateException e) {
//                LOGGER.error("Trying to register a tracer when one is already registered");
//            }
//        }
//
//        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
//        Scope scope = Utils.startServerSpan(headers, "jdbcTest");

        properties = new Properties();
        try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
        } catch (Exception e) {
            LOGGER.error("Error while initializing config :: " + e.getMessage());
        }

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .create();
        mocker = new SimpleMocker(gson);
        recorder = new SimpleRecorder(gson);
    }

}
