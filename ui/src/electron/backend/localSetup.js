const {app} = require('electron');
const fs = require('fs');
const path = require('path');
const childProcess = require('child_process');
const logger = require("electron-log");
const find = require('find-process');
const Store = require("electron-store");
const {setupJavaBinaries,
    setupCoreExecutable,
    setupGatewayExecutable} = require('../core/driver-utils/setup')
const store = new Store();

const setupLocalCubeBackend = async() => {
  // const app = remote.app 
  try{
    ///localhost check 
    const domain = store.get("domain");
    const replayDriverPort = store.get("replayDriverPort"); //Default 9004
    const cubeUIBackendPort = store.get("cubeUIBackendPort"); //Default 9003
    const redisPort = store.get("redisPort"); //Default 9005
   
        const userDataPath = app.getPath("userData")
        const localCubeIOBackendDataPath = path.join(userDataPath, "cubeioBackendData")
        if(!fs.existsSync(localCubeIOBackendDataPath)) {
            fs.mkdirSync(localCubeIOBackendDataPath)
        }
        const localCatlinaCorePath = path.join(userDataPath, "core")
        if(!fs.existsSync(localCatlinaCorePath)) {
            fs.mkdirSync(localCatlinaCorePath)
        }
        const localCatlinaGatewayPath = path.join(userDataPath, "gateway")
        if(!fs.existsSync(localCatlinaGatewayPath)) {
            fs.mkdirSync(localCatlinaGatewayPath)
        }
        const localCubeUIBackendDataPath = path.join(userDataPath, "cubeuiBackendData")
        if(!fs.existsSync(localCubeUIBackendDataPath)) {
            fs.mkdirSync(localCubeUIBackendDataPath)
        }
        const javaBinaryPath = await setupJavaBinaries(); // java location
        const coreJarPath = setupCoreExecutable();
        const gatewayBinaryPath = setupGatewayExecutable();

        const gatewayCommand = `"${javaBinaryPath}" -Dspring.profiles.active=local -Dspring.datasource.url="jdbc:h2:file:${localCubeUIBackendDataPath}"  -jar -Dcube.server.port=${replayDriverPort} -Dserver.port=${cubeUIBackendPort} -Dcatalina.base="${localCatlinaGatewayPath}" -Dlog4j.configurationFile=log4j2local.xml "${gatewayBinaryPath}"`
        const coreCommand = `"${javaBinaryPath}" -jar -Ddata_dir="${localCubeIOBackendDataPath}" -Dcatalina.base="${localCatlinaCorePath}" -DPORT=${replayDriverPort} -Dlog4j.configurationFile=log4j2local.xml -Dredis_port=${redisPort} -Drun_mode=local  "${coreJarPath}"`;


         /**
        * Cleanup and Setup Proxy
        */
          find('port', cubeUIBackendPort) // TODO: Remove this once it is taken from config --future
          .then((pList) => {
                  pList.map((item) => {
                          logger.info(`Current PID at gateway port ${cubeUIBackendPort}:`, item.pid);
                          process.kill(item.pid);
                          logger.info(`Killed process ${item.pid} for gateway startup`);
                      }
                  );

                  const timeout = pList.length > 0 ? 3000: 0;
              
                  // Killing process may take time for OS. Delay a few seconds before starting
                    setTimeout(() => {
                        
                        logger.info("Running core command", coreCommand);
                        if(domain.includes(`//localhost:${cubeUIBackendPort}`)){
                            logger.log("Found localhost as backend, will start local backend server");
                            logger.info("Running gateway command", gatewayCommand);
                            const gatewayChild = childProcess.exec(gatewayCommand, (error, stdout, stderr) => {
                            
                                if (error) {
                                    logger.error(`bin exec error: ${error}`);
                                    return;
                                }
                            
                                logger.info(`stdout: ${stdout}`);
                                logger.error(`stderr: ${stderr}`);
                            });

                            logger.info("pid for started process is : ", gatewayChild.pid);
                        }else{
                            logger.log("Not a localhost backend, will not start local-backend")
                        }
                    }, timeout);
          })

        /**
        * Cleanup and Replay driver
        */
         find('port', replayDriverPort) // TODO: Remove this once it is taken from config --future
         .then((pList) => {
                 pList.map((item) => {
                         logger.info(`Current PID at replay JAR port ${replayDriverPort}:`, item.pid);
                         process.kill(item.pid);
                         logger.info(`Killed process ${item.pid} for replay JAR startup`);
                     }
                 );
                 const timeout = pList.length > 0 ? 3000: 0;
                 // Killing process may take time for OS. Delay a few seconds before starting
             setTimeout(() => {
                const coreChild = childProcess.exec(coreCommand, (error, stdout, stderr) => {
                    
                    if (error) {
                        logger.error(`bin exec error: ${error}`);
                        return;
                    }
                
                    logger.info(`stdout: ${stdout}`);
                    logger.error(`stderr: ${stderr}`);
                });

                logger.info("pid for started process is : ", coreChild.pid);
             }, timeout);
         })
     
   
  }
  catch(error){
      console.error("Some error while starting local server", error);
  }
}

module.exports = setupLocalCubeBackend 
