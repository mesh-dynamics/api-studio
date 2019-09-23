package io.cube.agent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.cube.agent.FnReqResponse.RetStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.cube.agent.UtilException.rethrowFunction;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleMocker implements Mocker {

    private static final Logger LOGGER = LogManager.getLogger(SimpleMocker.class);
    private static Map<Integer,Instant> fnMap = new ConcurrentHashMap<>();
    private ObjectMapper jsonMapper;
    private Gson gson;
    CubeClient cubeClient;

    public SimpleMocker(Gson gson) {
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        cubeClient = new CubeClient(jsonMapper);
        this.gson = gson;
    }


    @Override
    public FnResponseObj mock(FnKey fnKey, Optional<String> traceId, Optional<String> spanId, Optional<String> parentSpanId,
                              Optional<Instant> prevRespTS, Optional<Type> retType, Object... args) {

        try {
            String[] argVals =
                    Arrays.stream(args).map(rethrowFunction(gson::toJson)).toArray(String[]::new);
            Integer[] argsHash = Arrays.stream(argVals).map(String::hashCode).toArray(Integer[]::new);
            String traceIdString = traceId.orElse("N/A");
            LOGGER.info(new ObjectMessage(Map.of("state" , "Before Mock" , "func_name" , fnKey.fnName , "trace_id" , traceIdString)));
            var counter = new Object(){int x = 0;};
            Arrays.stream(argVals).forEach(arg -> LOGGER.info(new ObjectMessage(Map.of("func_name" , fnKey.fnName
                    , "trace_id" , traceIdString  , "arg_hash" , argsHash[counter.x] , "arg_val_" + counter.x ++ , arg))));

            //This key is to identify cases where multiple Solr docs are matched
            Integer key = traceId.orElse("").concat(spanId.orElse(""))
                    .concat(parentSpanId.orElse(""))
                    .concat(fnKey.signature).hashCode();

            // forming a dummy req response object with empty ret value, we can just serialize this object
            // and send it to cube mock service to get appropriate response
            FnReqResponse fnReqResponse = new FnReqResponse(fnKey.customerId, fnKey.app, fnKey.instanceId,
                    fnKey.service, fnKey.fnSigatureHash, fnKey.fnName, traceId, spanId, parentSpanId,
                    Optional.ofNullable(prevRespTS.orElse(fnMap.get(key))), argsHash, argVals, "",
                    RetStatus.Success, Optional.empty());

            Optional<FnResponse> ret = cubeClient.getMockResponse(fnReqResponse);

            // need to check is before trying to convert return value, otherwise null return value also leads to
            // empty optional
            if (ret.isEmpty()) {
                LOGGER.error(new ObjectMessage(Map.of("reason" , "No Matching Response Received"
                        , "trace_id" , traceIdString , "func_name" , fnKey.fnName)));
                return new FnResponseObj(null, Optional.empty(), RetStatus.Success, Optional.empty());
            } else {
                FnResponse response = ret.get();
                LOGGER.info(new ObjectMessage(Map.of("state" , "After Mock" , "func_name" , fnKey.fnName , "trace_id" , traceIdString,
                         "return_val" , response.retVal)));
                //If multiple Solr docs were returned, we need to maintain the last timestamp
                //to be used in the next mock call.
                if (response.retStatus == RetStatus.Success) {
                    if (response.multipleResults) {
                        fnMap.put(key, response.timeStamp.get());
                    } else {
                        fnMap.remove(key); //remove if the key is present.
                    }
                } else {
                    fnMap.remove(key);
                }

                Object retOrExceptionVal = gson.fromJson(response.retVal, retType.isPresent()?retType.get():getRetOrExceptionClass(response,
                        fnKey.function.getGenericReturnType()));
                return new FnResponseObj(retOrExceptionVal, response.timeStamp, response.retStatus, response.exceptionType);
            }

        } catch (Throwable e) {
            // encode can throw UnsupportedEncodingException
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error(new ObjectMessage(Map.of("func_name", fnKey.fnName , "trace_id" , traceId)) , e);
            return new FnResponseObj(null, Optional.empty(), RetStatus.Success, Optional.empty());
        }
    }

    private Type getRetOrExceptionClass(FnResponse response, Type returnType) throws Exception {
        if (response.retStatus == RetStatus.Success) {
            return returnType;
        } else {
            return response.exceptionType.map(rethrowFunction(Class::forName)).map(TypeToken::get).map(TypeToken::getType).orElseThrow(()-> new Exception(
                    "Exception class not specified"));
        }
    }
}
