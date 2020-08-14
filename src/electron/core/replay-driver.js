const os = require('os');
const fs = require('fs');
const path = require('path');
const logger = require('electron-log');
const extract = require('extract-zip');
const isDev = require('electron-is-dev');
const download = require('download-file');
const { ipcMain } = require('electron');
const { exec } = require('child_process');

let jdkDownloaded = false;

const javaBinaryUrl = {
    macOS: '',
    windows: 'https://download.java.net/openjdk/jdk12/ri/openjdk-12+32_windows-x64_bin.zip',
    linux: 'https://download.java.net/openjdk/jdk12/ri/openjdk-12+32_linux-x64_bin.tar.gz',
};

const platforms = {
    WINDOWS: 'WINDOWS',
    MAC: 'MAC',
    LINUX: 'LINUX',
};

const platformNames = {
    win32: platforms.WINDOWS,
    darwin: platforms.MAC,
    linux: platforms.LINUX,
};

const devConfig = {
    javaDownloadDirectory: path.join(__dirname, '../../java'),
    javaZipPath: path.join(__dirname, '../../java/jdk.zip'),
    javaUnzipPath: path.join(__dirname, "../../java/jdk"),
    javaBinaryPath: path.join(__dirname, '../../java/jdk/jdk-12/bin/java.exe'),
    javaExecutablePath: path.join(__dirname, '../../bin/demo-0.0.1-SNAPSHOT.war')
};

const prodConfig = {
    javaDownloadDirectory: path.join(__dirname, '../java'),
    javaZipPath: path.join(__dirname, '../java/jdk.zip'),
    javaUnzipPath: path.join(__dirname, "../java/jdk"),
    javaBinaryPath: path.join(__dirname, '../java/jdk/jdk-12/bin/java.exe'),
    javaExecutablePath: path.join(__dirname, '../bin/demo-0.0.1-SNAPSHOT.war')
};

const replayDriverConfig = isDev ? devConfig : prodConfig;

const downloadHandler = (error) => {
    if(error) {
        logger.info('Error downloading jdk :', error);
    }

    jdkDownload = true;

    extractJVM();
}

const downloadJavaBinaries = () => {
    try {
        logger.info('Platform Detected :', platformNames[os.platform()]);

        const downloadOptions = {
            directory: replayDriverConfig.javaDownloadDirectory,
            filename: "jdk.zip"
        };
    
        if(fs.existsSync(path.join(replayDriverConfig.javaDownloadDirectory, 'jdk.zip'))) {
            logger.info('Java downloaded. Skipping download...')
            return;
        }
    
        if(platformNames[os.platform()] === platforms.WINDOWS) {
            logger.info('Downloading java for Windows...');
            
            download(javaBinaryUrl.windows, downloadOptions, downloadHandler);
    
            return;
        }
    
        if(platformNames[os.platform()] === platforms.MAC) {
            logger.info('Downloading java for Mac...');
    
            download(javaBinaryUrl.macOS, downloadOptions, downloadHandler);
    
            return;
        }
    
        if(platformNames[os.platform()] === platforms.LINUX) {
            logger.info('Downloading java for Linux...');
    
            download(javaBinaryUrl.linux, downloadOptions, downloadHandler);
    
            return;
        }
    } catch (error) {
        logger.info('Error Downloading Binaries', error);
    }
    
};

const extractJVM = async (replayContext) => {
    const { javaZipPath, javaUnzipPath, javaBinaryPath } = replayDriverConfig;
    
    logger.info("Java binary path :", javaBinaryPath);

    try {
        if(fs.existsSync(javaBinaryPath)) {

            logger.info('java already exists... skipping extraction...');
            logger.info('java path:', javaBinaryPath);

        } else {
            logger.info('Starting Extraction');

            await extract(javaZipPath, { dir: javaUnzipPath });
            // await extract(source, { dir: target });

            logger.info('Extraction complete');
        }

    } catch (err) {
        logger.info('Error extracting JVM', err);
        // handle any errors
    }
};

const executeJar = () => {
    logger.info('Starting Server...');

    const { javaBinaryPath, javaExecutablePath } = replayDriverConfig;
    
    const child = exec(`${javaBinaryPath} -jar ${javaExecutablePath}`, (error, stdout, stderr) => {
        
        if (error) {
            logger.error(`exec error: ${error}`);
            return;
        }
    
        logger.info(`stdout: ${stdout}`);
        logger.error(`stderr: ${stderr}`);
    });

    logger.info("pid for started process is : ", child.pid);
};

const setupReplayDriver = (replayContext) => {
    
    /**
     * Download from a remote source and setup 
     * command paths for jar execution
     */
    downloadJavaBinaries();

    ipcMain.on('start_replay_driver', () => {
        logger.info('Starting Replay Driver...');
    
        executeJar();
    });

};

module.exports = {
    setupReplayDriver
};