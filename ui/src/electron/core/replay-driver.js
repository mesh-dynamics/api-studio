const find = require('find-process');
const logger = require('electron-log');
// const { ipcMain } = require('electron');
const { executeJar } = require('./driver-utils/bin-exec');
const { setupJavaBinaries, setupDriverExecutable } = require('./driver-utils/setup');


const setupReplayDriver = async (replayContext) => {
    const REPLAY_DRIVER_PORT = 9992;
    logger.info('Initializing replay driver...');

    try {
        /**
         * Download from a remote source and setup 
         * command paths for jar execution
         */
        const javaBinaryPath = await setupJavaBinaries(); // java location
        const driverExecutablePath = setupDriverExecutable(); // jar location

        // ipcMain.on('start_replay_driver', () => { // TODO: Setup Listeners in client side -- future
        logger.info('Replay driver initialization complete');
        logger.info('JRE binary path: ', javaBinaryPath);
        logger.info('Driver executable path: ', driverExecutablePath);
        
        /**
        * Cleanup and Setup Proxy
        */
        find('port', REPLAY_DRIVER_PORT) // TODO: Remove this once it is taken from config --future
            .then((pList) => {
                    pList.map((item) => {
                            logger.info(`Current PID at replay driver port ${REPLAY_DRIVER_PORT}:`, item.pid);
                            process.kill(item.pid);
                            logger.info(`Killed process ${item.pid} for replay driver startup`);
                        }
                    );
                
                    // Killing process may take time for OS. Delay a few seconds before starting
                setTimeout(() => executeJar(javaBinaryPath, driverExecutablePath, replayContext), 3000);
            })
        
        // }); <-- closing braces for ipc event
    } catch(error) {
        // TODO: Fire up an alert -- future
        logger.info('Error occurred during setting up replay driver :', error);
    }
    
};

module.exports = setupReplayDriver;