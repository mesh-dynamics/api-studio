/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.agent;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.agent.FnReqResponse.RetStatus;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponse {
    @JsonProperty("returnVal")
    public String retVal;
    @JsonProperty("recordTimestamp")
    Optional<Instant> timeStamp;
    @JsonProperty("returnStatus")
    RetStatus retStatus;
    @JsonProperty("exceptionType")
    Optional<String> exceptionType;

    // for jackson deserialization
    public FnResponse() {
    }

    public FnResponse(String retVal, Optional<Instant> timeStamp, RetStatus retStatus, Optional<String> exceptionType) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
        this.retStatus = retStatus;
        this.exceptionType = exceptionType;
    }
}
