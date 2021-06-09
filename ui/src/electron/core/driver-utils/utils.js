const os = require('os');
const path = require('path');
const { testJava } = require('./bin-exec');
const { resourceRootPath } = require('../fs-utils');

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

const jreZipFileName = {
    LINUX: 'OpenJDK14U-jre_x64_linux_openj9_14.0.2_12_openj9-0.21.0.tar.gz',
    MAC: 'jdk-14.0.2+12-jre.tar.gz',
    WINDOWS: 'OpenJDK14U-jre_x64_windows_openj9_14.0.2_12_openj9-0.21.0.zip'
    // LINUX: 'jre-8u281-linux-i586.tar.gz',
    // MAC: 'jre-8u281-macosx-x64.tar.gz',
    // WINDOWS: 'jre-8u281-windows-x64.tar.gz'
}

/**
 * 
 * @param {*} jrePathBasedOnPlatform - can be null
 */
const hasWorkingJre = (jreFullPath) => {
    // if jrePathBasedOnPlatform is not null

    if(jreFullPath) {
        // Return execution status in boolean
        // Will return false if there were errors in execution
        return testJava(jreFullPath);
    }

    return false;
};

/**
 * 
 * @param {*} jreDirectoryPath Path based on platform
 * @path  estimated jre path for the platform
 */
const getJrePathBasedOnPlatform = (jreDirectoryPath) => {
    const platform = platformNames[os.platform()];

    switch (platform) {
        case platforms.WINDOWS:
        case platforms.LINUX:
            //For linux and windows it is under this directory /jre1.8.0_281/bin
            return path.join(jreDirectoryPath, 'jdk-14.0.2+12-jre', 'bin', 'java')
        case platforms.MAC:
            // jre1.8.0_281.jre/Contents/Home/bin
            return path.join(jreDirectoryPath, 'jdk-14.0.2+12-jre', 'Contents', 'Home', 'bin', 'java');
        default:
            return null;
    }
};

const jrePackageFullPath = () => {
    const platform = platformNames[os.platform()];

    switch(platform) {
        case platforms.WINDOWS:
            return path.join(resourceRootPath, 'bin', jreZipFileName.WINDOWS);
        case platforms.MAC:
            return path.join(resourceRootPath, 'bin', jreZipFileName.MAC);
        case platforms.LINUX:
            return path.join(resourceRootPath, 'bin', jreZipFileName.LINUX);
        default:
            return null;
    }
} 

module.exports = {
    hasWorkingJre,
    jrePackageFullPath,
    getJrePathBasedOnPlatform
}