package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponse {
    final public String retVal;
    final Optional<Instant> timeStamp;


    public FnResponse(String retVal, Optional<Instant> timeStamp) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
    }
}
