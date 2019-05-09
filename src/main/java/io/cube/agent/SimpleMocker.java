package io.cube.agent;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleMocker implements Mocker {

    private static final Logger LOGGER = LogManager.getLogger(SimpleMocker.class);



    @Override
    public Object mock(FnKey fnKey, Optional<String> traceId, Optional<String> spanId, Optional<String> parentSpanId,
                       Optional<Instant> prevRespTS, Object... args) {


        ObjectMapper jsonMapper = new ObjectMapper();

        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(jsonMapper::writeValueAsString)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);


            String respVal = "";
            // TODO: call cube api to get response, passing fnkey fields, traceid, spanId, parentSpanId, prevRespTS and
            //  argsHash

            return jsonMapper.readValue(respVal, fnKey.function.getReturnType());

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in mocking function, returning null:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return null;
        }
    }

}
