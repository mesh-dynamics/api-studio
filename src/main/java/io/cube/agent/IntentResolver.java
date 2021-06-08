package io.cube.agent;

public interface IntentResolver {

    //TODO: Change this to replay for better semantics
    public boolean isIntentToMock();

    public boolean isIntentToRecord();

}
