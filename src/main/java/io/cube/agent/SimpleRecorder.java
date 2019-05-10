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
public class SimpleRecorder implements Recorder {

    private static final Logger LOGGER = LogManager.getLogger(SimpleRecorder.class);
    private static final String cubeRecordServiceUrl = "";

    private CubeClient cubeClient;
    private ObjectMapper jsonMapper;

    public SimpleRecorder() {
        this.jsonMapper = new ObjectMapper();
        this.cubeClient = new CubeClient(jsonMapper);
    }

    @Override
    public boolean record(FnKey fnKey, Optional<String> traceId,
                          Optional<String> spanId,
                          Optional<String> parentSpanId,
                          Object response,
                          Object... args) {


        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(jsonMapper::writeValueAsString)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            String respVal = jsonMapper.writeValueAsString(response);

            FnReqResponse fnrr = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId, fnKey.service,
                    fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    Optional.ofNullable(Instant.now()), argsHash,
                    argVals, respVal);

            Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnrr);

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in recording function, skipping:: " + fnKey.signature + " " + e.getMessage() + " " + stackTraceError);
            return false;
        }

        return true;
    }
}
