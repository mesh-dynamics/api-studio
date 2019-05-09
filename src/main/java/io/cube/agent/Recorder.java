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
     * @param args The arg values
     * @param response The return value
     * @return success status
     */
    boolean record(FnKey fnKey,
                   Optional<Instant> prevRespTS,
                   Object response,
                   Object... args);

}
