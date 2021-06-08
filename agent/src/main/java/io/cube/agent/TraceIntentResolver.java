package io.cube.agent;

public class TraceIntentResolver implements IntentResolver {
    @Override
    public boolean isIntentToMock() {
        return CommonConfig.isIntentToMock();
    }

    @Override
    public boolean isIntentToRecord() {
        return CommonConfig.isIntentToRecord();
    }
}
