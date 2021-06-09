module.exports = {
    "roots": [
      "<rootDir>/../../test"
    ],
    "transform": {
      "^.+\\.(ts|tsx)$": "ts-jest"
    },
    
    verbose: false,
    "testTimeout": 90000,

    "testMatch": [
      "**/test/electron/**.+(ts|tsx|js)",
      "**/test/common/**.+(ts|tsx|js)",
      "**/test/web/**.+(ts|tsx|js)",
  ],
  
  }