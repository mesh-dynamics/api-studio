package io.md.services;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.dao.FnReqRespPayload;
import io.md.dao.FnReqRespPayload.RetStatus;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponse {
    @JsonProperty("returnVal")
    final public String retVal;
    @JsonProperty("recordTimestamp")
    final public Optional<Instant> timeStamp;
    @JsonProperty("returnStatus")
    final public FnReqRespPayload.RetStatus retStatus;
    @JsonProperty("exceptionType")
    final public Optional<String> exceptionType;
    @JsonProperty("multipleResults")
    final public boolean multipleResults;

    // for jackson deserialization
    public FnResponse() {
        retVal = "";
        timeStamp = Optional.empty();
        retStatus = RetStatus.Success;
        exceptionType = Optional.empty();
        multipleResults = false;
    }

    public FnResponse(String retVal, Optional<Instant> timeStamp, FnReqRespPayload.RetStatus retStatus, Optional<String> exceptionType, boolean multipleResults) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
        this.retStatus = retStatus;
        this.exceptionType = exceptionType;
        this.multipleResults = multipleResults;
    }
}
