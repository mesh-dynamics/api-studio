package io.cube.agent;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

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
    public Object mock(String traceid, Method function, Object... args) {

        String signature = Utils.getFunctionSignature(function);

        int fnHash = Objects.hash(signature);

        ObjectMapper jsonMapper = new ObjectMapper();

        try {
            String[] argVals =
                    Arrays.stream(args).map(UtilException.rethrowFunction(jsonMapper::writeValueAsString)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);


            String respVal = "";
            // TODO: call cube api to get response, passing traceid, fnSignatureHash and argsHash

            return jsonMapper.readValue(respVal, function.getReturnType());

        } catch (Exception e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in mocking function, returning null:: " + signature + " " + e.getMessage() + " " + stackTraceError);
            return null;
        }
    }
}
