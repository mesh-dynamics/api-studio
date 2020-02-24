package io.cube.agent;

import java.util.Optional;

import com.google.gson.Gson;

import io.md.dao.Event;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleHttpRecorder extends AbstractGsonSerializeRecorder {

    private CubeClient cubeClient;

    public SimpleHttpRecorder(Gson gson) throws Exception {
        super(gson);
        this.cubeClient = new CubeClient(jsonMapper);
    }

    @Override
    public boolean record(FnReqResponse fnReqResponse) {
        Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnReqResponse);
        return true;
    }

    @Override
    public boolean record(Event event) {
        Optional<String> cubeResponse = cubeClient.storeEvent(event);
        return true;
    }


    @Override
    public boolean record(ReqResp httpReqResp) {
        Optional<String> cubeResponse = cubeClient.storeSingleReqResp(httpReqResp);
        return true;
    }
}
