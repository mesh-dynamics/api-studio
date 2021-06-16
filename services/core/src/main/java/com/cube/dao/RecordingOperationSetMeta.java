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

package com.cube.dao;

import java.util.UUID;

public class RecordingOperationSetMeta {
    public final String id;
    public final String customer;
    public final String app;

    // constructor that generates an id
    public RecordingOperationSetMeta(String customer, String app) {
        this.customer = customer;
        this.app = app;
        this.id = generateId();
    }

    // constructor that takes in an id
    public RecordingOperationSetMeta(String id, String customer, String app) {
        this.customer = customer;
        this.app = app;
        this.id = id;
    }

    // generate unique id for the entire operation set
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
