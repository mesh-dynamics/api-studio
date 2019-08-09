package io.cube;

import com.google.gson.reflect.TypeToken;
import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.Optional;

public class CubeDatabaseMetaData implements DatabaseMetaData {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeDatabaseMetaData.class);
    private static Type type = new TypeToken<Integer>() {}.getType();
    private final DatabaseMetaData metaData;
    private final CubeConnection cubeConnection;
    private final Config config;
    private final int metadataInstanceId;
    private FnKey gdpFnKey;
    private FnKey guFnKey;
    private FnKey gunFnKey;
    private FnKey gdpvFnKey;
    private FnKey gdnFnKey;
    private FnKey gdvFnKey;
    private FnKey giqsFnKey;
    private FnKey gskFnKey;
    private FnKey gstFnKey;
    private FnKey gsfFnKey;
    private FnKey gsyfFnKey;
    private FnKey gsseFnKey;
    private FnKey gptFnKey;
    private FnKey gctFnKey;
    private FnKey gnfFnKey;
    private FnKey gtdfFnKey;
    private FnKey gencFnKey;
    private FnKey gcsFnKey;
    private FnKey srstFnKey;
    private FnKey sbuFnKey;
    private FnKey sggkFnKey;
    private FnKey snpFnKey;
    private FnKey ddiitFnKey;
    private FnKey ddctcFnKey;
    private FnKey gsstFnKey;
    private FnKey lucFnKey;
    private FnKey gtiFnKey;
    private FnKey scitdFnKey;
    private FnKey ssitdFnKey;
    private FnKey slciFnKey;
    private FnKey sluiFnKey;
    private FnKey smciFnKey;
    private FnKey slcqiFnKey;
    private FnKey sucqiFnKey;
    private FnKey smcqiFnKey;
    private FnKey icasFnKey;
    private FnKey ssidmFnKey;
    private FnKey ssipcFnKey;

    public CubeDatabaseMetaData (Config config, int metadataInstanceId) {
        this.metaData = null;
        this.cubeConnection = null;
        this.config = config;
        this.metadataInstanceId = metadataInstanceId;
    }

    public CubeDatabaseMetaData (DatabaseMetaData metaData, CubeConnection cubeConnection, Config config) {
        this.metaData = metaData;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.metadataInstanceId = System.identityHashCode(this);
    }

    public int getMetadataInstanceId() {
        return metadataInstanceId;
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return false;
    }

    @Override
    public String getURL() throws SQLException {
        if (null == guFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            guFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, guFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getURL();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    guFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getUserName() throws SQLException {
        if (null == gunFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gunFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gunFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getUserName();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gunFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        if (null == gdpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gdpFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getDatabaseProductName();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gdpFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        if (null == gdpvFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdpvFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gdpvFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getDatabaseProductVersion();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gdpvFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getDriverName() throws SQLException {
        if (null == gdnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gdnFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getDriverName();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gdnFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getDriverVersion() throws SQLException {
        if (null == gdvFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdvFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gdvFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getDriverVersion();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gdvFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public int getDriverMajorVersion() {
        return 0;
    }

    @Override
    public int getDriverMinorVersion() {
        return 0;
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        if (null == sluiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sluiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, sluiFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesUpperCaseIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, sluiFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        if (null == slciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, slciFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesLowerCaseIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, slciFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        if (null == smciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, smciFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesMixedCaseIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, smciFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        if (null == smcqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smcqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, smcqiFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesMixedCaseQuotedIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, smcqiFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        if (null == sucqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sucqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, sucqiFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesUpperCaseQuotedIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, sucqiFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        if (null == slcqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slcqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, slcqiFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.storesLowerCaseQuotedIdentifiers();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, slcqiFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        if (null == giqsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giqsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, giqsFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getIdentifierQuoteString();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    giqsFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        if (null == gskFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gskFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gskFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getSQLKeywords();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gskFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        if (null == gnfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gnfFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getNumericFunctions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gnfFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getStringFunctions() throws SQLException {
        if (null == gsfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gsfFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getStringFunctions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gsfFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        if (null == gsyfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsyfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gsyfFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getSystemFunctions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gsyfFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        if (null == gtdfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtdfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gdpFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getTimeDateFunctions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gtdfFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        if (null == gsseFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsseFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gsseFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getSearchStringEscape();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gsseFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        if (null == gencFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gencFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gencFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getExtraNameCharacters();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gencFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        return false;
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        if (null == gstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gstFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getSchemaTerm();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gstFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        if (null == gptFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gptFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gptFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getProcedureTerm();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gptFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        if (null == gctFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gctFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gctFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getCatalogTerm();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gctFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        if (null == icasFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icasFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, icasFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.isCatalogAtStart();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, icasFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        if (null == gcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gcsFnKey, false,
                    this.metadataInstanceId);
        }

        String toReturn = metaData.getCatalogSeparator();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gcsFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        if (null == ssidmFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssidmFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ssidmFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsSchemasInDataManipulation();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ssidmFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        if (null == ssipcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssipcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ssipcFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsSchemasInProcedureCalls();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ssipcFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        if (null == ssitdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssitdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ssitdFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsSchemasInTableDefinitions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ssitdFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        if (null == scitdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scitdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, scitdFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsCatalogsInTableDefinitions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, scitdFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return false;
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxConnections() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        return 0;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return false;
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxStatements() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        if (null == ddctcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ddctcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ddctcFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.dataDefinitionCausesTransactionCommit();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ddctcFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        if (null == ddiitFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ddiitFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ddiitFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.dataDefinitionIgnoredInTransactions();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ddiitFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return null;
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        return null;
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        return null;
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gtiFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.of(type),
                    this.metadataInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeResultSet mockResultSet = new CubeResultSet(config, (int)ret.retVal);
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet(metaData.getTypeInfo(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gtiFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeResultSet.getResultSetInstanceId(), FnReqResponse.RetStatus.Success,
                    Optional.empty(), this.metadataInstanceId);
        }

        return cubeResultSet;
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return null;
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        if (null == srstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            srstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, srstFnKey, false, this.metadataInstanceId, type);
        }

        boolean toReturn = metaData.supportsResultSetType(type);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, srstFnKey, true, this.metadataInstanceId, type);
        }

        return toReturn;
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        if (null == sbuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sbuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, sbuFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsBatchUpdates();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, sbuFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return metaData.getConnection();
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        if (null == snpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            snpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, snpFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsNamedParameters();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, snpFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        if (null == sggkFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sggkFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, sggkFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.supportsGetGeneratedKeys();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, sggkFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        return null;
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getSQLStateType() throws SQLException {
        if (null == gsstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gsstFnKey, false, this.metadataInstanceId);
        }

        int toReturn = metaData.getSQLStateType();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gsstFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        if (null == lucFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            lucFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, lucFnKey, false, this.metadataInstanceId);
        }

        boolean toReturn = metaData.locatorsUpdateCopy();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, lucFnKey, true, this.metadataInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return false;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return null;
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return null;
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return false;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        return null;
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return null;
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
