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

const { app, Menu } = require('electron');
const events = require('./event-handlers');

const isMac = process.platform === 'darwin';

const menutemplate = (mainWindow) => [
    {
        label: 'App',
        submenu: [
            { 
                label: 'Settings',
                click() {
                    events.settingsClickHandler(mainWindow);
                }
            },
            { role: 'about' },
            { role: 'quit' }

        ]
    },
    {
        label: 'Edit',
        submenu: [
          { role: 'undo' },
          { role: 'redo' },
          { type: 'separator' },
          { role: 'cut' },
          { role: 'copy' },
          { role: 'paste' },
          ...(isMac ? [
            { role: 'pasteAndMatchStyle' },
            { role: 'delete' },
            { role: 'selectAll' },
            { type: 'separator' },
            {
              label: 'Speech',
              submenu: [
                { role: 'startspeaking' },
                { role: 'stopspeaking' }
              ]
            }
          ] : [
            { role: 'delete' },
            { type: 'separator' },
            { role: 'selectAll' }
          ])
        ]
    },
    {
        label: 'View',
        submenu: [
            // {
            //     role: 'reload'
            // },
            { 
                label: 'Reload',
                click() {
                    events.reloadClickHandler(mainWindow);
                }
            },
            {
                role: 'toggledevtools'
            },
            {
                type: 'separator'
            },
            {
                role: 'resetzoom'
            },
            {
                role: 'zoomin'
            },
            {
                role: 'zoomout'
            },
            {
                type: 'separator'
            },
            {
                role: 'togglefullscreen'
            },
            {
                label: 'Reset',
                click() {
                    events.resetClickHandler(mainWindow);
                }
            },
            {
                label: 'Restart',
                click() {
                    app.relaunch()
                    app.exit()
                }
            },
        ]
    }
];

const createMenuTemplate = (window) => Menu.buildFromTemplate(menutemplate(window))

module.exports = {
    createMenuTemplate
};