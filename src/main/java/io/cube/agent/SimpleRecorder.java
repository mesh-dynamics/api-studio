package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.json.JSONArray;
import org.json.JSONObject;


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

    public SimpleRecorder(Gson gson) {
        this.jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.cubeClient = new CubeClient(jsonMapper);
        this.gson = gson;
    }

    @Override
    public boolean record(FnKey fnKey, Optional<String> traceId,
                          Optional<String> spanId,
                          Optional<String> parentSpanId,
                          Object responseOrException,
                          FnReqResponse.RetStatus retStatus,
                          Optional<String> exceptionType,
                          Object... args) {
        try {
            JsonObject payload = createPayload(responseOrException, args);
            Optional<Event> event = createEvent(fnKey, traceId, payload);
            cubeClient.storeEvent(event);
            return true;
        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in recording function, skipping:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return false;
        }
    }

    private JsonObject createPayload(Object responseOrException, Object... args) {
        JsonObject payloadObj = new JsonObject();
        payloadObj.add("args", createArgsJsonArray(args));
        payloadObj.addProperty("response", gson.toJson(responseOrException));
        LOGGER.info("Function Payload : " + payloadObj.toString());
        return payloadObj;
    }

    private Optional<Event> createEvent(FnKey fnKey, Optional<String> traceId, JsonObject payload) {
        return Event.createEvent("NA", Optional.of(fnKey.customerId), Optional.of(fnKey.app),
                        Optional.of(fnKey.service), Optional.of(fnKey.instanceId), Optional.of(fnKey.service), traceId, Optional.of(Instant.now()),
                        Optional.of("NA"), Optional.of(fnKey.signature), Optional.of("JavaRequest"), Optional.empty(), Optional.of(payload.toString()));
    }

    private JsonArray createArgsJsonArray(Object... argVals) {
        JsonArray argsArray = new JsonArray();
        Arrays.stream(argVals).forEach(arg -> argsArray.add(gson.toJson(arg)));
        return argsArray;
    }
}
