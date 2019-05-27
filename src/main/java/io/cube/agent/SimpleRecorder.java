package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleRecorder implements Recorder {

    private static final Logger LOGGER = LogManager.getLogger(SimpleRecorder.class);
    private static final String cubeRecordServiceUrl = "";

    private CubeClient cubeClient;
    private ObjectMapper jsonMapper;
    private Gson gson;

    public SimpleRecorder() {
        this.jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.cubeClient = new CubeClient(jsonMapper);
        gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();
    }

    @Override
    public boolean record(FnKey fnKey, Optional<String> traceId,
                          Optional<String> spanId,
                          Optional<String> parentSpanId,
                          Object response,
                          Object... args) {
        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(gson::toJson)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            //String respVal = jsonMapper.writeValueAsString(response);
            String respVal = gson.toJson(response);
            LOGGER.info("Trying to record function :: " + fnKey.function.getName());
            Arrays.stream(argVals).forEach(arg -> LOGGER.info("Argument while storing :: " + arg));
            LOGGER.info("Function return value serialized :: " + respVal);
            FnReqResponse fnrr = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId, fnKey.service,
                    fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    Optional.ofNullable(Instant.now()), argsHash,
                    argVals, respVal);

            Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnrr);
            //cubeResponse.ifPresent(responseStr -> System.out.println(responseStr));
            return true;
        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in recording function, skipping:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return false;
        }
    }
}
