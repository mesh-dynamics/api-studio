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