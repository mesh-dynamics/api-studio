package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
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

    private ObjectMapper jsonMapper;
    CubeClient cubeClient;

    public SimpleMocker() {
        jsonMapper = new ObjectMapper();
        CubeClient client = new CubeClient(jsonMapper);
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

            cubeClient.getMockResponse(fnReqResponse);

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
