const config  = require('./jest.config');
//"**/?(*.)+(spec|test).+(ts|tsx|js)"
module.exports = {
    ...config,
    "testMatch": [
        "**/test/web/**.+(ts|tsx|js)",
        "**/test/common/**.+(ts|tsx|js)",
    ],
    setupFiles:[
      "./jestSetup.web.js"
    ],
    "globals": {
      "PLATFORM_ELECTRON": false
    }
}