let ipcRenderer = null;
let remote = null;
let fs = null;

if(PLATFORM_ELECTRON) {
    const electron = window.require('electron');
    fs = window.require("fs");
    ipcRenderer  = electron.ipcRenderer;
    remote = electron.remote;
    
}

export { ipcRenderer, remote, fs };