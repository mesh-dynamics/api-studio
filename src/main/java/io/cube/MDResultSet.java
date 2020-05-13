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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import com.google.gson.reflect.TypeToken;

import io.md.utils.FnKey;

public class MDResultSet implements ResultSet {
    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final ResultSet resultSet;
    private final MDStatement mdStatement;
    private final Config config;
    private final int resultSetInstanceId;
    private final MDDatabaseMetaData metaData;
    private final String query;
    private int rowIndex;
    private int columnIndex; /* Needed for wasNull call */
    private String columnLabel;
    private FnKey nxtFnKey;
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
    private FnKey ifFnKey;
    private FnKey ilFnKey;
    private FnKey ibfFnKey;
    private FnKey ialFnKey;
    private FnKey fFnKey;
    private FnKey lFnKey;
    private FnKey grFnKey;
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
    private FnKey gbdclsFnKey;
    private FnKey gdclFnKey;
    private FnKey gdciFnKey;
    private FnKey gdcicFnKey;
    private FnKey gdclcFnKey;
    private FnKey gtiFnKey;
    private FnKey gticFnKey;
    private FnKey gticlFnKey;
    private FnKey gticlcFnKey;
    private FnKey gwFnKey;
    private FnKey gcnFnKey;
    private FnKey fcFnKey;
    private FnKey prevFnKey;
    private FnKey icFnKey;
    private FnKey gbysFnKey;
    private FnKey gbyscFnKey;
    private FnKey goFnKey;
    private FnKey gocFnKey;
    private FnKey gfdFnKey;
    private FnKey gfsFnKey;
    private FnKey gftFnKey;
    private FnKey gfcoFnKey;
    private FnKey ruFnKey;
    private FnKey riFnKey;
    private FnKey rdFnKey;
    private FnKey guFnKey;
    private FnKey gurFnKey;
    private FnKey ghFnKey;
    private FnKey gnsFnKey;
    private FnKey gnscFnKey;
    private FnKey abFnKey;
    private FnKey reFnKey;
    private FnKey gmdFnkey;
    private FnKey gcFnkey;
    private FnKey gcclFnkey;
    private FnKey gbclFnKey;
    private FnKey gbciFnkey;

    public static class Builder {
        private final Config config;

        private ResultSet resultSet = null;
        private MDStatement mdStatement = null;
        private MDDatabaseMetaData metaData = null;
        private int resultSetInstanceId = System.identityHashCode(this);
        private String query = null;
        private int rowIndex = 0;
        private int columnIndex = 0;
        private String columnLabel = "";

        public Builder(Config config) {
            this.config = config;
        }

        public Builder resultSet(ResultSet val) {
            resultSet = val;
            return this;
        }

        public Builder mdStatement(MDStatement val) {
            mdStatement = val;
            return this;
        }

        public Builder metaData(MDDatabaseMetaData val) {
            metaData = val;
            return this;
        }

        public Builder resultSetInstanceId(int val) {
            resultSetInstanceId = val;
            return this;
        }

        public Builder query(String val) {
            query = val;
            return this;
        }

        public Builder rowIndex(int val) {
            rowIndex = val;
            return this;
        }

        public Builder columnIndex(int val) {
            columnIndex = val;
            return this;
        }

        public Builder columnLabel(String val) {
            columnLabel = val;
            return this;
        }

        public MDResultSet build() {
            return new MDResultSet(this);
        }
    }

    private MDResultSet(Builder builder) {
        config = builder.config;
        resultSet = builder.resultSet;
        mdStatement = builder.mdStatement;
        metaData = builder.metaData;
        resultSetInstanceId = builder.resultSetInstanceId;
        query = builder.query;
        rowIndex = builder.rowIndex;
        columnIndex = builder.columnIndex;
        columnLabel = builder.columnLabel;
    }

    public int getResultSetInstanceId() {
        return resultSetInstanceId;
    }

    @Override
    public boolean next() throws SQLException {
        if (null == nxtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nxtFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, nxtFnKey, (fnArgs) -> resultSet.next(),
                this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn ? this.rowIndex+1 : 0;
        this.columnIndex = 0;

        return toReturn;
    }

    @Override
    public void close() throws SQLException {
        this.rowIndex = 0;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.close();
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        if (null == wnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            wnFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, wnFnKey, (fnArgs) -> resultSet.wasNull(),
                this.resultSetInstanceId, this.rowIndex, this.columnIndex, this.columnLabel);
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (String) Utils.recordOrMock(config, gsFnKey,
                (fnArgs) -> resultSet.getString(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        if (null == gbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (boolean) Utils.recordOrMock(config, gbFnKey,
                (fnArgs) -> resultSet.getBoolean(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (byte) Utils.recordOrMock(config, gbyFnKey,
                (fnArgs) -> resultSet.getByte(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        if (null == gshcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (short) Utils.recordOrMock(config, gshcFnKey,
                (fnArgs) -> resultSet.getShort(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        if (null == gicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gicFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (int) Utils.recordOrMock(config, gicFnKey,
                (fnArgs) -> resultSet.getInt(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        if (null == glcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (long) Utils.recordOrMock(config, glcFnKey,
                (fnArgs) -> resultSet.getLong(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        if (null == gfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (float) Utils.recordOrMock(config, gfFnKey,
                (fnArgs) -> resultSet.getFloat(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        if (null == gdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Double) Utils.recordOrMock(config, gdFnKey,
                (fnArgs) -> resultSet.getDouble(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        if (null == gbdcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcsFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdcsFnKey,
                (fnArgs) -> resultSet.getBigDecimal(columnIndex, scale),
                columnIndex, scale, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        if (null == gbysFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbysFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (byte[]) Utils.recordOrMock(config, gbysFnKey,
                (fnArgs) -> resultSet.getBytes(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        if (null == gdciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdciFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Date) Utils.recordOrMock(config, gdciFnKey,
                (fnArgs) -> resultSet.getDate(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Time) Utils.recordOrMock(config, gtiFnKey,
                (fnArgs) -> resultSet.getTime(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        if (null == gtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Timestamp) Utils.recordOrMock(config, gtFnKey,
                (fnArgs) -> resultSet.getTimestamp(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getAsciiStream(columnIndex);
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getUnicodeStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getBinaryStream(columnIndex);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (String) Utils.recordOrMock(config, gscFnKey,
                (fnArgs) -> resultSet.getString(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        if (null == gbcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (boolean) Utils.recordOrMock(config, gbcFnKey,
                (fnArgs) -> resultSet.getBoolean(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (byte) Utils.recordOrMock(config, gbyFnKey,
                (fnArgs) -> resultSet.getByte(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        if (null == gshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (short) Utils.recordOrMock(config, gshFnKey,
                (fnArgs) -> resultSet.getShort(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        if (null == giFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (int) Utils.recordOrMock(config, giFnKey,
                (fnArgs) -> resultSet.getInt(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        if (null == glclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glclFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (long) Utils.recordOrMock(config, glclFnKey,
                (fnArgs) -> resultSet.getLong(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        if (null == gfcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (float) Utils.recordOrMock(config, gfcFnKey,
                (fnArgs) -> resultSet.getFloat(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        if (null == gdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (double) Utils.recordOrMock(config, gdcFnKey,
                (fnArgs) -> resultSet.getDouble(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        if (null == gbdclsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdclsFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (BigDecimal) Utils.recordOrMock(config, gbdclsFnKey,
                (fnArgs) -> resultSet.getBigDecimal(columnLabel, scale), columnLabel, scale, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        if (null == gbyscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (byte[]) Utils.recordOrMock(config, gbyscFnKey,
                (fnArgs) -> resultSet.getBytes(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        if (null == gdclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Date) Utils.recordOrMock(config, gdclFnKey,
                (fnArgs) -> resultSet.getDate(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        if (null == gticlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Time) Utils.recordOrMock(config, gticlFnKey,
                (fnArgs) -> resultSet.getTime(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        if (null == gtclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtclFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Timestamp) Utils.recordOrMock(config, gtclFnKey,
                (fnArgs) -> resultSet.getTimestamp(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getAsciiStream(columnLabel);
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getUnicodeStream(columnLabel);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getBinaryStream(columnLabel);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (null == gwFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gwFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (SQLWarning) Utils.recordOrMock(config, gwFnKey, (fnArgs) -> resultSet.getWarnings(), this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.clearWarnings();
        }
    }

    @Override
    public String getCursorName() throws SQLException {
        if (null == gcnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcnFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gtclFnKey, (fnArgs) -> resultSet.getCursorName(), this.resultSetInstanceId);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        if (null == gmdFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmdFnkey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, gmdFnkey, Optional.of(integerType), this.resultSetInstanceId);
            MDResultSetMetaData mockMetadata = new MDResultSetMetaData(this, config, (int) retVal);
            return mockMetadata;
        }

        MDResultSetMetaData metaData = new MDResultSetMetaData(resultSet.getMetaData(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(metaData.getResultSetMetaDataInstanceId(), config, gmdFnkey, this.resultSetInstanceId);
        }

        return metaData;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        if (null == goFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            goFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return Utils.recordOrMock(config, goFnKey,
                (fnArgs) -> resultSet.getObject(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        if (null == gocFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gocFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return Utils.recordOrMock(config, gocFnKey,
                (fnArgs) -> resultSet.getObject(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (null == fcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            fcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int)Utils.recordOrMock(config, fcFnKey,
                (fnArgs) -> resultSet.findColumn(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getCharacterStream(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        if (null == gbdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdFnKey,
                (fnArgs) -> resultSet.getBigDecimal(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        if (null == gbdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (BigDecimal) Utils.recordOrMock(config, gbdcFnKey,
                (fnArgs) -> resultSet.getBigDecimal(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        if (null == ibfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ibfFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ibfFnKey, (fnArgs) -> resultSet.isBeforeFirst(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        if (null == ialFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ialFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ialFnKey, (fnArgs) -> resultSet.isAfterLast(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isFirst() throws SQLException {
        if (null == ifFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ifFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ifFnKey, (fnArgs) -> resultSet.isFirst(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isLast() throws SQLException {
        if (null == ilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ilFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ilFnKey, (fnArgs) -> resultSet.isLast(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void beforeFirst() throws SQLException {
        this.rowIndex = 0;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.beforeFirst();
        }
    }

    @Override
    public void afterLast() throws SQLException {
        this.rowIndex = 0;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.afterLast();
        }
    }

    @Override
    public boolean first() throws SQLException {
        if (null == fFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            fFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, fFnKey, (fnArgs) -> resultSet.first(),
                this.resultSetInstanceId);
        this.rowIndex = toReturn ? 1 : 0;

        return toReturn;
    }

    @Override
    public boolean last() throws SQLException {
        if (null == lFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            lFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.rowIndex = (int) Utils.recordOrMock(config, lFnKey, (fnArgs) -> resultSet.last() ? resultSet.getRow() : 0,
                this.resultSetInstanceId);
        return this.rowIndex == 0 ? false : true;
    }

    @Override
    public int getRow() throws SQLException {
        if (null == grFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grFnKey, (fnArgs) -> resultSet.getRow(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (null == abFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            abFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, abFnKey,
                (fnArgs) -> resultSet.absolute(row), row, this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn && row >= 0 ? row : getRow();

        return toReturn;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        if (null == reFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            reFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, reFnKey,
                (fnArgs) -> resultSet.relative(rows), rows, this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn && rows >= 0 ? rows : getRow();

        return toReturn;
    }

    @Override
    public boolean previous() throws SQLException {
        if (null == prevFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            prevFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, prevFnKey, (fnArgs) -> resultSet.previous(),
                this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn ? this.rowIndex-- : 0;
        this.columnIndex = 0; //reset column index

        return toReturn;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.setFetchDirection(direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        if (null == gfdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfdFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfdFnKey, (fnArgs) -> resultSet.getFetchDirection(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        if (null == gfsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfsFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfsFnKey, (fnArgs) -> resultSet.getFetchSize(),
                this.resultSetInstanceId);
    }

    @Override
    public int getType() throws SQLException {
        if (null == gftFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gftFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gftFnKey, (fnArgs) -> resultSet.getType(),
                this.resultSetInstanceId);
    }

    @Override
    public int getConcurrency() throws SQLException {
        if (null == gfcoFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcoFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfcoFnKey, (fnArgs) -> resultSet.getConcurrency(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        if (null == ruFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ruFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ruFnKey, (fnArgs) -> resultSet.rowUpdated(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowInserted() throws SQLException {
        if (null == riFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            riFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, riFnKey, (fnArgs) -> resultSet.rowInserted(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        if (null == rdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            rdFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, rdFnKey, (fnArgs) -> resultSet.rowDeleted(),
                this.resultSetInstanceId);
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNull(columnIndex);
        }
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBoolean(columnIndex, x);
        }
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateByte(columnIndex, x);
        }
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateShort(columnIndex, x);
        }
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateInt(columnIndex, x);
        }
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateLong(columnIndex, x);
        }
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateFloat(columnIndex, x);
        }
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDouble(columnIndex, x);
        }
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBigDecimal(columnIndex, x);
        }
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateString(columnIndex, x);
        }
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBytes(columnIndex, x);
        }
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDate(columnIndex, x);
        }
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTime(columnIndex, x);
        }
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTimestamp(columnIndex, x);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnIndex, x, scaleOrLength);
        }
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnIndex, x);
        }
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNull(columnLabel);
        }
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBoolean(columnLabel, x);
        }
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateByte(columnLabel, x);
        }
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateShort(columnLabel, x);
        }
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateInt(columnLabel, x);
        }
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateLong(columnLabel, x);
        }
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateFloat(columnLabel, x);
        }
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDouble(columnLabel, x);
        }
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBigDecimal(columnLabel, x);
        }
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateString(columnLabel, x);
        }
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBytes(columnLabel, x);
        }
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDate(columnLabel, x);
        }
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTime(columnLabel, x);
        }
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTimestamp(columnLabel, x);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnLabel, x, scaleOrLength);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnLabel, x);
        }
    }

    @Override
    public void insertRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.insertRow();
        }
    }

    @Override
    public void updateRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRow();
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.deleteRow();
        }
    }

    @Override
    public void refreshRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.refreshRow();
        }
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.cancelRowUpdates();
        }
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.moveToInsertRow();
        }
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.moveToCurrentRow();
        }
    }

    @Override
    public Statement getStatement() throws SQLException {
        return this.mdStatement;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getObject(columnIndex, map);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        if (null == gbciFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbciFnkey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Blob) Utils.recordOrMock(config, gbciFnkey,
            (fnArgs) -> new SerialBlob(resultSet.getBlob(columnIndex)), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        if (null == gcFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcFnkey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Clob) Utils.recordOrMock(config, gcFnkey,
            (fnArgs) -> new SerialClob(resultSet.getClob(columnIndex)), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getObject(columnLabel, map);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        if (null == gbclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbclFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Blob) Utils.recordOrMock(config, gbclFnKey,
            (fnArgs) -> new SerialBlob(resultSet.getBlob(columnLabel)), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        if (null == gcclFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcclFnkey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Clob) Utils.recordOrMock(config, gcclFnkey,
            (fnArgs) -> new SerialClob(resultSet.getClob(columnLabel)), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        if (null == gdcicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcicFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Date) Utils.recordOrMock(config, gdcicFnKey,
                (fnArgs) -> resultSet.getDate(columnIndex, cal), columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        if (null == gdclcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Date) Utils.recordOrMock(config, gdclcFnKey,
                (fnArgs) -> resultSet.getDate(columnLabel, cal), columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        if (null == gticFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Time) Utils.recordOrMock(config, gticFnKey,
                (fnArgs) -> resultSet.getTime(columnIndex, cal), columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        if (null == gticlcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Time) Utils.recordOrMock(config, gticlcFnKey,
                (fnArgs) -> resultSet.getTime(columnLabel, cal), columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        if (null == gtcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Timestamp) Utils.recordOrMock(config, gtcFnKey,
                (fnArgs) -> resultSet.getTimestamp(columnIndex, cal), columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        if (null == gtccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtccFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (Timestamp) Utils.recordOrMock(config, gtccFnKey,
                (fnArgs) -> resultSet.getTimestamp(columnLabel, cal), columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        if (null == guFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            guFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (URL) Utils.recordOrMock(config, guFnKey,
                (fnArgs) -> resultSet.getURL(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        if (null == gurFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gurFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (URL) Utils.recordOrMock(config, gurFnKey,
                (fnArgs) -> resultSet.getURL(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRef(columnIndex, x);
        }
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRef(columnLabel, x);
        }
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, x);
        }
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, x);
        }
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, x);
        }
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, x);
        }
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateArray(columnIndex, x);
        }
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateArray(columnLabel, x);
        }
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getRowId(columnLabel);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRowId(columnIndex, x);
        }
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRowId(columnLabel, x);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        if (null == ghFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ghFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, ghFnKey, (fnArgs) -> resultSet.getHoldability(), this.resultSetInstanceId);
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icFnKey, (fnArgs) -> resultSet.isClosed(), this.resultSetInstanceId);
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNString(columnIndex, nString);
        }
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNString(columnLabel, nString);
        }
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, nClob);
        }
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, nClob);
        }
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getSQLXML(columnLabel);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateSQLXML(columnIndex, xmlObject);
        }
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateSQLXML(columnLabel, xmlObject);
        }
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        if (null == gnsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnsFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (String) Utils.recordOrMock(config, gnsFnKey,
                (fnArgs) -> resultSet.getNString(columnIndex), columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        if (null == gnscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        this.columnLabel = columnLabel;
        return (String) Utils.recordOrMock(config, gnscFnKey,
                (fnArgs) -> resultSet.getNString(columnLabel), columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getNCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, inputStream, length);
        }
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, inputStream, length);
        }
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, reader, length);
        }
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, reader, length);
        }
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, reader, length);
        }
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, reader, length);
        }
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnIndex, x);
        }
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnLabel, reader);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader);
        }
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, inputStream);
        }
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, inputStream);
        }
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, reader);
        }
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, reader);
        }
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, reader);
        }
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, reader);
        }
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getObject(columnIndex, type);
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.getObject(columnLabel, type);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSet.isWrapperFor(iface);
    }
}
