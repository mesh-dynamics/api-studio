/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const { app, BrowserWindow, ipcMain, Menu, session  } = require('electron');
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

    session.defaultSession.clearStorageData([]);

    mainWindow.webContents.send('clear_local_storage');

}

module.exports = {
    settingsClickHandler,
    reloadClickHandler,
    resetClickHandler
};