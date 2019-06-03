package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponseObj {

    final public Object retVal;
    final public Optional<Instant> timeStamp;
    final public FnReqResponse.RetStatus retStatus;
    final Optional<String> exceptionType;



    public FnResponseObj(Object retVal, Optional<Instant> timeStamp, FnReqResponse.RetStatus retStatus, Optional<String> exceptionType) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
        this.retStatus = retStatus;
        this.exceptionType = exceptionType;
    }
}
