package io.md.dao;

import java.time.Instant;

public class ReplayContext {
    public String reqTraceId;
    public Instant reqStartTs;
    public Instant reqEndTs;

    public ReplayContext(){};
    public ReplayContext(String reqTraceId , Instant reqStartTs , Instant reqEndTs){
        this.reqTraceId = reqTraceId;
        this.reqStartTs = reqStartTs;
        this.reqEndTs = reqEndTs;
    }
}
