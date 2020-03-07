const config = {
    apiBaseUrl: "https://demo.dev.cubecorp.io/api",
    recordBaseUrl: "https://demo.dev.cubecorp.io/api/cs",
    replayBaseUrl: "https://demo.dev.cubecorp.io/api/rs",
    analyzeBaseUrl: "https://demo.dev.cubecorp.io/api/as",
    defaultPageSize: process.env.REACT_APP_DEFAULT_PAGE_SIZE || 5,
};

export default config;
