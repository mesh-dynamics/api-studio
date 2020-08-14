const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const path = require('path');
const logger = require('electron-log');
const { resourceRootPath } = require('./fs-utils');

const settingsClickHandler = (mainWindow) => {
    logger.info('User clicked on settings menu');

    mainWindow.loadURL(`file://${path.join(__dirname, `../../../dist/pages/settings.html`)}`);
};

module.exports = {
    settingsClickHandler
};