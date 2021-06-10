package io.cube;

import static io.cube.Utils.getFnKey;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import com.google.gson.reflect.TypeToken;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import com.google.gson.reflect.TypeToken;

import io.md.utils.FnKey;


public class MDCallableStatement extends MDPreparedStatement implements CallableStatement {
    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final CallableStatement callableStatement;
    private final Config config;
    private int parameterIndex;
    private String parameterName;
    private FnKey wnFnKey;
    private FnKey gsFnKey;
    private FnKey gbFnKey;
    private FnKey gbcFnKey;
    private FnKey gscFnKey;
    private FnKey giFnKey;
    private FnKey gicFnKey;
    private FnKey glcFnKey;
    private FnKey gshFnKey;
    private FnKey gshcFnKey;
    private FnKey glclFnKey;
    private FnKey gbyFnKey;
    private FnKey gtFnKey;
    private FnKey gtcFnKey;
    private FnKey gtclFnKey;
    private FnKey gtccFnKey;
    private FnKey gfFnKey;
    private FnKey gfcFnKey;
    private FnKey gdFnKey;
    private FnKey gdcFnKey;
    private FnKey gbdFnKey;
    private FnKey gbdcFnKey;
    private FnKey gbdcsFnKey;
    private FnKey gdclFnKey;
    private FnKey gdciFnKey;
    private FnKey gdcicFnKey;
    private FnKey gdclcFnKey;
    private FnKey gtiFnKey;
    private FnKey gticFnKey;
    private FnKey gticlFnKey;
    private FnKey gticlcFnKey;
    private FnKey gbysFnKey;
    private FnKey gbyscFnKey;
    private FnKey goFnKey;
    private FnKey gocFnKey;
    private FnKey guFnKey;
    private FnKey gurFnKey;
    private FnKey gnsFnKey;
    private FnKey gnscFnKey;
    private FnKey gcFnkey;
    private FnKey gcpnFnkey;
    private FnKey gbpiFnKey;
    private FnKey gbpnFnkey;


    public MDCallableStatement(MDConnection mdConnection, Config config, int statementInstanceId) {
        super(mdConnection, config, statementInstanceId);
        this.callableStatement = null;
        this.lastExecutedQuery = null;
        this.config = config;
    }

    public MDCallableStatement(CallableStatement callableStatement, String query, MDConnection mdConnection, Config config) {
        super(callableStatement, query, mdConnection, config);
        this.callableStatement = callableStatement;
        this.lastExecutedQuery = query;
        this.config = config;
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterIndex, sqlType);
        }
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        if (null == wnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            wnFnKey = getFnKey(method);
        }

        return (boolean) Utils.recordOrMock(config, wnFnKey, (fnArgs) -> callableStatement.wasNull(),
                this.statementInstanceId, this.parameterIndex, this.parameterName);
    }

    @Override
    public String getString(int parameterIndex) throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (String) Utils.recordOrMock(config, gsFnKey, (fnArgs) -> callableStatement.getString(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        if (null == gbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (boolean) Utils.recordOrMock(config, gbFnKey,
                (fnArgs) -> callableStatement.getBoolean(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (byte) Utils.recordOrMock(config, gbyFnKey,
                (fnArgs) -> callableStatement.getByte(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        if (null == gshcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshcFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (short) Utils.recordOrMock(config, gshcFnKey,
                (fnArgs) -> callableStatement.getShort(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        if (null == gicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gicFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (int) Utils.recordOrMock(config, gicFnKey,
                (fnArgs) -> callableStatement.getInt(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        if (null == glcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glcFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (long) Utils.recordOrMock(config, glcFnKey,
                (fnArgs) -> callableStatement.getLong(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        if (null == gfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (float) Utils.recordOrMock(config, gfFnKey,
                (fnArgs) -> callableStatement.getFloat(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        if (null == gdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Double) Utils.recordOrMock(config, gdFnKey,
                (fnArgs) -> callableStatement.getDouble(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        if (null == gbdcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcsFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdcsFnKey,
                (fnArgs) -> callableStatement.getBigDecimal(parameterIndex, scale), parameterIndex, scale, this.statementInstanceId);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        if (null == gbysFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbysFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (byte[]) Utils.recordOrMock(config, gbysFnKey,
                (fnArgs) -> callableStatement.getBytes(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        if (null == gdciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdciFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Date) Utils.recordOrMock(config, gdciFnKey,
                (fnArgs) -> callableStatement.getDate(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Time) Utils.recordOrMock(config, gtiFnKey,
                (fnArgs) -> callableStatement.getTime(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        if (null == gtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Timestamp) Utils.recordOrMock(config, gtFnKey,
                (fnArgs) -> callableStatement.getTimestamp(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        if (null == goFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            goFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return Utils.recordOrMock(config, goFnKey,
                (fnArgs) -> callableStatement.getObject(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        if (null == gbdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdFnKey,
                (fnArgs) -> callableStatement.getBigDecimal(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getObject(parameterIndex, map);
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        if (null == gbpiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbpiFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Blob) Utils.recordOrMock(config, gbpiFnKey,
            (fnArgs) -> new SerialBlob(callableStatement.getBlob(parameterIndex)), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        if (null == gcFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcFnkey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Clob) Utils.recordOrMock(config, gcFnkey,
            (fnArgs) -> new SerialClob(callableStatement.getClob(parameterIndex)), parameterIndex, this.statementInstanceId);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        if (null == gdcicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcicFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Date) Utils.recordOrMock(config, gdcicFnKey,
                (fnArgs) -> callableStatement.getDate(parameterIndex, cal), parameterIndex, cal, this.statementInstanceId);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        if (null == gticFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Time) Utils.recordOrMock(config, gticFnKey,
                (fnArgs) -> callableStatement.getTime(parameterIndex, cal), parameterIndex, cal, this.statementInstanceId);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        if (null == gtcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtcFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (Timestamp) Utils.recordOrMock(config, gtcFnKey,
                (fnArgs) -> callableStatement.getTimestamp(parameterIndex, cal), parameterIndex, cal, this.statementInstanceId);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterName, sqlType);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterName, sqlType, scale);
        }
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            callableStatement.registerOutParameter(parameterName, sqlType, typeName);
        }
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        if (null == guFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            guFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (URL) Utils.recordOrMock(config, guFnKey,
                (fnArgs) -> callableStatement.getURL(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setURL(parameterName, val);
        }
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNull(parameterName, sqlType);
        }
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBoolean(parameterName, x);
        }
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setByte(parameterName, x);
        }
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setShort(parameterName, x);
        }
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setInt(parameterName, x);
        }
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setLong(parameterName, x);
        }
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setFloat(parameterName, x);
        }
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setDouble(parameterName, x);
        }
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBigDecimal(parameterName, x);
        }
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setString(parameterName, x);
        }
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBytes(parameterName, x);
        }
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setDate(parameterName, x);
        }
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setTime(parameterName, x);
        }
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setTimestamp(parameterName, x);
        }
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setAsciiStream(parameterName, x, length);
        }
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBinaryStream(parameterName, x, length);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setObject(parameterName, x, targetSqlType, scale);
        }
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setObject(parameterName, x, targetSqlType);
        }
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setObject(parameterName, x);
        }
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setCharacterStream(parameterName, reader, length);
        }
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setDate(parameterName, x, cal);
        }
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setTime(parameterName, x, cal);
        }
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setTimestamp(parameterName, x, cal);
        }
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNull(parameterName, sqlType, typeName);
        }
    }

    @Override
    public String getString(String parameterName) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (String) Utils.recordOrMock(config, gscFnKey,
                (fnArgs) -> callableStatement.getString(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        if (null == gbcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (boolean) Utils.recordOrMock(config, gbcFnKey,
                (fnArgs) -> callableStatement.getBoolean(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (byte) Utils.recordOrMock(config, gbyFnKey,
                (fnArgs) -> callableStatement.getByte(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        if (null == gshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (short) Utils.recordOrMock(config, gshFnKey,
                (fnArgs) -> callableStatement.getShort(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        if (null == giFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (int) Utils.recordOrMock(config, giFnKey,
                (fnArgs) -> callableStatement.getInt(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        if (null == glclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glclFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (long) Utils.recordOrMock(config, glclFnKey,
                (fnArgs) ->  callableStatement.getLong(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        if (null == gfcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (float) Utils.recordOrMock(config, gfcFnKey,
                (fnArgs) -> callableStatement.getFloat(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        if (null == gdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (double) Utils.recordOrMock(config, gdcFnKey,
                (fnArgs) -> callableStatement.getDouble(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        if (null == gbyscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyscFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (byte[]) Utils.recordOrMock(config, gbyscFnKey,
                (fnArgs) -> callableStatement.getBytes(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        if (null == gdclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Date) Utils.recordOrMock(config, gdclFnKey,
                (fnArgs) -> callableStatement.getDate(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        if (null == gticlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Time) Utils.recordOrMock(config, gticlFnKey,
                (fnArgs) -> callableStatement.getTime(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        if (null == gtclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtclFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Timestamp) Utils.recordOrMock(config, gtclFnKey,
                (fnArgs) -> callableStatement.getTimestamp(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        if (null == gocFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gocFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return Utils.recordOrMock(config, gocFnKey,
                (fnArgs) -> callableStatement.getObject(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        if (null == gbdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (BigDecimal) Utils.recordOrMock(config, gbdcFnKey,
                (fnArgs) -> callableStatement.getBigDecimal(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getObject(parameterName, map);
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        if (null == gbpnFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbpnFnkey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Blob) Utils.recordOrMock(config, gbpnFnkey,
            (fnArgs) -> new SerialBlob(callableStatement.getBlob(parameterName)), parameterName, this.statementInstanceId);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        if (null == gcpnFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcpnFnkey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Clob) Utils.recordOrMock(config, gcpnFnkey,
            (fnArgs) -> new SerialClob(callableStatement.getClob(parameterName)), parameterName, this.statementInstanceId);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        if (null == gdclcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Date) Utils.recordOrMock(config, gdclcFnKey,
                (fnArgs) -> callableStatement.getDate(parameterName, cal), parameterName, cal, this.statementInstanceId);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        if (null == gticlcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlcFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Time) Utils.recordOrMock(config, gticlcFnKey,
                (fnArgs) -> callableStatement.getTime(parameterName, cal), parameterName, cal, this.statementInstanceId);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        if (null == gtccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtccFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (Timestamp) Utils.recordOrMock(config, gtccFnKey,
                (fnArgs) -> callableStatement.getTimestamp(parameterName, cal), parameterName, cal, this.statementInstanceId);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        if (null == gurFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gurFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (URL) Utils.recordOrMock(config, gurFnKey,
                (fnArgs) -> callableStatement.getURL(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setRowId(parameterName, x);
        }
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNString(parameterName, value);
        }
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNCharacterStream(parameterName, value, length);
        }
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNClob(parameterName, value);
        }
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setClob(parameterName, reader, length);
        }
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBlob(parameterName, inputStream, length);
        }
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNClob(parameterName, reader, length);
        }
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setSQLXML(parameterName, xmlObject);
        }
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        if (null == gnsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnsFnKey = getFnKey(method);
        }

        this.parameterIndex = parameterIndex;
        return (String) Utils.recordOrMock(config, gnsFnKey,
                (fnArgs) -> callableStatement.getNString(parameterIndex), parameterIndex, this.statementInstanceId);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        if (null == gnscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnscFnKey = getFnKey(method);
        }

        this.parameterName = parameterName;
        return (String) Utils.recordOrMock(config, gnscFnKey,
                (fnArgs) -> callableStatement.getNString(parameterName), parameterName, this.statementInstanceId);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBlob(parameterName, x);
        }
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setClob(parameterName, x);
        }
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setAsciiStream(parameterName, x, length);
        }
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBinaryStream(parameterName, x, length);
        }
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setCharacterStream(parameterName, reader, length);
        }
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setAsciiStream(parameterName, x);
        }
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBinaryStream(parameterName, x);
        }
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setCharacterStream(parameterName, reader);
        }
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNCharacterStream(parameterName, value);
        }
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setClob(parameterName, reader);
        }
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setBlob(parameterName, inputStream);
        }
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        if(!config.intentResolver.isIntentToMock()) {
            callableStatement.setNClob(parameterName, reader);
        }
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getObject(parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return callableStatement.getObject(parameterName, type);
    }
}
