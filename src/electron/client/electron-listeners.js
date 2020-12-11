import { ipcRenderer } from '../../common/helpers/ipc-renderer';
import config from '../../common/config';

const setupElectronListeners = () => {

    if(PLATFORM_ELECTRON) {
        const notification = document.getElementById('notification');
        const message = document.getElementById('message');
        const closeButton = document.getElementById('close-button');
        const laterButton = document.getElementById('later-button');
        const downloadButton =  document.getElementById('download-button');
        const restartButton = document.getElementById('restart-button');

        const updaterConfig = {
          releaseType: RELEASE_TYPE, // (develop, staging, master, customer)
          accessKeyId: AWS_ACCESS_KEY_ID,
          secretAccessKey: AWS_SECRET_ACCESS_KEY
        };
    
        ipcRenderer.send('get_config');
        ipcRenderer.send('set_updater_config', updaterConfig);
    
        ipcRenderer.on('update_available', () => {
          ipcRenderer.removeAllListeners('update_available');
          message.innerText = 'A new update is available for download.';
          notification.classList.remove('hidden');
          downloadButton.classList.remove('hidden');
          restartButton.classList.add('hidden');
        });
    
        const downloadProgressInterval = setInterval(() => ipcRenderer.on('download_progress', processDownloadProgress), 1000);
    
        ipcRenderer.on('update_downloaded', () => {
          ipcRenderer.removeAllListeners('update_downloaded');
          message.innerText = 'A new version has been downloaded. It will be installed on restart. Restart now?';
          notification.classList.remove('hidden');
          restartButton.classList.remove('hidden');
          laterButton.classList.remove('hidden');
          downloadButton.classList.add('hidden');
          closeButton.classList.add('hidden');
        });
    
        ipcRenderer.on('get_config', (event, appConfig) => {
          ipcRenderer.removeAllListeners('get_config');
          
          config.apiBaseUrl= `${appConfig.domain}/api`;
          config.recordBaseUrl= `${appConfig.domain}/api/cs`;
          config.replayBaseUrl= `${appConfig.domain}/api/rs`;
          config.analyzeBaseUrl= `${appConfig.domain}/api/as`;
        });

        ipcRenderer.on('error_downloading_update', () => {
          ipcRenderer.removeAllListeners('error_downloading_update');
          message.innerText = 'There was an error downloading update. Please try again later.';
          notification.classList.remove('hidden');

        });

        ipcRenderer.on('clear_local_storage', () => {
          ipcRenderer.removeAllListeners('clear_local_storage');
          localStorage.clear();
          ipcRenderer.send('clear_local_storage_complete');
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

        function downloadUpdate(){
          ipcRenderer.send('download_update');
          downloadButton.classList.add('hidden');
        }

        closeButton.onclick = closeNotification;
        laterButton.onclick = closeNotification;
        downloadButton.onclick = downloadUpdate;
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