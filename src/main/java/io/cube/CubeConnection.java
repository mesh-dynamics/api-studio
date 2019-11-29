package io.cube;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.google.gson.reflect.TypeToken;

import io.cube.agent.FnKey;

public class CubeConnection implements Connection {
    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final Driver driver;
    private final Connection connection;
    private final String url;
    private final Config config;
    private final int connectionInstanceId;
    private FnKey csFnKey;
    private FnKey psFnKey;
    private FnKey pcFnKey;
    private FnKey irFnKey;
    private FnKey gntFnKey;
    private FnKey icFnKey;
    private FnKey ivFnKey;
    private FnKey gmdFnkey;
    private FnKey gtiFnKey;
    private FnKey gacFnKey;
    private FnKey gcFnKey;
    private FnKey nsFnKey;
    private FnKey gwFnKey;
    private FnKey ghFnKey;
    private FnKey csrsFnKey;
    private FnKey csrshFnKey;
    private FnKey psaFnkey;
    private FnKey pscFnkey;
    private FnKey pscnFnkey;
    private FnKey psrsFnkey;
    private FnKey psrshFnkey;
    private FnKey pcrsFnKey;
    private FnKey pcrshFnKey;
    private FnKey ssFnKey;
    private FnKey ssnFnKey;
    private FnKey gsFnKey;
    private FnKey gciFnKey;
    private FnKey gcipFnkey;

    public CubeConnection(Config config, int connectionInstanceId) {
        this.driver = null;
        this.connection = null;
        this.url = null;
        this.config = config;
        this.connectionInstanceId = connectionInstanceId;
    }

    public CubeConnection(Connection connection, Driver driver, String url, Config config) {
        this.connection = connection;
        this.driver = driver;
        this.url = url;
        this.config = config;
        this.connectionInstanceId = url.hashCode();
    }

    public int getConnectionInstanceId() {
        return connectionInstanceId;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (null == csFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, csFnKey, Optional.of(integerType), this.connectionInstanceId);
            CubeStatement mockStatement = new CubeStatement(this, config,  (int) retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeStatement.getStatementInstanceId(), config, csFnKey, this.connectionInstanceId);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (null == psFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, psFnKey, Optional.of(integerType), sql, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, psFnKey, sql, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        if (null == pcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, pcFnKey, Optional.of(integerType), sql, this.connectionInstanceId);
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeCallableStatement.getStatementInstanceId(), config, pcFnKey, sql, this.connectionInstanceId);
        }

        return cubeCallableStatement;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        if (null == nsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, nsFnKey, (fnArgs) -> connection.nativeSQL(sql), sql, this.connectionInstanceId);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (null == gacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, gacFnKey, (fnArgs) -> connection.getAutoCommit(),
                this.connectionInstanceId);
    }

    @Override
    public void commit() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.commit();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.rollback();
        }
    }

    @Override
    public void close() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.close();
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icFnKey, (fnArgs) -> connection.isClosed(),
                this.connectionInstanceId);
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == gmdFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmdFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, gmdFnkey, Optional.of(integerType), this.connectionInstanceId);
            CubeDatabaseMetaData mockMetadata = new CubeDatabaseMetaData(this, config, (int) retVal);
            return mockMetadata;
        }

        CubeDatabaseMetaData metaData = new CubeDatabaseMetaData(connection.getMetaData(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(metaData.getMetadataInstanceId(), config, gmdFnkey, this.connectionInstanceId);
        }

        return metaData;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setReadOnly(readOnly);
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (null == irFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            irFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, irFnKey, (fnArgs) -> connection.isReadOnly(),
                this.connectionInstanceId);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setCatalog(catalog);
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        if (null == gcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gcFnKey, (fnArgs) -> connection.getCatalog(), this.connectionInstanceId);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setTransactionIsolation(level);
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gtiFnKey, (fnArgs) -> connection.getTransactionIsolation(), this.connectionInstanceId);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (null == gwFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gwFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (SQLWarning) Utils.recordOrMock(config, gwFnKey, (fnArgs) -> connection.getWarnings(), this.connectionInstanceId);
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.clearWarnings();
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == csrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, csrsFnKey, Optional.of(integerType), resultSetType, resultSetConcurrency, this.connectionInstanceId);
            CubeStatement mockStatement = new CubeStatement(this, config,  (int) retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(resultSetType, resultSetConcurrency), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeStatement.getStatementInstanceId(), config, csrsFnKey, resultSetType, resultSetConcurrency, this.connectionInstanceId);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == psrsFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psrsFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, psrsFnkey, Optional.of(integerType), sql, resultSetType, resultSetConcurrency, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, resultSetType, resultSetConcurrency), sql,this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, psrsFnkey, sql, resultSetType, resultSetConcurrency, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == pcrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, pcrsFnKey, Optional.of(integerType), sql, resultSetType, resultSetConcurrency, this.connectionInstanceId);
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql, resultSetType, resultSetConcurrency), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeCallableStatement.getStatementInstanceId(), config, pcrsFnKey, sql, resultSetType, resultSetConcurrency, this.connectionInstanceId);
        }

        return cubeCallableStatement;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setTypeMap(map);
        }
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setHoldability(holdability);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        if (null == ghFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ghFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, ghFnKey, (fnArgs) -> connection.getHoldability(), this.connectionInstanceId);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        if (null == ssFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, ssFnKey, Optional.of(integerType), this.connectionInstanceId);
            CubeSavepoint mockSavepoint = new CubeSavepoint(this, config, (int) retVal);
            return mockSavepoint;
        }

        CubeSavepoint cubeSavepoint = new CubeSavepoint(connection.setSavepoint(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeSavepoint.getSavepointInstanceId(), config, ssFnKey, this.connectionInstanceId);
        }

        return cubeSavepoint;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        if (null == ssnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, ssnFnKey, Optional.of(integerType), name, this.connectionInstanceId);
            CubeSavepoint mockSavepoint = new CubeSavepoint(this, config, (int) retVal);

            return mockSavepoint;
        }

        CubeSavepoint cubeSavepoint = new CubeSavepoint(connection.setSavepoint(name), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeSavepoint.getSavepointInstanceId(), config, ssnFnKey, name, this.connectionInstanceId);
        }

        return cubeSavepoint;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.rollback(savepoint);
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.releaseSavepoint(savepoint);
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == csrshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csrshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, csrshFnKey, Optional.of(integerType), resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);

            CubeStatement mockStatement = new CubeStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeStatement.getStatementInstanceId(), config, csrshFnKey, resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == psrshFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psrshFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, psrshFnkey, Optional.of(integerType), sql, resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, psrshFnkey, sql, resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == pcrshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcrshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, pcrshFnKey, Optional.of(integerType), sql, resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeCallableStatement.getStatementInstanceId(), config, pcrshFnKey, sql, resultSetType, resultSetConcurrency, resultSetHoldability, this.connectionInstanceId);
        }

        return cubeCallableStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == psaFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psaFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, psaFnkey, Optional.of(integerType), sql, autoGeneratedKeys, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, autoGeneratedKeys), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, psaFnkey, sql, autoGeneratedKeys, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        if (null == pscFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pscFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, pscFnkey, Optional.of(integerType), sql, columnIndexes, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, columnIndexes), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, pscFnkey, sql, columnIndexes, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        if (null == pscnFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pscnFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, pscnFnkey, Optional.of(integerType), sql, columnNames, this.connectionInstanceId);
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int) retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, columnNames), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubePreparedStatement.getStatementInstanceId(), config, pscnFnkey, sql, columnNames, this.connectionInstanceId);
        }

        return cubePreparedStatement;
    }

    @Override
    public Clob createClob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (null == ivFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ivFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ivFnKey, (fnArgs) -> connection.isValid(timeout), timeout, this.connectionInstanceId);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setClientInfo(name, value);
        }
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setClientInfo(properties);
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        if (null == gciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gciFnKey, (fnArgs) -> connection.getClientInfo(name), name, this.connectionInstanceId);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        if (null == gcipFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcipFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (Properties) Utils.recordOrMock(config, gcipFnkey, (fnArgs) -> connection.getClientInfo(), this.connectionInstanceId);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setSchema(schema);
        }
    }

    @Override
    public String getSchema() throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsFnKey, (fnArgs) -> connection.getSchema(), this.connectionInstanceId);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.abort(executor);
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setNetworkTimeout(executor, milliseconds);
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        if (null == gntFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gntFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gntFnKey, (fnArgs) -> connection.getNetworkTimeout(), this.connectionInstanceId);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        //TODO
        return false;
    }
}
