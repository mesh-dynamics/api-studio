const { app, BrowserWindow, ipcMain } = require('electron');
const { autoUpdater } = require('electron-updater');
autoUpdater.logger = require("electron-log")
const isDev = require('electron-is-dev');
const httpProxy = require('http-proxy');
const logger = require('electron-log');
const path = require('path');
autoUpdater.autoDownload = true 

// TODO: Add daily reminder for installing update 
// manually if the window is not closed

let defaultTargetHost = 'demo.dev.cubecorp.io';
/**
 * Target Server Options
 */
const targetOptions = {
    target: {
        protocol: 'https:',
        host: defaultTargetHost,
        port: 443,
    },
    changeOrigin: true,
};

/**
 * @param {*} proxyReq 
 * @param {*} req 
 * @param {*} res 
 * @param {*} options 
 */
const proxyRequestInterceptor = (proxyReq) => {
    logger.info('Request Intercepted. Removing Header <Origin>');
    proxyReq.removeHeader('Origin');
    logger.info('Logging Request Headers\n', proxyReq._headers);
};

const proxy = httpProxy.createProxyServer(targetOptions);

/**
 * Proxy Event Listener
 */
proxy.on('proxyReq', proxyRequestInterceptor);

/**
 * Setup Proxy Listening
 */
proxy.listen(9000);

/**
 * END OF PROXY SERVER CODE
 */

/**************************************************** */

/**
 * ELECTRON WINDOW AND EVENT LISTENER SETUP
 */
let mainWindow;

function createWindow () {
    mainWindow = new BrowserWindow({
        width: 800,
        height: 600,
        webPreferences: {
        nodeIntegration: true,
        },
    });

    mainWindow.loadURL(
        isDev
            ? 'http://localhost:3006'
            : `file://${path.join(__dirname, '../build/index.html')}`,
        )
        
    // mainWindow.loadFile('index.html');
    
    mainWindow.on('closed', function () {
        mainWindow = null;
    });
}

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


ipcMain.on('app_version', (event) => {
    logger.info("Sending App version to IPC Renderer");
    event.sender.send('app_version', { version: app.getVersion() });
});

ipcMain.on('proxy_target_change', (event, arg) => {
    logger.info('Current target is :', targetServer);
    logger.info('Changing proxy target to : ', arg);
    targetServer = arg;
    logger.info('Updated target server is : ', targetServer);
});

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

ipcMain.on('restart_app', () => {
    logger.info("Performing a quit and install");
    autoUpdater.quitAndInstall();
});