const { app, BrowserWindow, ipcMain } = require('electron');
const { autoUpdater } = require('electron-updater');
autoUpdater.logger = require("electron-log")
const isDev = require('electron-is-dev');
const logger = require('electron-log');
const path = require('path');
autoUpdater.autoDownload = true 

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
    logger.info("IPC Event", event);
    event.sender.send('app_version', { version: app.getVersion() });
})

autoUpdater.on('update-available', (event) => {
    logger.info("Update available:::::::::::::")
    logger.info("Logging event", event);
    mainWindow.webContents.send('update_available');
});

autoUpdater.on('update-not-available', arg => {
    logger.info('Update no available:::::::::::::');
    logger.info(arg);
});

autoUpdater.on('download-progress', (event, arg) => {
    logger.info('Download is in progress:::::::::::::');
    logger.info('Printing Event::::', event);
    logger.info('Printing Arg::::::', arg);
    mainWindow.webContents.send('download_progress', Math.round(arg.percent));
});

autoUpdater.on('error', error => {
    logger.info('An error has occured:::::::::::::');
    logger.info(error.message);
    logger.info(error.stack);
});

autoUpdater.on('update-downloaded', info => {
    mainWindow.webContents.send('update_available');
    logger.info("Update downloaded:::::::::::::")
    const quitAndInstalled = autoUpdater.quitAndInstall();
    logger.info('Installed new version:::::::::::::');
    logger.info(quitAndInstalled);
})

ipcMain.on('restart_app', () => {
    logger.info("Doing a quit and install:::::::::::::");
    autoUpdater.quitAndInstall();
});

// const electron = require('electron')
// const path = require('path')
// const isDev = require('electron-is-dev')
// const { autoUpdater } = require('electron-updater')
// const app = electron.app
// const BrowserWindow = electron.BrowserWindow
// const ipcMain = electron.ipcMain

// let mainWindow

// const assetsPath = app.isPackaged ? path.join(process.resourcesPath, "assets") : "assets";

// function createWindow() {
//     mainWindow = new BrowserWindow({
//         width: 800,
//         height: 600,
//         webPreferences: {
//         nodeIntegration: true,
//         },
//     })

// mainWindow.loadURL(
//     isDev
//         ? 'http://localhost:3006'
//         : `file://${path.join(__dirname, '../build/index.html')}`,
//     )

// mainWindow.on('closed', () => { mainWindow = null })

// mainWindow.once('ready-to-show', () => {
//         autoUpdater.checkForUpdatesAndNotify();
//     });
// }

// app.on('ready', createWindow)

// app.on('window-all-closed', () => {
//     if (process.platform !== 'darwin') {
//         app.quit()
//     }
// })

// app.on('activate', () => {
//     if (mainWindow === null) {
//         createWindow()
//     }
// })

// ipcMain.on('app_version', (event) => {
//     event.sender.send('app_version', { version: app.getVersion() });
// });

// autoUpdater.on('update-available', () => {
//     mainWindow.webContents.send('update_available');
// });

// autoUpdater.on('update-downloaded', () => {
//     mainWindow.webContents.send('update_downloaded');
// });

// autoUpdater.on('error', (error) => {
//     console.log('Error on update', error);
// })

// ipcMain.on('restart_app', () => {ipcMain
//     autoUpdater.quitAndInstall();
// });