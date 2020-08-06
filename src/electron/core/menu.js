const { app, Menu } = require('electron');
const events = require('./event-handlers');

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
        label: 'View',
        submenu: [
            {
                role: 'reload'
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