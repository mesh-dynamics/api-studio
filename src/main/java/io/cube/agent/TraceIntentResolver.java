package io.cube.agent;

public class TraceIntentResolver implements IntentResolver {
    @Override
    public boolean isIntentToMock() {
        return CommonUtils.isIntentToMock();
    }

    @Override
    public boolean isIntentToRecord() {
        return CommonUtils.isIntentToRecord();
    }
}
