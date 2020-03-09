const config = {
    apiBaseUrl: process.env.REACT_APP_API_BASE_URL || "/api",
    recordBaseUrl: process.env.REACT_APP_RECORD_BASE_URL || "/api/cs",
    replayBaseUrl: process.env.REACT_APP_REPLAY_BASE_URL || "/api/rs",
    analyzeBaseUrl: process.env.REACT_APP_ANALYZE_BASE_URL || "/api/as",
    defaultPageSize: process.env.REACT_APP_DEFAULT_PAGE_SIZE || 5,
};

export default config;
