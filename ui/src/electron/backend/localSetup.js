const {app} = require('electron');
const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');
const logger = require("electron-log");
const Store = require("electron-store");
const {setupJavaBinaries,
    setupCoreExecutable,
    setupGatewayExecutable} = require('../core/driver-utils/setup')
const store = new Store();

const setupLocalCubeBackend = async() => {
  // const app = remote.app 
  try{

  
    const userDataPath = app.getPath("userData")
    const localCubeIOBackendDataPath = path.join(userDataPath, "cubeioBackendData")
    if(!fs.existsSync(localCubeIOBackendDataPath)) {
        fs.mkdirSync(localCubeIOBackendDataPath)
    }
    const localCubeUIBackendDataPath = path.join(userDataPath, "cubeuiBackendData")
    if(!fs.existsSync(localCubeUIBackendDataPath)) {
        fs.mkdirSync(localCubeUIBackendDataPath)
    }
    const javaBinaryPath = await setupJavaBinaries(); // java location
    const coreJarPath = setupCoreExecutable();
    const gatewayBinaryPath = setupGatewayExecutable();

    const replayDriverPort = store.get("replayDriverPort"); //Default 9004
    const cubeUIBackendPort = store.get("cubeUIBackendPort"); //Default 9003
    const gatewayCommand = `"${javaBinaryPath}" -Dspring.profiles.active=local -Dspring.datasource.url="jdbc:h2:file:${localCubeUIBackendDataPath}"  -jar -Dcube.server.port=${replayDriverPort} -Dserver.port=${cubeUIBackendPort} "${gatewayBinaryPath}"`
    const coreCommand = `"${javaBinaryPath}" -jar -Ddata_dir="${localCubeIOBackendDataPath}" -DPORT=${replayDriverPort}  "${coreJarPath}"`;
    logger.info("Running gateway command", gatewayCommand);
    logger.info("Running core command", coreCommand);
    const gatewayChild = childProcess.exec(gatewayCommand, (error, stdout, stderr) => {
        
        if (error) {
            logger.error(`bin exec error: ${error}`);
            return;
        }
    
        logger.info(`stdout: ${stdout}`);
        logger.error(`stderr: ${stderr}`);
    });

    logger.info("pid for started process is : ", gatewayChild.pid);
    const coreChild = childProcess.exec(coreCommand, (error, stdout, stderr) => {
        
        if (error) {
            logger.error(`bin exec error: ${error}`);
            return;
        }
    
        logger.info(`stdout: ${stdout}`);
        logger.error(`stderr: ${stderr}`);
    });

    logger.info("pid for started process is : ", coreChild.pid);
  }
  catch(error){
      console.error("Some error while starting local server", error);
  }
}

module.exports = setupLocalCubeBackend 
