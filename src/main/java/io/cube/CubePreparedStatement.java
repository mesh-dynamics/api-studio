package io.cube;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

import io.cube.agent.FnKey;

public class CubePreparedStatement extends CubeStatement implements PreparedStatement {

    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final PreparedStatement preparedStatement;
    private final Config config;
    private FnKey exqFnKey;
    private FnKey euFnKey;
    private FnKey exFnKey;

    public CubePreparedStatement (CubeConnection cubeConnection, Config config, int statementInstanceId) {
        super(cubeConnection, config, statementInstanceId);
        this.preparedStatement = null;
        this.lastExecutedQuery = null;
        this.config = config;
    }

    public CubePreparedStatement (PreparedStatement preparedStatement, String query, CubeConnection cubeConnection, Config config) {
        super(preparedStatement, cubeConnection, config);
        this.preparedStatement = preparedStatement;
        this.lastExecutedQuery = query;
        this.config = config;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        if (null == exqFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exqFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, exqFnKey, Optional.of(integerType), this.statementInstanceId);
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(preparedStatement.executeQuery()).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeResultSet.getResultSetInstanceId(), config, exqFnKey, this.statementInstanceId);
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

        return (int) Utils.recordOrMock(config, euFnKey, (fnArgs) -> preparedStatement.executeUpdate(),
                this.statementInstanceId);
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
        if (null == exFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, exFnKey, (fnArgs) -> preparedStatement.execute(),
                this.statementInstanceId);
    }

    @Override
    public void addBatch() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            preparedStatement.addBatch();
        }
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
        //TODO
        return null;
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
        //TODO
        return null;
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
}
