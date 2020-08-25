/**
 * This file is set up proxy and listeners
 */
const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const { autoUpdater } = require('electron-updater');
autoUpdater.logger = require('electron-log')
const isDev = require('electron-is-dev');
const logger = require('electron-log');
const find = require('find-process');
const path = require('path');
const aws4 = require('aws4');
const os = require('os');
const menu = require('./menu');
const { resourceRootPath, updateApplicationConfig, getApplicationConfig } = require('./fs-utils');
const { useGetLatest } = require('react-table');

autoUpdater.autoDownload = false; 
autoUpdater.channel = 'latest';

let mainWindow;
let releaseDirectory = '/'; // Default is root of bucket

const browserWindowOptions = {
    width: 1280,
    height: 720,
    webPreferences: {
    nodeIntegration: true,
    },
};

const releaseMetaDataFile = (releaseType) => {
    const platform = os.platform();

    const filePath = {
        win32: 'latest.yml',
        darwin:  'latest-mac.yml',
        linux: 'latest-linux.yml'
        // In Case this is required.
        // win32: `${releaseType}.yml`,
        // darwin: `${releaseType}-mac.yml`,
        // linux: `${releaseType}-linux.yml`
    };

    return filePath[platform];
};

const awsSigingOptions = {
    service: 's3',
    region: 'us-east-2',
    method: 'GET',
    host:  'meshdynamics-devtool.s3.amazonaws.com',
    path: ''
};

const awsSigningCredentials = {
    accessKeyId: '',
    secretAccessKey: ''
};

const mainWindowIndex = `file://${path.join(__dirname, `../../../dist/index.html`)}`;

const setupListeners = (mockContext, user, replayContext) => {
    logger.info('Setup listeners');

    // Loading config
    const config = getApplicationConfig();

    logger.info('Config loaded :', config);

    /**
    * ELECTRON WINDOW AND EVENT LISTENER SETUP
    */

    const createWindow = () => {
        mainWindow = new BrowserWindow(browserWindowOptions);

        mainWindow.loadURL(mainWindowIndex);
        
        mainWindow.on('closed', function () {
            mainWindow = null;
        });

        // Set up menu
        Menu.setApplicationMenu(menu.createMenuTemplate(mainWindow));
    }

    /**
     * App window events
     */
    app.on('ready', async () => {
        logger.info('App is ready... Creating Window');
        
        createWindow();

        logger.info('Window created... Checking for updates...')

        // autoUpdater.checkForUpdatesAndNotify();
        // autoUpdater.checkForUpdates();
    });

    app.on('window-all-closed', async function () {
        try {
            const { mock: { proxyPort }} = config;

            const proxyPortList = await find('port', proxyPort);
            
            logger.info(`Processes Listening at port ${proxyPort} : `, proxyPortList);

            proxyPortList.map((item) => {
                logger.info(`Found process active at port ${proxyPort}`, item.pid);
                logger.info('Kill Process...')
                process.kill(item.pid);
            });
    
            if (process.platform !== 'darwin') {
                app.quit();
            }
            
        } catch (error) {
            logger.info('Error Exiting App', error);
        }
    });

    app.on('activate', function () {
        if (mainWindow === null) {
            createWindow();
        }
    });


    /**
     * IPC Events
     */
    ipcMain.on('app_version', (event) => {
        logger.info('Sending App version to IPC Renderer');
        event.sender.send('app_version', { version: app.getVersion() });
    });

    ipcMain.on('set_updater_config', (event, updaterConfig) => {
        logger.info('Received Updater Config', );

        const { releaseType, accessKeyId, secretAccessKey } = updaterConfig; // (develop, staging, master, customer)

        releaseDirectory = `/${releaseType}`;

        const metaFilePath = `/${releaseType}/${releaseMetaDataFile(releaseType)}`;

        awsSigingOptions.path = metaFilePath;

        awsSigningCredentials.accessKeyId = accessKeyId;

        awsSigningCredentials.secretAccessKey = secretAccessKey;

        logger.info('Current release directory is set to :', releaseDirectory);

        logger.info('Updated AWS Signing Options \n', awsSigingOptions);

        // Check for updates once the configs are set
        autoUpdater.checkForUpdates();
    });

    ipcMain.on('set_user', (event, arg) => {
        const { access_token, customer_name, token_type, username } = arg;

        logger.info('Updating user info...\n');
        
        user.accessToken = access_token;
        user.customerName = customer_name;
        user.tokenType = token_type;
        user.userName = username;

        logger.info('Updated user info: \n', user);
    });

    ipcMain.on('mock_context_change', (event, arg) => {
        const { collectionId, traceId, selectedApp, customerName, recordingCollectionId, runId } = arg;

        logger.info('Current mock context :', mockContext);
        logger.info('Changing mock context to : ', arg);

        mockContext.traceId = traceId;
        mockContext.selectedApp = selectedApp;
        mockContext.customerName = customerName;
        mockContext.collectionId = collectionId;
        mockContext.recordingCollectionId = recordingCollectionId;        
        mockContext.runId = runId   
        
        logger.info('Updated context is : ', mockContext);
    });

    ipcMain.on('restart_app', () => {
        logger.info('Performing a quit and install');
        autoUpdater.quitAndInstall();
    });

    ipcMain.on('save_target_domain', (event, config) => {
        // The whole config object is required
        logger.info('Received target domain as :', config);

        logger.info('Writing to config file...');

        // utils.writeTargetToConfig(config);
        updateApplicationConfig(config);

        event.sender.send('config_update_success');
        
        app.relaunch();
        app.exit();
    });

    ipcMain.on('get_config', (event) => {
        logger.info('Sending Config to IPC Renderer');
        event.sender.send('get_config', config);
    });

    ipcMain.on('return_main_window', (event) => {
        logger.info('Returing to main window');
        mainWindow.loadURL(mainWindowIndex);
    });


    /**
     * AUTO Updater events
     */
    autoUpdater.on('checking-for-update', () => {
        logger.info('Update checking triggered');
        
        aws4.sign(awsSigingOptions, awsSigningCredentials);

        logger.info("AWS Header Options", awsSigingOptions);

        autoUpdater.requestHeaders = awsSigingOptions.headers;

        logger.info('Update Check Complete');
    });

    autoUpdater.on('update-available', (info) => {
        logger.info('Update available');
        
        logger.info("\n\n", info);

        const filePath =  `${releaseDirectory}/${info.path}`;

        awsSigingOptions.path = filePath;

        // Sign the headers
        aws4.sign(awsSigingOptions, awsSigningCredentials);

        logger.info("Updater Signing Options \n", awsSigingOptions);

        // Update the headers
        autoUpdater.requestHeaders = awsSigingOptions.headers;
        
        // Trigger Download
        autoUpdater.downloadUpdate();

        // Notify Renderer Process
        mainWindow.webContents.send('update_available');
    });

    autoUpdater.on('update-not-available', arg => {
        logger.info('Update not available');
        logger.info(arg);
    });

    autoUpdater.on('download-progress', (arg) => {
        logger.info('Download is in progress...');
        logger.info(arg);
        try {
            mainWindow.webContents.send('download_progress', Math.round(arg.percent));
        } catch (error) {
            logger.info('Error while downloading \n', error);
        }
        
    });

    autoUpdater.on('error', error => {
        logger.info('An error has occured::');
        logger.info(error.message);
        logger.info(error.stack);
    });

    autoUpdater.on('update-downloaded', info => {
        logger.info('Update downloaded...')
        mainWindow.webContents.send('update_downloaded');
    })
};

module.exports = {
    setupListeners
};
