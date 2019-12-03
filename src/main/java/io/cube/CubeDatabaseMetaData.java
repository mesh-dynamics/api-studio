package io.cube;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import io.cube.agent.FnKey;

public class CubeDatabaseMetaData implements DatabaseMetaData {
    private final DatabaseMetaData metaData;
    private final CubeConnection cubeConnection;
    private final Config config;
    private final int metadataInstanceId;
    private FnKey iroFnKey;
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
    private FnKey apacFnKey;
    private FnKey atasFnKey;
    private FnKey nashFnKey;
    private FnKey naslFnKey;
    private FnKey nasasFnKey;
    private FnKey nasaeFnKey;
    private FnKey ulfFnKey;
    private FnKey ulfptFnKey;
    private FnKey sumciFnKey;
    private FnKey stmcqiFnKey;
    private FnKey satwacFnKey;
    private FnKey satwdcFnKey;
    private FnKey scaFnKey;
    private FnKey npnninFnKey;
    private FnKey scFnKey;
    private FnKey stcnFnKey;
    private FnKey sdtcnFnKey;
    private FnKey seiobFnKey;
    private FnKey sobuFnKey;
    private FnKey sgbFnKey;
    private FnKey sgbuFnKey;
    private FnKey sgbbsFnKey;
    private FnKey slecFnKey;
    private FnKey smrsFnKey;
    private FnKey smtFnKey;
    private FnKey snncFnKey;
    private FnKey smsgFnKey;
    private FnKey scsgFnKey;
    private FnKey sesgFnKey;
    private FnKey saelsFnKey;
    private FnKey saisFnKey;
    private FnKey safsFnKey;
    private FnKey siefFnKey;
    private FnKey sojFnKey;
    private FnKey sfojFnKey;
    private FnKey slojFnKey;
    private FnKey ssiidFnKey;
    private FnKey ssipdFnKey;
    private FnKey scidmFnKey;
    private FnKey scipcFnKey;
    private FnKey sciidFnKey;
    private FnKey scipdFnKey;
    private FnKey spdFnKey;
    private FnKey spuFnKey;
    private FnKey ssfuFnKey;
    private FnKey sspFnKey;
    private FnKey ssicFnKey;
    private FnKey ssieFnKey;
    private FnKey ssiiFnKey;
    private FnKey ssiqFnKey;
    private FnKey scsFnKey;
    private FnKey suFnKey;
    private FnKey suaFnKey;
    private FnKey socacFnKey;
    private FnKey socarFnKey;
    private FnKey sosacFnKey;
    private FnKey sosarFnKey;
    private FnKey dmrsibFnKey;
    private FnKey stFnKey;
    private FnKey stilFnKey;
    private FnKey sddadmtFnKey;
    private FnKey sdmtoFnKey;
    private FnKey ssfucsFnKey;
    private FnKey smorFnKey;
    private FnKey acfcarsFnKey;
    private FnKey gkarFnKey;
    private FnKey gmbllFnKey;
    private FnKey gmcllFnKey;
    private FnKey gmconlFnKey;
    private FnKey gmcigbFnKey;
    private FnKey gmciiFnKey;
    private FnKey gmciobFnKey;
    private FnKey gmcisFnKey;
    private FnKey gmcitFnKey;
    private FnKey gmcFnKey;
    private FnKey gmcunlFnKey;
    private FnKey gmilFnKey;
    private FnKey gmsnlFnKey;
    private FnKey gmpnlFnKey;
    private FnKey gmcnlFnKey;
    private FnKey gmrsFnKey;
    private FnKey gmslFnKey;
    private FnKey gmsFnKey;
    private FnKey gmtnlFnKey;
    private FnKey gmtisFnKey;
    private FnKey gmunlFnKey;
    private FnKey gdtiFnKey;
    private FnKey grshFnKey;
    private FnKey gdamavFnKey;
    private FnKey gdamivFnKey;
    private FnKey gjmavFnKey;
    private FnKey gjmivFnKey;
    private FnKey gdmavFnKey;
    private FnKey gdmivFnKey;
    private FnKey srscFnKey;
    private FnKey ouavFnKey;
    private FnKey odavFnKey;
    private FnKey oiavFnKey;
    private FnKey otiavFnKey;
    private FnKey otuavFnKey;
    private FnKey uadFnKey;
    private FnKey dadFnKey;
    private FnKey iadFnKey;
    private FnKey srshFnKey;
    private FnKey giiFnKey;
    private FnKey gpFnKey;
    private FnKey gpcFnKey;
    private FnKey gtFnKey;
    private FnKey gsFnKey;
    private FnKey gcFnKey;
    private FnKey gttFnKey;
    private FnKey gcpFnKey;
    private FnKey gcoFnKey;
    private FnKey gtpFnKey;
    private FnKey gbriFnKey;
    private FnKey gvcFnKey;
    private FnKey gpkFnKey;
    private FnKey gikFnKey;
    private FnKey gekFnKey;
    private FnKey gcrFnKey;
    private FnKey gpcoFnKey;
    private FnKey sucFnKey;
    private FnKey gudFnKey;
    private FnKey gsutyFnKey;
    private FnKey gsutaFnKey;
    private FnKey gaFnKey;
    private FnKey gcipFnKey;
    private FnKey gfFnKey;
    private FnKey gfcFnKey;
    private FnKey gscFnKey;
    private FnKey otdavFnKey;
    private FnKey grilFnKey;

    public CubeDatabaseMetaData (CubeConnection cubeConnection, Config config, int metadataInstanceId) {
        this.metaData = null;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.metadataInstanceId = metadataInstanceId;
    }

    public CubeDatabaseMetaData (DatabaseMetaData metaData, CubeConnection cubeConnection, Config config) {
        this.metaData = metaData;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.metadataInstanceId = cubeConnection.getUrl().hashCode();
    }

    public int getMetadataInstanceId() {
        return metadataInstanceId;
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        if (null == apacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            apacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, apacFnKey, (fnArgs) -> metaData.allProceduresAreCallable(), this.metadataInstanceId);
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        if (null == atasFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            atasFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, atasFnKey, (fnArgs) -> metaData.allTablesAreSelectable(), this.metadataInstanceId);
    }

    @Override
    public String getURL() throws SQLException {
        if (null == guFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            guFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, guFnKey, (fnArgs) -> metaData.getURL(), this.metadataInstanceId);
    }

    @Override
    public String getUserName() throws SQLException {
        if (null == gunFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gunFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gunFnKey, (fnArgs) -> metaData.getUserName(), this.metadataInstanceId);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (null == iroFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iroFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iroFnKey, (fnArgs) -> metaData.isReadOnly(),
                this.metadataInstanceId);
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        if (null == nashFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nashFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, nashFnKey, (fnArgs) -> metaData.nullsAreSortedHigh(),
                this.metadataInstanceId);
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        if (null == naslFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            naslFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, naslFnKey, (fnArgs) -> metaData.nullsAreSortedLow(),
                this.metadataInstanceId);
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        if (null == nasasFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nasasFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, nasasFnKey, (fnArgs) -> metaData.nullsAreSortedAtStart(),
                this.metadataInstanceId);
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        if (null == nasaeFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nasaeFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, nasaeFnKey, (fnArgs) -> metaData.nullsAreSortedAtEnd(),
                this.metadataInstanceId);
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        if (null == gdpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gdpFnKey, (fnArgs) -> metaData.getDatabaseProductName(), this.metadataInstanceId);
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        if (null == gdpvFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdpvFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gdpvFnKey, (fnArgs) -> metaData.getDatabaseProductVersion(), this.metadataInstanceId);
    }

    @Override
    public String getDriverName() throws SQLException {
        if (null == gdnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gdnFnKey, (fnArgs) -> metaData.getDriverName(), this.metadataInstanceId);
    }

    @Override
    public String getDriverVersion() throws SQLException {
        if (null == gdvFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdvFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gdvFnKey, (fnArgs) -> metaData.getDriverVersion(), this.metadataInstanceId);
    }

    @Override
    public int getDriverMajorVersion() {
        if (null == gdmavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdmavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        try {
            return (int) Utils.recordOrMock(config, gdmavFnKey, (fnArgs) -> metaData.getDriverMajorVersion(), this.metadataInstanceId);
        } catch (SQLException e) {
            //Do Nothing
        }

        return 1;
    }

    @Override
    public int getDriverMinorVersion() {
        if (null == gdmivFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdmivFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        try {
            return (int) Utils.recordOrMock(config, gdmivFnKey, (fnArgs) -> metaData.getDriverMinorVersion(), this.metadataInstanceId);
        } catch (SQLException e) {
            //Do Nothing
        }

        return 0;
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        if (null == ulfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ulfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ulfFnKey, (fnArgs) -> metaData.usesLocalFiles(),
                this.metadataInstanceId);
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        if (null == ulfptFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ulfptFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ulfptFnKey, (fnArgs) -> metaData.usesLocalFilePerTable(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        if (null == sumciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sumciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sumciFnKey, (fnArgs) -> metaData.supportsMixedCaseIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        if (null == sluiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sluiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sluiFnKey, (fnArgs) -> metaData.storesUpperCaseIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        if (null == slciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, slciFnKey, (fnArgs) -> metaData.storesLowerCaseIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        if (null == smciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smciFnKey, (fnArgs) -> metaData.storesMixedCaseIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        if (null == smcqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smcqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smcqiFnKey, (fnArgs) -> metaData.storesMixedCaseQuotedIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        if (null == sucqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sucqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sucqiFnKey, (fnArgs) -> metaData.storesUpperCaseQuotedIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        if (null == slcqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slcqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, slcqiFnKey, (fnArgs) -> metaData.storesLowerCaseQuotedIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        if (null == stmcqiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            stmcqiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, stmcqiFnKey, (fnArgs) -> metaData.storesMixedCaseQuotedIdentifiers(),
                this.metadataInstanceId);
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        if (null == giqsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giqsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, giqsFnKey, (fnArgs) -> metaData.getIdentifierQuoteString(), this.metadataInstanceId);
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        if (null == gskFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gskFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gskFnKey, (fnArgs) -> metaData.getSQLKeywords(), this.metadataInstanceId);
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        if (null == gnfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gnfFnKey, (fnArgs) -> metaData.getNumericFunctions(), this.metadataInstanceId);
    }

    @Override
    public String getStringFunctions() throws SQLException {
        if (null == gsfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsfFnKey, (fnArgs) -> metaData.getStringFunctions(), this.metadataInstanceId);
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        if (null == gsyfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsyfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsyfFnKey, (fnArgs) -> metaData.getSystemFunctions(), this.metadataInstanceId);
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        if (null == gtdfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtdfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gtdfFnKey, (fnArgs) -> metaData.getTimeDateFunctions(), this.metadataInstanceId);
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        if (null == gsseFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsseFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gsseFnKey, (fnArgs) -> metaData.getSearchStringEscape(), this.metadataInstanceId);
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        if (null == gencFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gencFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gencFnKey, (fnArgs) -> metaData.getExtraNameCharacters(), this.metadataInstanceId);
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        if (null == satwacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            satwacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, satwacFnKey, (fnArgs) -> metaData.supportsAlterTableWithAddColumn(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        if (null == satwdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            satwdcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, satwdcFnKey, (fnArgs) -> metaData.supportsAlterTableWithDropColumn(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        if (null == scaFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scaFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scaFnKey, (fnArgs) -> metaData.supportsColumnAliasing(),
                this.metadataInstanceId);
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        if (null == npnninFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            npnninFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, npnninFnKey, (fnArgs) -> metaData.nullPlusNonNullIsNull(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        if (null == scFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scFnKey, (fnArgs) -> metaData.supportsConvert(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        if (null == sucFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sucFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sucFnKey,
                (fnArgs) -> metaData.supportsConvert(fromType, toType), fromType, toType, this.metadataInstanceId);
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        if (null == stcnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            stcnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, stcnFnKey, (fnArgs) -> metaData.supportsTableCorrelationNames(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        if (null == sdtcnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sdtcnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sdtcnFnKey, (fnArgs) -> metaData.supportsDifferentTableCorrelationNames(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        if (null == seiobFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            seiobFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, seiobFnKey, (fnArgs) -> metaData.supportsExpressionsInOrderBy(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        if (null == sobuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sobuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sobuFnKey, (fnArgs) -> metaData.supportsOrderByUnrelated(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        if (null == sgbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sgbFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sgbFnKey, (fnArgs) -> metaData.supportsGroupBy(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        if (null == sgbuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sgbuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sgbuFnKey, (fnArgs) -> metaData.supportsGroupByUnrelated(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        if (null == sgbbsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sgbbsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sgbbsFnKey, (fnArgs) -> metaData.supportsGroupByBeyondSelect(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        if (null == slecFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slecFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, slecFnKey, (fnArgs) -> metaData.supportsLikeEscapeClause(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        if (null == smrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smrsFnKey, (fnArgs) -> metaData.supportsMultipleResultSets(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        if (null == smtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smtFnKey, (fnArgs) -> metaData.supportsMultipleTransactions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        if (null == snncFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            snncFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, snncFnKey, (fnArgs) -> metaData.supportsNonNullableColumns(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        if (null == smsgFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smsgFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smsgFnKey, (fnArgs) -> metaData.supportsMinimumSQLGrammar(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        if (null == scsgFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scsgFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scsgFnKey, (fnArgs) -> metaData.supportsCoreSQLGrammar(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        if (null == sesgFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sesgFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sesgFnKey, (fnArgs) -> metaData.supportsExtendedSQLGrammar(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        if (null == saelsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            saelsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, saelsFnKey, (fnArgs) -> metaData.supportsANSI92EntryLevelSQL(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        if (null == saisFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            saisFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, saisFnKey, (fnArgs) -> metaData.supportsANSI92IntermediateSQL(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        if (null == safsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            safsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, safsFnKey, (fnArgs) -> metaData.supportsANSI92FullSQL(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        if (null == siefFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            siefFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, siefFnKey, (fnArgs) -> metaData.supportsIntegrityEnhancementFacility(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        if (null == sojFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sojFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sojFnKey, (fnArgs) -> metaData.supportsOuterJoins(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        if (null == sfojFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sfojFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sfojFnKey, (fnArgs) -> metaData.supportsFullOuterJoins(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        if (null == slojFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            slojFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, slojFnKey, (fnArgs) -> metaData.supportsLimitedOuterJoins(),
                this.metadataInstanceId);
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        if (null == gstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gstFnKey, (fnArgs) -> metaData.getSchemaTerm(), this.metadataInstanceId);
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        if (null == gptFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gptFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gptFnKey, (fnArgs) -> metaData.getProcedureTerm(), this.metadataInstanceId);
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        if (null == gctFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gctFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gctFnKey, (fnArgs) -> metaData.getCatalogTerm(), this.metadataInstanceId);
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        if (null == icasFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icasFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icasFnKey, (fnArgs) -> metaData.isCatalogAtStart(),
                this.metadataInstanceId);
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        if (null == gcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (String) Utils.recordOrMock(config, gcsFnKey, (fnArgs) -> metaData.getCatalogSeparator(), this.metadataInstanceId);
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        if (null == ssidmFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssidmFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssidmFnKey, (fnArgs) -> metaData.supportsSchemasInDataManipulation(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        if (null == ssipcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssipcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssipcFnKey, (fnArgs) -> metaData.supportsSchemasInProcedureCalls(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        if (null == ssitdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssitdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssitdFnKey, (fnArgs) -> metaData.supportsSchemasInTableDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        if (null == ssiidFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssiidFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssiidFnKey, (fnArgs) -> metaData.supportsSchemasInIndexDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        if (null == ssipdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssipdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssipdFnKey, (fnArgs) -> metaData.supportsSchemasInPrivilegeDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        if (null == scidmFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scidmFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scidmFnKey, (fnArgs) -> metaData.supportsCatalogsInDataManipulation(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        if (null == scipcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scipcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scipcFnKey, (fnArgs) -> metaData.supportsCatalogsInProcedureCalls(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        if (null == scitdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scitdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scitdFnKey, (fnArgs) -> metaData.supportsCatalogsInTableDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        if (null == sciidFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sciidFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sciidFnKey, (fnArgs) -> metaData.supportsCatalogsInIndexDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        if (null == scipdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scipdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scipdFnKey, (fnArgs) -> metaData.supportsCatalogsInPrivilegeDefinitions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        if (null == spdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            spdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, spdFnKey, (fnArgs) -> metaData.supportsPositionedDelete(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        if (null == spuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            spuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, spuFnKey, (fnArgs) -> metaData.supportsPositionedUpdate(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        if (null == ssfuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssfuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssfuFnKey, (fnArgs) -> metaData.supportsSelectForUpdate(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        if (null == sspFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sspFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sspFnKey, (fnArgs) -> metaData.supportsStoredProcedures(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        if (null == ssicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssicFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssicFnKey, (fnArgs) -> metaData.supportsSubqueriesInComparisons(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        if (null == ssieFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssieFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssieFnKey, (fnArgs) -> metaData.supportsSubqueriesInExists(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        if (null == ssiiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssiiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssiiFnKey, (fnArgs) -> metaData.supportsSubqueriesInIns(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        if (null == ssiqFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssiqFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssiqFnKey, (fnArgs) -> metaData.supportsSubqueriesInQuantifieds(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        if (null == scsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            scsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, scsFnKey, (fnArgs) -> metaData.supportsCorrelatedSubqueries(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        if (null == suFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            suFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, suFnKey, (fnArgs) -> metaData.supportsUnion(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        if (null == suaFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            suaFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, suaFnKey, (fnArgs) -> metaData.supportsUnionAll(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        if (null == socacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            socacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, socacFnKey, (fnArgs) -> metaData.supportsOpenCursorsAcrossCommit(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        if (null == socarFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            socarFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, socarFnKey, (fnArgs) -> metaData.supportsOpenCursorsAcrossRollback(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        if (null == sosacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sosacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sosacFnKey, (fnArgs) -> metaData.supportsOpenStatementsAcrossCommit(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        if (null == sosarFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sosarFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sosarFnKey, (fnArgs) -> metaData.supportsOpenStatementsAcrossRollback(),
                this.metadataInstanceId);
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        if (null == gmbllFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmbllFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmbllFnKey, (fnArgs) -> metaData.getMaxBinaryLiteralLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        if (null == gmcllFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcllFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcllFnKey, (fnArgs) -> metaData.getMaxCharLiteralLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        if (null == gmconlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmconlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmconlFnKey, (fnArgs) -> metaData.getMaxColumnNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        if (null == gmcigbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcigbFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcigbFnKey, (fnArgs) -> metaData.getMaxColumnsInGroupBy(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        if (null == gmciiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmciiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmciiFnKey, (fnArgs) -> metaData.getMaxColumnsInIndex(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        if (null == gmciobFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmciobFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmciobFnKey, (fnArgs) -> metaData.getMaxColumnsInOrderBy(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        if (null == gmcisFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcisFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcisFnKey, (fnArgs) -> metaData.getMaxColumnsInSelect(), this.metadataInstanceId);
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        if (null == gmcitFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcitFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcitFnKey, (fnArgs) -> metaData.getMaxColumnsInTable(), this.metadataInstanceId);
    }

    @Override
    public int getMaxConnections() throws SQLException {
        if (null == gmcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcFnKey, (fnArgs) -> metaData.getMaxConnections(), this.metadataInstanceId);
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        if (null == gmcunlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcunlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcunlFnKey, (fnArgs) -> metaData.getMaxCursorNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        if (null == gmilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmilFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmilFnKey, (fnArgs) -> metaData.getMaxIndexLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        if (null == gmsnlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmsnlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmsnlFnKey, (fnArgs) -> metaData.getMaxSchemaNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        if (null == gmpnlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmpnlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmpnlFnKey, (fnArgs) -> metaData.getMaxProcedureNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        if (null == gmcnlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmcnlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmcnlFnKey, (fnArgs) -> metaData.getMaxCatalogNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        if (null == gmrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmrsFnKey, (fnArgs) -> metaData.getMaxRowSize(), this.metadataInstanceId);
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        if (null == dmrsibFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            dmrsibFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, dmrsibFnKey, (fnArgs) -> metaData.doesMaxRowSizeIncludeBlobs(),
                this.metadataInstanceId);
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        if (null == gmslFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmslFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmslFnKey, (fnArgs) -> metaData.getMaxStatementLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxStatements() throws SQLException {
        if (null == gmsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmsFnKey, (fnArgs) -> metaData.getMaxStatements(), this.metadataInstanceId);
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        if (null == gmtnlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmtnlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmtnlFnKey, (fnArgs) -> metaData.getMaxTableNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        if (null == gmtisFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmtisFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmtisFnKey, (fnArgs) -> metaData.getMaxTablesInSelect(), this.metadataInstanceId);
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        if (null == gmunlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmunlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmunlFnKey, (fnArgs) -> metaData.getMaxUserNameLength(), this.metadataInstanceId);
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        if (null == gdtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gdtiFnKey, (fnArgs) -> metaData.getDefaultTransactionIsolation(), this.metadataInstanceId);
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        if (null == stFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            stFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, stFnKey, (fnArgs) -> metaData.supportsTransactions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        if (null == stilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            stilFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, stilFnKey,
                (fnArgs) -> metaData.supportsTransactionIsolationLevel(level), level, this.metadataInstanceId);
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        if (null == sddadmtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sddadmtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sddadmtFnKey, (fnArgs) -> metaData.supportsDataDefinitionAndDataManipulationTransactions(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        if (null == sdmtoFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sdmtoFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sdmtoFnKey, (fnArgs) -> metaData.supportsDataManipulationTransactionsOnly(),
                this.metadataInstanceId);
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        if (null == ddctcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ddctcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ddctcFnKey, (fnArgs) -> metaData.dataDefinitionCausesTransactionCommit(),
                this.metadataInstanceId);
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        if (null == ddiitFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ddiitFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ddiitFnKey, (fnArgs) -> metaData.dataDefinitionIgnoredInTransactions(),
                this.metadataInstanceId);
    }

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        if (null == gpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gpFnKey, this,
                (fnArgs) -> metaData.getProcedures(catalog, schemaPattern, procedureNamePattern),
                catalog, schemaPattern, procedureNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        if (null == gpcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gpcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gpcFnKey, this,
                (fnArgs) -> metaData.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern),
                catalog, schemaPattern, procedureNamePattern, columnNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        if (null == gtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gtFnKey, this,
                (fnArgs) -> metaData.getTables(catalog, schemaPattern, tableNamePattern, types),
                catalog, schemaPattern, tableNamePattern, types, this.metadataInstanceId);
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gsFnKey, this, (fnArgs) -> metaData.getSchemas(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        if (null == gcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gcFnKey, this, (fnArgs) -> metaData.getCatalogs(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        if (null == gttFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gttFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gttFnKey, this, (fnArgs) -> metaData.getTableTypes(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        if (null == gcoFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcoFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gcoFnKey, this,
                (fnArgs) -> metaData.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern),
                catalog, schemaPattern, tableNamePattern, columnNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        if (null == gcpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gcpFnKey, this,
                (fnArgs) -> metaData.getColumnPrivileges(catalog, schema, table, columnNamePattern),
                catalog, schema, table, columnNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        if (null == gtpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gtpFnKey, this,
                (fnArgs) -> metaData.getTablePrivileges(catalog, schemaPattern, tableNamePattern),
                catalog, schemaPattern, tableNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        if (null == gbriFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbriFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gbriFnKey, this,
                (fnArgs) -> metaData.getBestRowIdentifier(catalog, schema, table, scope, nullable),
                catalog, schema, table, scope, nullable, this.metadataInstanceId);
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        if (null == gvcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gvcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gvcFnKey, this,
                (fnArgs) -> metaData.getVersionColumns(catalog, schema, table),
                catalog, schema, table, this.metadataInstanceId);
    }

    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        if (null == gpkFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gpkFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gpkFnKey, this,
                (fnArgs) -> metaData.getPrimaryKeys(catalog, schema, table),
                catalog, schema, table, this.metadataInstanceId);
    }

    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        if (null == gikFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gikFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gikFnKey, this,
                (fnArgs) -> metaData.getImportedKeys(catalog, schema, table),
                catalog, schema, table, this.metadataInstanceId);
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        if (null == gekFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gekFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gekFnKey, this,
                (fnArgs) -> metaData.getExportedKeys(catalog, schema, table),
                catalog, schema, table, this.metadataInstanceId);
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        if (null == gcrFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcrFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gcrFnKey, this,
                (fnArgs) -> metaData.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable),
                parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable, this.metadataInstanceId);
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gtiFnKey, this, (fnArgs) -> metaData.getTypeInfo(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        if (null == giiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, giiFnKey, this,
                (fnArgs) -> metaData.getIndexInfo(catalog, schema, table, unique, approximate),
                catalog, schema, table, unique, approximate, this.metadataInstanceId);
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        if (null == srstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            srstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, srstFnKey, (fnArgs) -> metaData.supportsResultSetType(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        if (null == srscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            srscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, srscFnKey,
                (fnArgs) -> metaData.supportsResultSetConcurrency(type, concurrency),
                type, concurrency, this.metadataInstanceId);
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        if (null == ouavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ouavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ouavFnKey, (fnArgs) -> metaData.ownUpdatesAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        if (null == odavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            odavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance, config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, odavFnKey,
                (fnArgs) -> metaData.ownDeletesAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        if (null == oiavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            oiavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, oiavFnKey,
                (fnArgs) -> metaData.ownInsertsAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        if (null == otuavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            otuavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, otuavFnKey,
                (fnArgs) -> metaData.othersUpdatesAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        if (null == otdavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            otdavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, otdavFnKey,
                (fnArgs) -> metaData.othersDeletesAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        if (null == otiavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            otiavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, otiavFnKey,
                (fnArgs) -> metaData.othersInsertsAreVisible(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        if (null == uadFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            uadFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, uadFnKey,
                (fnArgs) -> metaData.updatesAreDetected(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        if (null == dadFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            dadFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, dadFnKey,
                (fnArgs) -> metaData.deletesAreDetected(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        if (null == iadFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            iadFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, iadFnKey,
                (fnArgs) -> metaData.insertsAreDetected(type), type, this.metadataInstanceId);
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        if (null == sbuFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sbuFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sbuFnKey, (fnArgs) -> metaData.supportsBatchUpdates(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        if (null == gudFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gudFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gudFnKey, this,
                (fnArgs) -> metaData.getUDTs(catalog, schemaPattern, typeNamePattern, types),
                catalog, schemaPattern, typeNamePattern, types, this.metadataInstanceId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.cubeConnection;
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        if (null == sspFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sspFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sspFnKey, (fnArgs) -> metaData.supportsSavepoints(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        if (null == snpFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            snpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, snpFnKey, (fnArgs) -> metaData.supportsNamedParameters(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        if (null == smorFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            smorFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, smorFnKey, (fnArgs) -> metaData.supportsMultipleOpenResults(),
                this.metadataInstanceId);
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        if (null == sggkFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sggkFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sggkFnKey, (fnArgs) -> metaData.supportsGetGeneratedKeys(),
                this.metadataInstanceId);
    }

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        if (null == gsutyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsutyFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gsutyFnKey, this,
                (fnArgs) -> metaData.getSuperTypes(catalog, schemaPattern, typeNamePattern),
                catalog, schemaPattern, typeNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        if (null == gsutaFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsutaFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gsutaFnKey, this,
                (fnArgs) -> metaData.getSuperTables(catalog, schemaPattern, tableNamePattern),
                catalog, schemaPattern, tableNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        if (null == gaFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gaFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gaFnKey, this,
                (fnArgs) -> metaData.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern),
                catalog, schemaPattern, typeNamePattern, attributeNamePattern, this.metadataInstanceId);
    }

    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        if (null == srshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            srshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, srshFnKey,
                (fnArgs) -> metaData.supportsResultSetHoldability(holdability), holdability, this.metadataInstanceId);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        if (null == grshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grshFnKey, (fnArgs) -> metaData.getResultSetHoldability(), this.metadataInstanceId);
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        if (null == gdamavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdamavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gdamavFnKey, (fnArgs) -> metaData.getDatabaseMajorVersion(), this.metadataInstanceId);
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        if (null == gdamivFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdamivFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gdamivFnKey, (fnArgs) -> metaData.getDatabaseMinorVersion(), this.metadataInstanceId);
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        if (null == gjmavFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gjmavFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gjmavFnKey, (fnArgs) -> metaData.getJDBCMajorVersion(), this.metadataInstanceId);
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        if (null == gjmivFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gjmivFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gjmivFnKey, (fnArgs) -> metaData.getJDBCMinorVersion(), this.metadataInstanceId);
    }

    @Override
    public int getSQLStateType() throws SQLException {
        if (null == gsstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gsstFnKey, (fnArgs) -> metaData.getSQLStateType(), this.metadataInstanceId);
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        if (null == lucFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            lucFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, lucFnKey, (fnArgs) -> metaData.locatorsUpdateCopy(), this.metadataInstanceId);
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        if (null == sspFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            sspFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, sspFnKey, (fnArgs) -> metaData.supportsStatementPooling(),
                this.metadataInstanceId);
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        if (null == grilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grilFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (RowIdLifetime) Utils.recordOrMock(config, grilFnKey, (fnArgs) -> metaData.getRowIdLifetime(),
                this.metadataInstanceId);
    }

    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gscFnKey, this,
                (fnArgs) -> metaData.getSchemas(catalog, schemaPattern),
                catalog, schemaPattern, this.metadataInstanceId);
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        if (null == ssfucsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssfucsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ssfucsFnKey, (fnArgs) -> metaData.supportsStoredFunctionsUsingCallSyntax(),
                this.metadataInstanceId);
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        if (null == acfcarsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            acfcarsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, acfcarsFnKey, (fnArgs) -> metaData.autoCommitFailureClosesAllResultSets(),
                this.metadataInstanceId);
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        if (null == gcipFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcipFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gcipFnKey, this, (fnArgs) -> metaData.getClientInfoProperties(), this.metadataInstanceId);
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        if (null == gfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gfFnKey, this,
                (fnArgs) -> metaData.getFunctions(catalog, schemaPattern, functionNamePattern),
                catalog, schemaPattern, functionNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        if (null == gfcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gfcFnKey, this,
                (fnArgs) -> metaData.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern),
                catalog, schemaPattern, functionNamePattern, columnNamePattern, this.metadataInstanceId);
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        if (null == gpcoFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gpcoFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (ResultSet) Utils.recordOrMockResultSet(config, gpcoFnKey, this,
                (fnArgs) -> metaData.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern),
                catalog, schemaPattern, tableNamePattern, columnNamePattern, this.metadataInstanceId);
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        if (null == gkarFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gkarFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, gkarFnKey, (fnArgs) -> metaData.generatedKeyAlwaysReturned(),
                this.metadataInstanceId);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return metaData.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return metaData.isWrapperFor(iface);
    }
}
