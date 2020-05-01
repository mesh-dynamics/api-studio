package io.cube.agent;

import java.util.Optional;

import io.md.dao.Event;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.utils.FnKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-03
 * @author Prasad M D
 */
public interface Recorder {


    /**
     * @param fnKey               The object storing the function key, that will not change on each
     *                            invocation of the function
     * @param args                The arg values
     * @param responseOrException The return value or the exception value
     * @param retStatus           Success or exception
     * @param exceptionType       Type of exception if any
     * @return success status
     */
    boolean record(FnKey fnKey,
        Object responseOrException,
        RetStatus retStatus,
        Optional<String> exceptionType,
        Object... args);

    boolean record(ReqResp httpReqResp);

    boolean record(Event event);
}