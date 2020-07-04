package io.cube.agent;

import static io.md.utils.UtilException.rethrowFunction;

import java.lang.reflect.Type;
import java.sql.Blob;
import java.sql.Clob;
import java.time.Instant;
import java.util.Optional;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.md.dao.Event;
import io.md.dao.FnReqRespPayload;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording;
import io.md.services.FnResponse;
import io.md.services.Mocker;
import io.md.utils.CommonUtils;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.FnKey;
import io.md.utils.MeshDGsonProvider;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 02/07/20
 */
public class FnMockerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FnMockerHelper.class);

    private final Mocker mocker;
    private final Gson gson;
    private final ObjectMapper jsonMapper;

    public FnMockerHelper(Mocker mocker) {
        this.mocker = mocker;
        this.gson = MeshDGsonProvider.getInstance();
        this.jsonMapper = CubeObjectMapperProvider.getInstance();
    }


    public FnResponseObj mock(FnKey fnKey,
                                     Optional<Instant> prevRespTS, Optional<Type> retType, Object... args) {
        MDTraceInfo mdTraceInfo;
        if (CommonUtils.getCurrentTraceId().isPresent()) {
            //load the created context
            mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

        } else {
            //No span context. Initialization scenario.
            mdTraceInfo = CommonUtils.getDefaultTraceInfo();
        }

        FnReqRespPayload fnReqRespPayload = new FnReqRespPayload(Optional.of(Instant.now()),
                args, null, null , null);
        Optional<Event> event = CommonUtils.createEvent(fnKey, mdTraceInfo, Event.RunType.Replay,
                Optional.empty(), fnReqRespPayload, Recording.RecordingType.Golden);

        try {
            return event.map(UtilException.rethrowFunction(eve -> {
                Optional<FnResponse> fnResponse = Utils.mockResponseToFnResponse(mocker.mock(eve, prevRespTS,
                        Optional.empty()));

                return fnResponse.map(resp -> {

                    Object retOrExceptionVal = null;
                    try {
                        Type returnType = retType.orElse(fnKey.function.getGenericReturnType());
                        if (returnType.getTypeName().equals(Clob.class.getTypeName())) {
                            retOrExceptionVal = new SerialClob(resp.retVal.toCharArray());
                        } else if (returnType.getTypeName().equals(Blob.class.getTypeName())) {
                            retOrExceptionVal = new SerialBlob(Base64.decodeBase64(resp.retVal));
                        } else {
                            retOrExceptionVal = gson.fromJson(resp.retVal,
                                    retType.isPresent() ? retType.get() : getRetOrExceptionClass(resp,
                                            returnType));
                        }
                    } catch (JsonSyntaxException ex) {
                        //If the returned value is a String with spaces, this exception
                        //is thrown, In that case we will return the same value
                        LOGGER.error(
                                "Json Syntax exception, could be a simple string, returning the original value");
                        return new FnResponseObj(resp.retVal, resp.timeStamp, resp.retStatus,
                                resp.exceptionType);
                    }
                    catch (Exception e) {
                        LOGGER.error("func_signature :".concat(eve.apiPath)
                                .concat(" , trace_id : ").concat(eve.getTraceId()), e);
                        return emptyFnResponseObj;
                    }

                    return new FnResponseObj(retOrExceptionVal, resp.timeStamp, resp.retStatus,
                            resp.exceptionType);
                }).orElseGet(() -> {
                    LOGGER.error(
                            "No Matching Response Received : trace_id : ".concat(eve.getTraceId())
                                    .concat(" , func_signature : ".concat(eve.apiPath)));
                    return emptyFnResponseObj;
                });
            })).orElseGet(() -> {
                LOGGER.error("Not able to form an event : trace_id :".concat(mdTraceInfo.traceId)
                        .concat(" , func_signature : ".concat(fnKey.signature)));
                return emptyFnResponseObj;
            });
        } catch (Exception ex) {
            LOGGER.error("Exception occurred while mocking function! Function Key : " + fnKey);
        }
        return emptyFnResponseObj;
    }

    static private Type getRetOrExceptionClass(FnResponse response, Type returnType) throws Exception {
        if (response.retStatus == FnReqRespPayload.RetStatus.Success) {
            return returnType;
        } else {
            return response.exceptionType.map(rethrowFunction(Class::forName)).map(TypeToken::get)
                    .map(TypeToken::getType).orElseThrow(() -> new Exception(
                            "Exception class not specified"));
        }
    }

    static final FnResponseObj emptyFnResponseObj = new FnResponseObj(null, Optional.empty(), FnReqRespPayload.RetStatus.Success,
            Optional.empty());

}
