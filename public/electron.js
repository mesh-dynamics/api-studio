const electron = require('electron')
const path = require('path')
const isDev = require('electron-is-dev')
const { autoUpdater } = require('electron-updater')
const app = electron.app
const BrowserWindow = electron.BrowserWindow
const ipcMain = electron.ipcMain

let mainWindow

const assetsPath = app.isPackaged ? path.join(process.resourcesPath, "assets") : "assets";

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 800,
        height: 600,
        webPreferences: {
        nodeIntegration: true,
        },
    })

mainWindow.loadURL(
    isDev
        ? 'http://localhost:3006'
        : `file://${path.join(__dirname, '../build/index.html')}`,
    )

mainWindow.on('closed', () => { mainWindow = null })

mainWindow.once('ready-to-show', () => {
        autoUpdater.checkForUpdatesAndNotify();
    });
}

app.on('ready', createWindow)

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit()
    }
})

app.on('activate', () => {
    if (mainWindow === null) {
        createWindow()
    }
})

ipcMain.on('app_version', (event) => {
    event.sender.send('app_version', { version: app.getVersion() });
});

autoUpdater.on('update-available', () => {
    mainWindow.webContents.send('update_available');
});

autoUpdater.on('update-downloaded', () => {
    mainWindow.webContents.send('update_downloaded');
});

autoUpdater.on('error', (error) => {
    console.log('Error on update', error);
})

ipcMain.on('restart_app', () => {ipcMain
    autoUpdater.quitAndInstall();
});