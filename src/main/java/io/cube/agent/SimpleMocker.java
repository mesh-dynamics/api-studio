package io.cube.agent;

import static io.cube.agent.UtilException.rethrowFunction;

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

import io.cube.agent.FnReqResponse.RetStatus;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleMocker implements Mocker {

    private static final Logger LOGGER = LogManager.getLogger(SimpleMocker.class);

    private ObjectMapper jsonMapper;
    private Gson gson;
    CubeClient cubeClient;

    public SimpleMocker() {
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        cubeClient = new CubeClient(jsonMapper);
        gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();
    }


    @Override
    public FnResponseObj mock(FnKey fnKey, Optional<String> traceId, Optional<String> spanId, Optional<String> parentSpanId,
                              Optional<Instant> prevRespTS, Object... args) {

        try {
            String[] argVals =
                    Arrays.stream(args).map(rethrowFunction(gson::toJson)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            LOGGER.info("Trying to mock function :: " + fnKey.function.getName());
            Arrays.stream(argVals).forEach(arg -> LOGGER.info("Argument while mocking :: " + arg));
            // forming a dummy req response object with empty ret value, we can just serialize this object
            // and send it to cube mock service to get appropriate response
            FnReqResponse fnReqResponse = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId,
                    fnKey.service, fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    prevRespTS, argsHash, argVals, "", RetStatus.Success, Optional.empty());

            Optional<FnResponse> ret = cubeClient.getMockResponse(fnReqResponse);

            // need to check is before trying to convert return value, otherwise null return value also leads to
            // empty optional
            if (ret.isEmpty()) {
                LOGGER.error("Error in mocking function, no matching response received, returning null");
                return new FnResponseObj(null, Optional.empty(), RetStatus.Success, Optional.empty());
            } else {
                FnResponse response = ret.get();
                LOGGER.info("Return value received while mocking :: " + response.retVal);
                Object retOrExceptionVal = gson.fromJson(response.retVal, getRetOrExceptionClass(response,
                        fnKey.function.getReturnType()));
                return new FnResponseObj(retOrExceptionVal, response.timeStamp, response.retStatus, response.exceptionType);
            }

        } catch (Throwable e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Error in mocking function, returning " + null + " :: " + fnKey.signature + " "
                    + e.getMessage() + " " + stackTraceError);
            return new FnResponseObj(null, Optional.empty(), RetStatus.Success, Optional.empty());
        }
    }


    private Class getRetOrExceptionClass(FnResponse response, Class returnType) throws Exception {
        if (response.retStatus == RetStatus.Success) {
            return returnType;
        } else {
            return response.exceptionType.map(rethrowFunction(Class::forName)).orElseThrow(()-> new Exception(
                    "Exception class not specified"));
        }
    }
}
