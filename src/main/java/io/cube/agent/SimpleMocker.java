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

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleMocker implements Mocker {

    private static final Logger LOGGER = LogManager.getLogger(SimpleMocker.class);

    private ObjectMapper jsonMapper;
    CubeClient cubeClient;

    public SimpleMocker() {
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        cubeClient = new CubeClient(jsonMapper);
    }


    @Override
    public Object mock(FnKey fnKey, Optional<String> traceId, Optional<String> spanId, Optional<String> parentSpanId,
                       Optional<Instant> prevRespTS, Object... args) {
        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(jsonMapper::writeValueAsString)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);

            // forming a dummy req response object with empty ret value, we can just serialize this object
            // and send it to cube mock service to get appropriate response
            FnReqResponse fnReqResponse = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId,
                    fnKey.service, fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    prevRespTS, argsHash, argVals, "");

            Optional<String> ret = cubeClient.getMockResponse(fnReqResponse);

            // need to check is before trying to convert return value, otherwise null return value also leads to
            // empty optional
            if (ret.isEmpty()) {
                LOGGER.error("Error in mocking function, no matching response received, returning null");
                return null;
            }

            return ret.map(UtilException.rethrowFunction(response ->
                            jsonMapper.readValue(response, fnKey.function.getReturnType()))).orElse(null);

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in mocking function, returning null:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return null;
        }
    }

}
