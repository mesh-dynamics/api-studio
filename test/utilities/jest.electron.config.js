const config  = require('./jest.config');
module.exports = {
    ...config,
    "testMatch": [
        "**/test/electron/**.+(ts|tsx|js)",
        "**/test/common/**.+(ts|tsx|js)",
    ],
    setupFiles:[
      "./jestSetup.electron.js"
    ],
    "globals": {
      "PLATFORM_ELECTRON": true
    }
}