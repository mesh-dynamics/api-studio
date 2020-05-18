const config = {
    apiBaseUrl: process.env.REACT_APP_API_BASE_URL || "/api",
    recordBaseUrl: process.env.REACT_APP_RECORD_BASE_URL || "/api/cs",
    replayBaseUrl: process.env.REACT_APP_REPLAY_BASE_URL || "/api/rs",
    analyzeBaseUrl: process.env.REACT_APP_ANALYZE_BASE_URL || "/api/as",
    defaultFetchDiffResults: process.env.REACT_APP_DEFAULT_FETCH_DIFF_RESULTS || 5,
    diffObjectSizeThreshold: process.env.REACT_APP_DIFF_OBJECT_SIZE_THRESHOLD || 100000,
    maxDiffResultsPerPage: process.env.REACT_APP_MAX_DIFF_RESULTS_PER_PAGE || 5,
    diffCollapseLength: process.env.REACT_APP_DIFF_COLLAPSE_LENGTH || 2,
    diffCollapseLengthIncrement: process.env.REACT_APP_DIFF_COLLAPSE_LENGTH_INCREMENT || 10,
    diffCollapseStartIndex: process.env.REACT_APP_DIFF_COLLAPSE_START_INDEX || 3,
    timelineresRefreshIntervel: process.env.REACT_APP_TIMELINERES_REFRESH_INTERVAL || 15000,
    diffMaxLinesLength: process.env.REACT_APP_DIFF_MAX_LENGTH || 1000,
    diffMaxLinesLengthIncrement: process.env.REACT_APP_DIFF_MAX_LENGTH_INCREMENT || 100,
    enableClientSideDiff: process.env.REACT_APP_ENABLE_CLIENT_SIDE_DIFF || false
};

export default config;
