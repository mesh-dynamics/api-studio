/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const config = {
    defaultFetchDiffResults: process.env.REACT_APP_DEFAULT_FETCH_DIFF_RESULTS || 5,
    diffObjectSizeThreshold: process.env.REACT_APP_DIFF_OBJECT_SIZE_THRESHOLD || 100000,
    maxDiffResultsPerPage: process.env.REACT_APP_MAX_DIFF_RESULTS_PER_PAGE || 5,
    diffCollapseLength: process.env.REACT_APP_DIFF_COLLAPSE_LENGTH || 2,
    diffCollapseLengthIncrement: process.env.REACT_APP_DIFF_COLLAPSE_LENGTH_INCREMENT || 10,
    diffCollapseStartIndex: process.env.REACT_APP_DIFF_COLLAPSE_START_INDEX || 3,
    timelineresRefreshIntervel: process.env.REACT_APP_TIMELINERES_REFRESH_INTERVAL || 15000,
    diffMaxLinesLength: process.env.REACT_APP_DIFF_MAX_LENGTH || 1000,
    diffMaxLinesLengthIncrement: process.env.REACT_APP_DIFF_MAX_LENGTH_INCREMENT || 100,
    enableClientSideDiff: process.env.REACT_APP_ENABLE_CLIENT_SIDE_DIFF || "false",
    localReplayBaseUrl: 'http://localhost:9992/rs', // TODO: pick port from config
    apiBaseUrl: '/api',
    recordBaseUrl: '/api/cs',
    replayBaseUrl: '/api/rs',
    analyzeBaseUrl: '/api/as',
    defaultPageSize: 5,
};

export default config;
