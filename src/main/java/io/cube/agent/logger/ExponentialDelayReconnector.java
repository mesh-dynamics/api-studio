package io.cube.agent.logger;

import java.util.LinkedList;

/**
 * Calculates exponential delay for reconnecting. The start delay is 2000ms and exponentially grows to max 60 seconds in
 * function of the number of connection errors.
 */
public class ExponentialDelayReconnector implements Reconnector {
    // Visible for test
    public static final double WAIT_MILLIS = 2000; // Start wait is 2000ms

    private static final double WAIT_INCR_RATE = 1.5;

    private static final double WAIT_MAX_MILLIS = 60 * 1000; // Max wait is 1 minute

    private int waitMaxCount;

    private LinkedList<Long> errorHistory;
    private double suppressMillis;

    public ExponentialDelayReconnector() {
        waitMaxCount = getWaitMaxCount();
        errorHistory = new LinkedList<Long>();
    }

    private int getWaitMaxCount() {
        double r = WAIT_MAX_MILLIS / WAIT_MILLIS;
        for (int j = 1; j <= 100; j++) {
            if (r < WAIT_INCR_RATE) {
                return j + 1;
            }
            r = r / WAIT_INCR_RATE;
        }
        return 100;
    }

    public void addErrorHistory(long timestamp) {
        errorHistory.addLast(timestamp);
        if (errorHistory.size() > waitMaxCount) {
            errorHistory.removeFirst();
        }
        updateSuppressionDuration();
    }

    private void updateSuppressionDuration(){
        int size = errorHistory.size();
        if (size == 0) { //extra check , although will never be the case.
            suppressMillis = 0;
            return ;
        }

        if (size < waitMaxCount) {
            suppressMillis = WAIT_MILLIS * Math.pow(WAIT_INCR_RATE, size - 1);
        } else {
            suppressMillis = WAIT_MAX_MILLIS;
        }
    }

    public boolean isErrorHistoryEmpty() {
        return errorHistory.isEmpty();
    }

    public void clearErrorHistory() {
        if(!errorHistory.isEmpty()){
            errorHistory.clear();
            suppressMillis = 0;
        }

    }

    public boolean enableReconnection(long timestamp) {
        if(errorHistory.isEmpty()) return true;
        return (timestamp - errorHistory.getLast()) >= suppressMillis;
    }
}