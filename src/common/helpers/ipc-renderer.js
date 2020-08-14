let ipcRenderer = null;

if(PLATFORM_ELECTRON) {
    const electron = window.require('electron');
    ipcRenderer  = electron.ipcRenderer;
}

export { ipcRenderer };