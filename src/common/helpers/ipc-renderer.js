import isElectron from 'is-electron';

let ipcRenderer = null;


if(isElectron()) {
    const electron = window.require('electron');
    ipcRenderer  = electron.ipcRenderer;
}

export { isElectron, ipcRenderer };