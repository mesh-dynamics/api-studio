package io.cube.agent;

import com.google.gson.Gson;
import java.util.Optional;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleHttpRecorder extends AbstractGsonSerializeRecorder {

    private CubeClient cubeClient;

    public SimpleHttpRecorder(Gson gson) {
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


}
