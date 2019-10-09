package io.cube;

import io.cube.agent.FnKey;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Savepoint;

public class CubeSavepoint implements Savepoint {

    private final Savepoint savepoint;
    private final CubeConnection cubeConnection;
    private final int savepointInstanceId;
    private final Config config;
    private FnKey gsFnKey;
    private FnKey gsnFnKey;

    public int getSavepointInstanceId() {
        return savepointInstanceId;
    }

    public CubeSavepoint(CubeConnection cubeConnection, Config config, int savepointInstanceId) {
        this.savepoint = null;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.savepointInstanceId = savepointInstanceId;
    }

    public CubeSavepoint(Savepoint savepoint, CubeConnection cubeConnection, Config config) {
        this.savepoint = savepoint;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.savepointInstanceId = System.identityHashCode(this);
    }

    @Override
    public int getSavepointId() throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gsFnKey, (fnArgs) -> savepoint.getSavepointId(),
                 this.savepointInstanceId);
    }

    @Override
    public String getSavepointName() throws SQLException {
        if (null == gsnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsnFnKey, (fnArgs) -> savepoint.getSavepointName(),
                 this.savepointInstanceId);
    }
}
