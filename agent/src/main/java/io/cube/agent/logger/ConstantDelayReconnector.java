/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Handles constant delay for reconnecting. The default delay is 50 ms.
 */
public class ConstantDelayReconnector implements Reconnector {
    private static final Logger LOG = LoggerFactory.getLogger(ConstantDelayReconnector.class);

    private double wait = 50; // Default wait to 50 ms

    private static final int MAX_ERROR_HISTORY_SIZE = 100;

    private Deque<Long> errorHistory = new LinkedList<Long>();

    public ConstantDelayReconnector() {}

    public ConstantDelayReconnector(int wait) {
        this.wait = wait;
    }

    public void addErrorHistory(long timestamp) {
        errorHistory.addLast(timestamp);
        if (errorHistory.size() > MAX_ERROR_HISTORY_SIZE) {
            errorHistory.removeFirst();
        }
    }

    public boolean isErrorHistoryEmpty() {
        return errorHistory.isEmpty();
    }

    public void clearErrorHistory() {
        errorHistory.clear();
    }

    public boolean enableReconnection(long timestamp) {
        return errorHistory.isEmpty() || timestamp - errorHistory.getLast() >= wait;
    }
}