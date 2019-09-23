package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

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
import org.apache.logging.log4j.message.ObjectMessage;

public abstract class AbstractGsonSerializeRecorder implements Recorder {

    protected final Logger LOGGER = LogManager.getLogger(this.getClass());

    protected ObjectMapper jsonMapper;
    private Gson gson;

    public AbstractGsonSerializeRecorder(Gson gson) {
        // TODO pass this from above too
        this.jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.gson = gson;
    }

    public abstract boolean record(FnReqResponse fnReqResponse);
    public abstract boolean record(Event event);

    @Override
    public boolean recordOld(FnKey fnKey, Optional<String> traceId,
                          Optional<String> spanId,
                          Optional<String> parentSpanId,
                          Object responseOrException,
                          FnReqResponse.RetStatus retStatus,
                          Optional<String> exceptionType,
                          Object... args) {
        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(gson::toJson)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            //String respVal = jsonMapper.writeValueAsString(responseOrException);
            String respVal = gson.toJson(responseOrException);

            String traceIdString = traceId.orElse("N/A");
            var counter = new Object(){int x = 0;};
            Arrays.stream(argVals).forEach(arg -> LOGGER.info(new ObjectMessage(Map.of("func_name" , fnKey.fnName
                    , "trace_id" , traceIdString  , "arg_hash" , argsHash[counter.x] , "arg_val_" + counter.x ++ , arg))));

            LOGGER.info(new ObjectMessage(Map.of("return_value" , respVal)));

            FnReqResponse fnrr = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId, fnKey.service,
                    fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    Optional.ofNullable(Instant.now()), argsHash,
                    argVals, respVal, retStatus, exceptionType);
            return record(fnrr);
            //Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnrr);
            //cubeResponse.ifPresent(responseStr -> System.out.println(responseStr));
            //return true;
        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError = UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error(new ObjectMessage(Map.of("func_name", fnKey.fnName , "trace_id" , traceId.orElse("NA"))) , e);
            return false;
        }
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

            return event.map(ev -> record(ev)).orElseGet(() -> {
                LOGGER.error(new ObjectMessage(Map.of("func_name", fnKey.fnName , "trace_id" ,
                        traceId.orElse("NA"), "operation", "Record Event", "response", "Event is empty!")));
                return false;
            });

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError = UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error(new ObjectMessage(Map.of("func_name", fnKey.fnName , "trace_id" , traceId.orElse("NA"))) , e);
            return false;
        }
    }


    private JsonObject createPayload(Object responseOrException, Object... args) {
        JsonObject payloadObj = new JsonObject();
        payloadObj.add("args", createArgsJsonArray(args));
        payloadObj.addProperty("response", gson.toJson(responseOrException));
        LOGGER.info(new ObjectMessage(Map.of("function_payload", payloadObj.toString())));
        return payloadObj;
    }

    private Optional<Event> createEvent(FnKey fnKey, Optional<String> traceId, JsonObject payload) {
        return Event.createEvent("NA", Optional.of(fnKey.customerId), Optional.of(fnKey.app),
                        Optional.of(fnKey.service), Optional.of(fnKey.instanceId), Optional.of("NA"), traceId, Optional.of(Instant.now()),
                        Optional.of("NA"), Optional.of(fnKey.signature), Optional.of("JavaRequest"), Optional.empty(), Optional.of(payload.toString()));
    }

    private JsonArray createArgsJsonArray(Object... argVals) {
        JsonArray argsArray = new JsonArray();
        Arrays.stream(argVals).forEach(arg -> argsArray.add(gson.toJson(arg)));
        return argsArray;
    }
}
