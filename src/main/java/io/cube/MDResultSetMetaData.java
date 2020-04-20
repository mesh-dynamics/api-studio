package io.cube;

import java.lang.reflect.Method;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import io.md.utils.FnKey;

public class MDResultSetMetaData implements ResultSetMetaData {
    private final ResultSetMetaData resultSetMetaData;
    private final MDResultSet mdResultSet;
    private final Config config;
    private final int resultSetMetaDataInstanceId;
    private final MDPreparedStatement mdPreparedStatement;
    private FnKey gccFnKey;
    private FnKey iaicFnKey;
    private FnKey icscFnKey;
    private FnKey iscFnKey;
    private FnKey iccFnKey;
    private FnKey incFnKey;
    private FnKey gcdscFnKey;
    private FnKey gclcFnKey;
    private FnKey gcncFnKey;
    private FnKey gsncFnKey;
    private FnKey gpcFnKey;
    private FnKey gscFnKey;
    private FnKey gtncFnKey;
    private FnKey gcancFnKey;
    private FnKey gctcFnKey;
    private FnKey gctnFnKey;
    private FnKey gccncFnKey;
    private FnKey ircFnKey;
    private FnKey iwcFnKey;
    private FnKey idwcFnKey;
    private FnKey isicFnKey;

    public MDResultSetMetaData(MDResultSet mdResultSet, Config config, int resultSetMetaDataInstanceId) {
        this.resultSetMetaData = null;
        this.mdPreparedStatement = null;
        this.mdResultSet = mdResultSet;
        this.config = config;
        this.resultSetMetaDataInstanceId = resultSetMetaDataInstanceId;
    }

    public MDResultSetMetaData(ResultSetMetaData resultSetMetaData, MDResultSet mdResultSet, Config config) {
        this.resultSetMetaData = resultSetMetaData;
        this.mdResultSet = mdResultSet;
        this.mdPreparedStatement = null;
        this.config = config;
        this.resultSetMetaDataInstanceId = System.identityHashCode(this);
    }

    public MDResultSetMetaData(MDPreparedStatement mdPreparedStatement, Config config, int resultSetMetaDataInstanceId) {
        this.resultSetMetaData = null;
        this.mdResultSet = null;
        this.mdPreparedStatement = mdPreparedStatement;
        this.config = config;
        this.resultSetMetaDataInstanceId = resultSetMetaDataInstanceId;
    }

    public MDResultSetMetaData(ResultSetMetaData resultSetMetaData, MDPreparedStatement mdPreparedStatement, Config config) {
        this.resultSetMetaData = resultSetMetaData;
        this.mdResultSet = null;
        this.mdPreparedStatement = mdPreparedStatement;
        this.config = config;
        this.resultSetMetaDataInstanceId = System.identityHashCode(this);
    }

    public int getResultSetMetaDataInstanceId() {
        return resultSetMetaDataInstanceId;
    }

    @Override
    public int getColumnCount() throws SQLException {
        if (null == gccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gccFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gccFnKey,
                (fnArgs) -> resultSetMetaData.getColumnCount(), this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        if (null == iaicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iaicFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iaicFnKey,
                (fnArgs) -> resultSetMetaData.isAutoIncrement(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        if (null == icscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icscFnKey,
                (fnArgs) -> resultSetMetaData.isCaseSensitive(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        if (null == iscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iscFnKey,
                (fnArgs) -> resultSetMetaData.isSearchable(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        if (null == iccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iccFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iccFnKey,
                (fnArgs) -> resultSetMetaData.isCurrency(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public int isNullable(int column) throws SQLException {
        if (null == incFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            incFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, incFnKey,
                (fnArgs) -> resultSetMetaData.isNullable(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        if (null == isicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            isicFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, isicFnKey,
                (fnArgs) -> resultSetMetaData.isSigned(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        if (null == gcdscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcdscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gcdscFnKey,
                (fnArgs) -> resultSetMetaData.getColumnDisplaySize(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        if (null == gclcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gclcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gclcFnKey,
                (fnArgs) -> resultSetMetaData.getColumnLabel(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        if (null == gcncFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcncFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gcncFnKey,
                (fnArgs) -> resultSetMetaData.getColumnName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        if (null == gsncFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsncFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsncFnKey,
                (fnArgs) -> resultSetMetaData.getSchemaName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        if (null == gpcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gpcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gpcFnKey,
                (fnArgs) -> resultSetMetaData.getPrecision(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public int getScale(int column) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gscFnKey,
                (fnArgs) -> resultSetMetaData.getScale(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getTableName(int column) throws SQLException {
        if (null == gtncFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtncFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gtncFnKey,
                (fnArgs) -> resultSetMetaData.getTableName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        if (null == gcancFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcancFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gcancFnKey,
                (fnArgs) -> resultSetMetaData.getCatalogName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        if (null == gctcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gctcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gctcFnKey,
                (fnArgs) -> resultSetMetaData.getColumnType(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        if (null == gctnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gctnFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gctnFnKey,
                (fnArgs) -> resultSetMetaData.getColumnTypeName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        if (null == ircFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ircFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ircFnKey,
                (fnArgs) -> resultSetMetaData.isReadOnly(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        if (null == iwcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iwcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iwcFnKey,
                (fnArgs) -> resultSetMetaData.isWritable(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        if (null == idwcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            idwcFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, idwcFnKey,
                (fnArgs) -> resultSetMetaData.isDefinitelyWritable(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        if (null == gccncFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gccncFnKey = new FnKey(Config.commonConfig.customerId, Config.commonConfig.app, Config.commonConfig.instance,
                    Config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gccncFnKey,
                (fnArgs) -> resultSetMetaData.getColumnClassName(column), column, this.resultSetMetaDataInstanceId);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSetMetaData.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return resultSetMetaData.isWrapperFor(iface);
    }
}
