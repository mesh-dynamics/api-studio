package io.cube.agent.logger;

public interface Reconnector {
    void clearErrorHistory();

    void addErrorHistory(long timestamp);

    boolean isErrorHistoryEmpty();

    boolean enableReconnection(long timestamp);
}