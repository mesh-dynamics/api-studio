/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const logger = require('electron-log');
const { exec } = require('child_process');
const { store } = require('../fs-utils');

const testJava = (javaBinaryPath) => {
    logger.info('Testing <java>');

    exec(`${javaBinaryPath} -version`, (error, stdout, stderr) => {
        
        if (error) {
            logger.error(`test exec error: ${error}`);
            return false;
        }
    
        logger.info(`stdout: ${stdout}`);
        logger.error(`stderr: ${stderr}`);
    });

    return true;
}

const executeJar = (javaBinaryPath, driverExecutablePath) => {
    logger.info('Starting Replay Driver...');

    const appDomain = store.get("domain");
    const replayDriverPort = store.get("replayDriverPort");

    logger.info('Replay driver endpoint is set to :', appDomain);
    logger.info('Replay driver port listening at :', replayDriverPort);

    // To pass port config simply pass port number at the end 
    // Example - `"${javaBinaryPath}" -jar "${driverExecutablePath}" 9191`
    const child = exec(`"${javaBinaryPath}" -jar "-Dio.md.service.endpoint=${appDomain}/api" "${driverExecutablePath}" ${replayDriverPort}`, (error, stdout, stderr) => {
        
        if (error) {
            logger.error(`bin exec error: ${error}`);
            return;
        }
    
        logger.info(`stdout: ${stdout}`);
        logger.error(`stderr: ${stderr}`);
    });

    logger.info("pid for started process is : ", child.pid);
};


module.exports = {
    executeJar,
    testJava
};