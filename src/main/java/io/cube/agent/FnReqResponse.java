package io.cube.agent;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
class FnReqResponse {

    final int fnHash;
    final String name;
    final Integer[] argsHash;
    final String[] argVals;
    final String retVal;

    FnReqResponse(int fnHash, String name, Integer[] argsHash, String[] argVals, String retVal) {
        this.fnHash = fnHash;
        this.name = name;
        this.argsHash = argsHash;
        this.argVals = argVals;
        this.retVal = retVal;
    }
}
