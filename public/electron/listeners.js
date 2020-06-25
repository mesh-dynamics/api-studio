/**
 * This file is set up proxy and listeners
 */
const { app, BrowserWindow, ipcMain } = require('electron');
const { autoUpdater } = require('electron-updater');
autoUpdater.logger = require("electron-log")
const isDev = require('electron-is-dev');
const logger = require('electron-log');
const path = require('path');
autoUpdater.autoDownload = true 

const setupListeners = (proxyServerOptions, mockContext, user) => {
    /**
    * ELECTRON WINDOW AND EVENT LISTENER SETUP
    */
    let mainWindow;

    function createWindow () {
        mainWindow = new BrowserWindow({
            // width: 800,
            // height: 600,
            webPreferences: {
            nodeIntegration: true,
            },
        });

        mainWindow.loadURL(
            isDev
                ? 'http://localhost:3006'
                : `file://${path.join(__dirname, '../../build/index.html')}`,
            )
            
        // mainWindow.loadFile('index.html');
        
        mainWindow.on('closed', function () {
            mainWindow = null;
        });

        // mainWindow.maximize();
    }

    /**
     * App window events
     */
    app.on('ready', async () => {
        logger.info("App is ready... Creating Window");
        
        createWindow();

        logger.info("Window created... Checking for updates...")

        autoUpdater.checkForUpdatesAndNotify();
    });

    app.on('window-all-closed', function () {
        if (process.platform !== 'darwin') {
            app.quit();
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
        logger.info("Sending App version to IPC Renderer");
        event.sender.send('app_version', { version: app.getVersion() });
    });

    ipcMain.on('proxy_target_change', (event, arg) => {
        logger.info('Current target is :', targetServer);
        logger.info('Changing proxy target to : ', arg);

        proxyServerOptions.target.host = arg;
        
        logger.info('Updated target server is : ', targetServer);
    });

    ipcMain.on('set_user', (event, arg) => {
        const { access_token, customer_name, token_type, username } = arg;

        logger.info('Updating user info...\n');
        
        user.accessToken = access_token;
        user.customerName = customer_name;
        user.tokenType = token_type;
        user.userName = username;

        logger.info('Updated user info: \n', userInfo);
    });

    ipcMain.on('mock_context_change', (event, arg) => {
        const { collectionId, traceId, spanId, recordingId } = arg;
        
        logger.info('Current mock context :', mockContext);
        logger.info('Changing mock context to : ', arg);

        mockContext.collectionId = collectionId;
        mockContext.traceId = traceId;
        mockContext.spanId = spanId;
        mockContext.recordingId = recordingId;

        logger.info('Updated collection id is : ', mockContext);
    });

    ipcMain.on('restart_app', () => {
        logger.info("Performing a quit and install");
        autoUpdater.quitAndInstall();
    });

    /**
     * AUTO Updater events
     */
    autoUpdater.on('update-available', () => {
        logger.info("Update available")
        mainWindow.webContents.send('update_available');
    });

    autoUpdater.on('update-not-available', arg => {
        logger.info('Update not available');
        logger.info(arg);
    });

    autoUpdater.on('download-progress', (arg) => {
        logger.info('Download is in progress...');
        logger.info(arg)
        mainWindow.webContents.send('download_progress', Math.round(arg.percent));
    });

    autoUpdater.on('error', error => {
        logger.info('An error has occured::');
        logger.info(error.message);
        logger.info(error.stack);
    });

    autoUpdater.on('update-downloaded', info => {
        logger.info("Update downloaded...")
        mainWindow.webContents.send('update_downloaded');
    })
};

module.exports = {
    setupListeners
};