package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;

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

    @Override
    public boolean record(FnKey fnKey, Optional<String> traceId,
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
            LOGGER.info("Trying to record function :: " + fnKey.function.getName());
            Arrays.stream(argVals).forEach(arg -> LOGGER.info("Argument while storing :: " + arg));
            LOGGER.info("Function return value serialized :: " + respVal);
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
            LOGGER.error("Error in recording function, skipping:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return false;
        }
    }

}
