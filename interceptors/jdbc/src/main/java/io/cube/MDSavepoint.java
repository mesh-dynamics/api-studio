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

package io.cube;

import static io.cube.Utils.getFnKey;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Savepoint;

import io.md.utils.FnKey;

public class MDSavepoint implements Savepoint {

    private final Savepoint savepoint;
    private final MDConnection mdConnection;
    private final int savepointInstanceId;
    private final Config config;
    private FnKey gsFnKey;
    private FnKey gsnFnKey;

    public int getSavepointInstanceId() {
        return savepointInstanceId;
    }

    public MDSavepoint(MDConnection mdConnection, Config config, int savepointInstanceId) {
        this.savepoint = null;
        this.mdConnection = mdConnection;
        this.config = config;
        this.savepointInstanceId = savepointInstanceId;
    }

    public MDSavepoint(Savepoint savepoint, MDConnection mdConnection, Config config) {
        this.savepoint = savepoint;
        this.mdConnection = mdConnection;
        this.config = config;
        this.savepointInstanceId = System.identityHashCode(this);
    }

    @Override
    public int getSavepointId() throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = getFnKey(method);
        }

        return (int) Utils.recordOrMock(config, gsFnKey, (fnArgs) -> savepoint.getSavepointId(),
                 this.savepointInstanceId);
    }

    @Override
    public String getSavepointName() throws SQLException {
        if (null == gsnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsnFnKey = getFnKey(method);
        }

        return (String) Utils.recordOrMock(config, gsnFnKey, (fnArgs) -> savepoint.getSavepointName(),
                 this.savepointInstanceId);
    }
}
