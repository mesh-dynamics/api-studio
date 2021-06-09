
const fs = require('fs');
const path = require('path');
const process = require('process');
const { app } = require('electron');
const logger = require('electron-log');
const isDev = require("electron-is-dev");
const decompress = require('decompress');
const { resourceRootPath } = require('../fs-utils');
const { 
    hasWorkingJre,
    jrePackageFullPath, 
    getJrePathBasedOnPlatform 
} = require('./utils');

const appData = app.getPath('appData');
const jreOutput = path.join(appData, 'mdjre');

const extractPackageBasedOnPlatform = async () => {

    const jreZipFilePath = jrePackageFullPath();

    if(!jreZipFilePath) {
        // if file does not exist -- throw error
        throw new Error('Error unpacking jre. File not found');
    }

    try {
        // const jreOutput = path.join(resourceRootPath, 'bin', 'java'); 
        
        logger.info('JRE zip file path :', jreZipFilePath);
        
        logger.info('JRE zip output path :', jreOutput);

        await decompress(jreZipFilePath, jreOutput);

        logger.info('Extraction complete');

        // TODO: Return jre path
        return getJrePathBasedOnPlatform(jreOutput);
    } catch (error) {
        logger.info('Error decompressing file', error);

        throw new Error('Error unpacking jre');
    }
};


const setupJavaBinaries = async () => {
    logger.info('Setting up JRE...');

    // const javaRootDirectory = path.join(resourceRootPath, 'bin', 'java');

    // Check if java binary directory exists
    if(fs.existsSync(jreOutput)) {
        // If true
            
        const jreFullPath = getJrePathBasedOnPlatform(jreOutput);
        
        // check if java execution is successful // boolean
        if(hasWorkingJre(jreFullPath)){
            // if true 
                // RETURN PATH OF JAVA BINARY
            logger.info('Detected Existing JRE version :', jreFullPath);

            // return path
            return jreFullPath;
        } else {
            // if false
                    // Cleanup directory
                    fs.rmdirSync(jreOutput, { recursive: true });

                    // Extract Fresh
                    const extractedJrePath = await extractPackageBasedOnPlatform();

                    // return path
                    return extractedJrePath;
        }

    } else {
        // If false
            // Extract Fresh
            const extractedJrePath = await extractPackageBasedOnPlatform();

            // return path
            return extractedJrePath;
    }
};

const setupDriverExecutable = () => {
    logger.info('Setting up driver executable');

    const jarPath = path.join(resourceRootPath, 'bin', 'cubews-replay-V1-SNAPSHOT-jar-with-dependencies.jar');

    if(fs.existsSync(jarPath)) {
        return jarPath;
    } else {
        throw new Error('Executable Jar not found at location.')
    }
};


module.exports = {
    setupJavaBinaries,
    setupDriverExecutable
};