package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-03
 * @author Prasad M D
 */
public interface Recorder {


    /**
     * @param fnKey The object storing the function key, that will not change on each invocation of the function
     * @param traceId The traceid of this sequence of calls
     * @param spanId The span of the calling function (useful to distinguish between multiple sequence of calls
     *               within a fixed traceId
     * @param parentSpanId The parent span id
     * @param args The arg values
     * @param response The return value
     * @return success status
     */
    boolean record(FnKey fnKey,
                   Optional<String> traceId,
                   Optional<String> spanId,
                   Optional<String> parentSpanId,
                   Object response,
                   Object... args);

}
