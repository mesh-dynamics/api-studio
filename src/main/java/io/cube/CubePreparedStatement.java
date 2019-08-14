package io.cube;

import com.google.gson.reflect.TypeToken;
import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;

public class CubePreparedStatement extends CubeStatement implements PreparedStatement {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubePreparedStatement.class);
    private static Type type = new TypeToken<Integer>() {}.getType();
    private final PreparedStatement preparedStatement;
    private final CubeConnection cubeConnection;
    private final String query;
    private final int statementInstanceId;
    private final Config config;
    private FnKey statementFnKey;
    private FnKey gmrFnKey;
    private FnKey gqtFnKey;
    private FnKey euFnKey;
    private FnKey icFnKey;

    public CubePreparedStatement (Config config, int statementInstanceId) {
        super(config, statementInstanceId);
        this.preparedStatement = null;
        this.query = null;
        this.cubeConnection = null;
        this.config = config;
        this.statementInstanceId = statementInstanceId;
    }

    public CubePreparedStatement (PreparedStatement preparedStatement, String query, CubeConnection cubeConnection, Config config) {
        super(preparedStatement, cubeConnection, config);
        this.preparedStatement = preparedStatement;
        this.cubeConnection = cubeConnection;
        this.query = query;
        this.config = config;
        this.statementInstanceId = System.identityHashCode(this);
    }

    public int getStatementInstanceId() {
        return statementInstanceId;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        if (null == statementFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            statementFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(statementFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.of(type),
                    statementInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)ret.retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(preparedStatement.executeQuery()).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(statementFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeResultSet.getResultSetInstanceId(), FnReqResponse.RetStatus.Success,
                    Optional.empty(), statementInstanceId);
        }

        return cubeResultSet;
    }

    @Override
    public int executeUpdate() throws SQLException {
        if (null == euFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            euFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, euFnKey, false, this.statementInstanceId);
        }

        int toReturn = preparedStatement.executeUpdate();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, euFnKey, true, this.statementInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNull(parameterIndex, sqlType);
        }
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBoolean(parameterIndex, x);
        }
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setByte(parameterIndex, x);
        }
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setShort(parameterIndex, x);
        }
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setInt(parameterIndex, x);
        }
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setLong(parameterIndex, x);
        }
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setFloat(parameterIndex, x);
        }
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setDouble(parameterIndex, x);
        }
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBigDecimal(parameterIndex, x);
        }
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setString(parameterIndex, x);
        }
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBytes(parameterIndex, x);
        }
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setDate(parameterIndex, x);
        }
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setTime(parameterIndex, x);
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setTimestamp(parameterIndex, x);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setAsciiStream(parameterIndex, x, length);
        }
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setUnicodeStream(parameterIndex, x, length);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBinaryStream(parameterIndex, x, length);
        }
    }

    @Override
    public void clearParameters() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.clearParameters();
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setObject(parameterIndex, x, targetSqlType);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setObject(parameterIndex, x);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        return preparedStatement.execute();
    }

    @Override
    public void addBatch() throws SQLException {
        preparedStatement.addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setCharacterStream(parameterIndex, reader, length);
        }
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setRef(parameterIndex, x);
        }
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBlob(parameterIndex, x);
        }
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setClob(parameterIndex, x);
        }
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setArray(parameterIndex, x);
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return preparedStatement.getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setDate(parameterIndex, x, cal);
        }
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setTime(parameterIndex, x, cal);
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setTimestamp(parameterIndex, x, cal);
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNull(parameterIndex, sqlType, typeName);
        }
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setURL(parameterIndex, x);
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setRowId(parameterIndex, x);
        }
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNString(parameterIndex, value);
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNCharacterStream(parameterIndex, value, length);
        }
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNClob(parameterIndex, value);
        }
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setClob(parameterIndex, reader, length);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBlob(parameterIndex, inputStream, length);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNClob(parameterIndex, reader, length);
        }
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setSQLXML(parameterIndex, xmlObject);
        }
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setAsciiStream(parameterIndex, x, length);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBinaryStream(parameterIndex, x, length);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setCharacterStream(parameterIndex, reader, length);
        }
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setAsciiStream(parameterIndex, x);
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBinaryStream(parameterIndex, x);
        }
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setCharacterStream(parameterIndex, reader);
        }
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNCharacterStream(parameterIndex, value);
        }
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setClob(parameterIndex, reader);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setBlob(parameterIndex, inputStream);
        }
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setNClob(parameterIndex, reader);
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return preparedStatement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return preparedStatement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return preparedStatement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setMaxFieldSize(max);
        }
    }

    @Override
    public int getMaxRows() throws SQLException {
        if (null == gmrFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gmrFnKey, false, this.statementInstanceId);
        }

        int toReturn = preparedStatement.getMaxRows();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gmrFnKey, true, this.statementInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setMaxRows(max);
        }
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setEscapeProcessing(enable);
        }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        if (null == gqtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gqtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gqtFnKey, false, this.statementInstanceId);
        }

        int toReturn = preparedStatement.getQueryTimeout();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gqtFnKey, true, this.statementInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.setQueryTimeout(seconds);
        }
    }

    @Override
    public void cancel() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.cancel();
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return preparedStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.clearWarnings();
        }
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setCursorName(name);
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return preparedStatement.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return preparedStatement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return preparedStatement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return preparedStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setFetchDirection(direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return preparedStatement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        return preparedStatement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return preparedStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return preparedStatement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        preparedStatement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        preparedStatement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return preparedStatement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return preparedStatement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return preparedStatement.getMoreResults();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return preparedStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return preparedStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return preparedStatement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return preparedStatement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return preparedStatement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return preparedStatement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return preparedStatement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return preparedStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, icFnKey, false, this.statementInstanceId);
        }

        boolean toReturn = preparedStatement.isClosed();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, icFnKey, true, this.statementInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            preparedStatement.setPoolable(poolable);
        }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return preparedStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        preparedStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return preparedStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return preparedStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return preparedStatement.isWrapperFor(iface);
    }
}
