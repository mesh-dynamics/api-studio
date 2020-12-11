const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const path = require('path');
const logger = require('electron-log');
const { resourceRootPath } = require('./fs-utils');

const settingsClickHandler = (mainWindow) => {
    logger.info('User clicked on settings menu');

    mainWindow.loadURL(`file://${path.join(__dirname, `../../../dist/pages/settings.html`)}`);
};

const reloadClickHandler = (mainWindow) => {
    logger.info('User clicked on reload');

    mainWindow.loadURL(`file://${path.join(__dirname, `../../../dist/index.html`)}`);
};

const resetClickHandler = (mainWindow) => {
    logger.info('User clicked on reset');

    mainWindow.webContents.send('clear_local_storage');
}

module.exports = {
    settingsClickHandler,
    reloadClickHandler,
    resetClickHandler
};