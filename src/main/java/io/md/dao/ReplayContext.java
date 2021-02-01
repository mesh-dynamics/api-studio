package io.md.dao;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ReplayContext {
    public Optional<String> reqTraceId;
    public Optional<Instant> reqStartTs;
    public Optional<Instant> reqEndTs;
    public Optional<String> currentRecording;

    public final Map<String , Instant> mockReqsMatchInfo = new HashMap<>();

    public ReplayContext(){
        reqTraceId = Optional.empty();
        reqStartTs = Optional.empty();
        reqEndTs = Optional.empty();
        currentRecording = Optional.empty();
    }

    public ReplayContext(String reqTraceId , Instant reqStartTs , Instant reqEndTs){
        this.reqTraceId = Optional.ofNullable(reqTraceId);
        this.reqStartTs = Optional.ofNullable(reqStartTs);
        this.reqEndTs = Optional.ofNullable(reqEndTs);
        this.currentRecording = Optional.empty();
    }

    @JsonIgnore
    public void setCurrentRecordingCollection(String currentRecording) {
        this.currentRecording = Optional.of(currentRecording);
    }


    @JsonIgnore
    public void setMockResultToReplayContext(Event event){
        String id = getEventUniqueMockReqId(event);
        mockReqsMatchInfo.put(id , event.timestamp);
    }

    @JsonIgnore
    public Optional<Instant> getLastMockEventTs(Event event){
        String id = getEventUniqueMockReqId(event);
        return Optional.ofNullable(mockReqsMatchInfo.get(id));
    }

    @JsonIgnore
    public static String getEventUniqueMockReqId(Event event){
        return String.format("%s-%s-%s-%s" , event.service , event.apiPath , event.getTraceId() , event.payloadKey);
    }

    /*
    @JsonIgnore
    public boolean isTracePropagationEnabled(){
       return !reqStartTs.isPresent() && !reqEndTs.isPresent();
    }
    */
}
