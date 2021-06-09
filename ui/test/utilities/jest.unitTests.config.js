module.exports = {
  roots: ["<rootDir>/../../"],
  transform: {
    "^.+\\.(ts|tsx)$": "ts-jest",
  },

  verbose: false,
  testTimeout: 90000,

  testMatch: ["**/test/unit/**/**.+(ts|tsx|js)"],
};
