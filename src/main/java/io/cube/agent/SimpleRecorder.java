package io.cube.agent;

import java.lang.reflect.Method;
import java.security.Signature;
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
public class SimpleRecorder implements Recorder {

    private static final Logger LOGGER = LogManager.getLogger(SimpleRecorder.class);


    @Override
    public boolean record(FnKey fnKey, Optional<Instant> prevRespTS, Object response, Object... args) {

        ObjectMapper jsonMapper = new ObjectMapper();

        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(jsonMapper::writeValueAsString)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            String respVal = jsonMapper.writeValueAsString(response);

            FnReqResponse fnrr = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId, fnKey.service,
                    fnKey.traceId, fnKey.fnSigatureHash, fnKey.fnName, prevRespTS, argsHash, argVals, respVal);

            //TODO: Call cube api to log the FnReqResponse

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in recording function, skipping:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return false;
        }

        return true;
    }
}
