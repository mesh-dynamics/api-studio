package io.cube.agent;

public class TraceIntentResolver implements IntentResolver {
    @Override
    public boolean isIntentToMock() {
        return Utils.isIntentToMock();
    }

    @Override
    public boolean isIntentToRecord() {
        return Utils.isIntentToRecord();
    }
}
