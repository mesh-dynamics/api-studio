import { ipcRenderer } from '../../common/helpers/ipc-renderer';
import config from '../../common/config';

const setupElectronListeners = () => {

    if(PLATFORM_ELECTRON) {
        const notification = document.getElementById('notification');
        const message = document.getElementById('message');
        const closeButton = document.getElementById('close-button');
        const restartButton = document.getElementById('restart-button');
    
        ipcRenderer.send('get_config');
    
        ipcRenderer.on('update_available', () => {
          ipcRenderer.removeAllListeners('update_available');
          message.innerText = 'A new update is available. Downloading now...';
          notification.classList.remove('hidden');
        });
    
        const downloadProgressInterval = setInterval(() => ipcRenderer.on('download_progress', processDownloadProgress), 1000);
    
        ipcRenderer.on('update_downloaded', () => {
          ipcRenderer.removeAllListeners('update_downloaded');
          message.innerText = 'A new version has been downloaded. It will be installed on restart. Restart now?';
          notification.classList.remove('hidden');
          restartButton.classList.remove('hidden');
        });
    
        ipcRenderer.on('get_config', (event, appConfig) => {
          ipcRenderer.removeAllListeners('get_config');
          
          config.apiBaseUrl= `${appConfig.domain}/api`;
          config.recordBaseUrl= `${appConfig.domain}/api/cs`;
          config.replayBaseUrl= `${appConfig.domain}/api/rs`;
          config.analyzeBaseUrl= `${appConfig.domain}/api/as`;
        });
    
        function processDownloadProgress(event, percent) {
          ipcRenderer.removeAllListeners('download_progress');
          message.innerText = "Please do not close the main window.\nDownloading... " + percent + "%";
    
          if (percent === 100) {
            clearInterval(downloadProgressInterval);
          }
        }
    
        function closeNotification() {
          notification.classList.add('hidden');
        }
    
        function restartApp() {
          ipcRenderer.send('restart_app');
        }

        closeButton.onclick = closeNotification;
        restartButton.onclick = restartApp;
    }
};

export {
    setupElectronListeners
}
        
// ipcRenderer.send('app_version');
// ipcRenderer.on('app_version', (event, arg) => {
//   ipcRenderer.removeAllListeners('app_version');
//   version.innerText = 'Version ' + arg.version;
// });