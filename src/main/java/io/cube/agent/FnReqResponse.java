package io.cube.agent;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
class FnReqResponse {

    final String traceid;
    final int fnHash;
    final String name;
    final Integer[] argsHash;
    final String[] argVals;
    final String retVal;

    FnReqResponse(String traceid, int fnHash, String name, Integer[] argsHash, String[] argVals, String retVal) {
        this.traceid = traceid;
        this.fnHash = fnHash;
        this.name = name;
        this.argsHash = argsHash;
        this.argVals = argVals;
        this.retVal = retVal;
    }
}
