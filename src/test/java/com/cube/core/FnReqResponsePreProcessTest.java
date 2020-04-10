package com.cube.core;

import java.util.Optional;

import io.cube.agent.FnReqResponse;
import io.md.dao.FnReqRespPayload.RetStatus;

public class FnReqResponsePreProcessTest {

    public static void main(String[] args) {


        /**
         * String customerId, String app, String instanceId, String service,
         *                          int fnSignatureHash, String name, Optional<String> traceId,
         *                          Optional<String> spanId, Optional<String> parentSpanId, Optional<Instant> respTS,
         *                          Integer[] argsHash, String[] argVals, String retVal
         */
        String argVal =
            "{\"id\":{\"name\":\"id\",\"value\":\"Analysis-test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\"},\"replayid_s\":{\"name\":\"replayid_s\",\"value\":\"test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\"},\"json_ni\":{\"name\":\"json_ni\",\"value\":\"{\\\"replayid\\\":\\\"test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\\\",\\\"status\\\":\\\"Running\\\",\\\"reqcnt\\\":0,\\\"reqmatched\\\":0,\\\"reqpartiallymatched\\\":0,\\\"reqsinglematch\\\":0,\\\"reqmultiplematch\\\":0,\\\"reqnotmatched\\\":0,\\\"respmatched\\\":0,\\\"resppartiallymatched\\\":0,\\\"respnotmatched\\\":0,\\\"timestamp\\\":1559220495736,\\\"reqanalyzed\\\":0}\"},\"type_s\":{\"name\":\"type_s\",\"value\":\"Analysis\"}}";
           // "{\"type_s\":{\"name\":\"type_s\",\"value\":\"Recording\"},\"id\":{\"name\":\"id\",\"value\":\"Recording-ravivj-movieinfo-test4-30-may-movieinfo\"},\"customerid_s\":{\"name\":\"customerid_s\",\"value\":\"ravivj\"},\"app_s\":{\"name\":\"app_s\",\"value\":\"movieinfo\"},\"instanceid_s\":{\"name\":\"instanceid_s\",\"value\":\"staging\"},\"collection_s\":{\"name\":\"collection_s\",\"value\":\"test4-30-may-movieinfo\"},\"status_s\":{\"name\":\"status_s\",\"value\":\"Completed\"},\"timestamp_dt\":{\"name\":\"timestamp_dt\",\"value\":\"2019-05-30T12:48:14.106997Z\"}}";
        System.out.println(argVal);
        int argHash = argVal.hashCode();
        System.out.println(argHash);
        FnReqResponse fnReqResponse = new FnReqResponse("ravivj" , "moveinfo" , "dev",
            "movieinfo", 1, "add", Optional.empty() , Optional.empty() , Optional.empty(),
            Optional.empty(), new Integer[]{argHash}, new String[]{argVal} , "random",
            RetStatus.Success, Optional.empty());


        Utils.preProcess(fnReqResponse);
        System.out.println(fnReqResponse.argVals[0]);
        System.out.println(fnReqResponse.argsHash[0]);

        System.out.println();

        argVal =
            "{\"id\":{\"name\":\"id\",\"value\":\"Analysis-test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\"},\"replayid_s\":{\"name\":\"replayid_s\",\"value\":\"test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\"},\"json_ni\":{\"name\":\"json_ni\",\"value\":\"{\\\"replayid\\\":\\\"test4-30-may-movieinfo-221662e0-2bbf-405b-8fcf-deb149e5c235\\\",\\\"status\\\":\\\"Running\\\",\\\"reqcnt\\\":0,\\\"reqmatched\\\":0,\\\"reqpartiallymatched\\\":0,\\\"reqsinglematch\\\":0,\\\"reqmultiplematch\\\":0,\\\"reqnotmatched\\\":0,\\\"respmatched\\\":0,\\\"resppartiallymatched\\\":0,\\\"respnotmatched\\\":0,\\\"timestamp\\\":1559220216950,\\\"reqanalyzed\\\":0}\"},\"type_s\":{\"name\":\"type_s\",\"value\":\"Analysis\"}}";
           // "{\"type_s\":{\"name\":\"type_s\",\"value\":\"Recording\"},\"id\":{\"name\":\"id\",\"value\":\"Recording-ravivj-movieinfo-test4-30-may-movieinfo\"},\"customerid_s\":{\"name\":\"customerid_s\",\"value\":\"ravivj\"},\"app_s\":{\"name\":\"app_s\",\"value\":\"movieinfo\"},\"instanceid_s\":{\"name\":\"instanceid_s\",\"value\":\"staging\"},\"collection_s\":{\"name\":\"collection_s\",\"value\":\"test4-30-may-movieinfo\"},\"status_s\":{\"name\":\"status_s\",\"value\":\"Completed\"},\"timestamp_dt\":{\"name\":\"timestamp_dt\",\"value\":\"2018-05-30T12:48:14.106997Z\"}}";
        System.out.println(argVal);
        argHash = argVal.hashCode();
        System.out.println(argHash);
        fnReqResponse = new FnReqResponse("ravivj" , "moveinfo" , "dev",
            "movieinfo", 1, "add", Optional.empty() , Optional.empty() , Optional.empty(),
            Optional.empty(), new Integer[]{argHash}, new String[]{argVal} , "random",
            RetStatus.Success, Optional.empty());

        Utils.preProcess(fnReqResponse);
        System.out.println(fnReqResponse.argVals[0]);
        System.out.println(fnReqResponse.argsHash[0]);


    }

}
