import { cubeConstants } from '../constants';
import { cubeService } from '../services'

export const cubeActions = {
    getApps,
    setSelectedApp,
    getTestIds,
    setSelectedTestId,
    getReplayId,
    getGraphData,
    startReplay,
    getReplayStatus,
    getAnalysis,
    getReport,
    clear
};

function clear() {
    return async dispatch => {
        dispatch(clearPrev());
    };

    function clearPrev() {
        return { type: cubeConstants.CLEAR_PREVIOUS_DATA, data: null };
    }
}

function getApps () {
    return async dispatch => {
        dispatch(request());
        try {
            let appsList = await cubeService.fetchAppsList();
            dispatch(success(appsList, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getApps", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST }; }
    function success(appsList, date) { return { type: cubeConstants.APPS_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date }; }
}

function setSelectedApp ( appLabel ) {
    return {type: cubeConstants.SET_SELECTED_APP, data: appLabel}
}

function getTestIds () {
    return async dispatch => {
        dispatch(request());
        try {
            let collections = await cubeService.fetchCollectionList();
            dispatch(success(collections, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getTestIds", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TESTIDS_REQUEST } }
    function success(collections, date) { return { type: cubeConstants.TESTIDS_SUCCESS, data: collections, date: date } }
    function failure(message, date) { return { type: cubeConstants.TESTIDS_FAILURE, err: message, date: date } }
}

function setSelectedTestId ( testIdLabel ) {
    return {type: cubeConstants.SET_SELECTED_TESTID, data: testIdLabel};
}

function getGraphData () {
    return async dispatch => {
        dispatch(request());
        try {
            let gd = await cubeService.getGraphData();
            dispatch(success(gd, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getTestIds", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.GRAPH_REQUEST } }
    function success(gd, date) { return { type: cubeConstants.GRAPH_REQUEST_SUCCESS, data: gd, date: date } }
    function failure(message, date) { return { type: cubeConstants.GRAPH_REQUEST_FAILURE, err: message, date: date } }
}

function getReplayId(testIdLabel) {
    return async dispatch => {
        // dispatch(request());
        try {
            let replayId = await cubeService.getReplayId(testIdLabel);
            dispatch(success(replayId, Date.now()));
        } catch (error) {
            // dispatch(failure("Failed to getTestIds", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TESTIDS_REQUEST } }
    function success(replayId, date) { return { type: cubeConstants.REPLAY_ID_SUCCESS, data: replayId, date: date } }
    function failure(message, date) { return { type: cubeConstants.TESTIDS_FAILURE, err: message, date: date } }
}

function getTimelineData(replayData) {
    return async dispatch => {
        try {
            let timeline = await cubeService.fetchTimelineData(replayId);
            dispatch(success(timeline, Date.now()));
        } catch (error) {

        }
    };

    function success(timeline, date) { return { type: cubeConstants.TIMELINE_DATA_SUCCESS, data: timeline, date: date } }
}

function startReplay(collectionId, replayId) {
    return async dispatch => {
        // dispatch(request());
        try {
            dispatch(success(Date.now()));
            let startReplay = await cubeService.startReplay(collectionId, replayId);
            dispatch(success(replayId, Date.now()));
        } catch (error) {
            // dispatch(failure("Failed to getTestIds", Date.now()));
        }
    }
    function success(date) { return { type: cubeConstants.REPLAY_STARTED, date: date } }
}

function getReplayStatus(collectionId, replayId) {
    return async dispatch => {
        // dispatch(request());
        try {
            let replayStatus = await cubeService.checkStatusForReplay(collectionId, replayId);
            dispatch(success(replayStatus, Date.now()));
        } catch (error) {
            // dispatch(failure("Failed to getTestIds", Date.now()));
        }
    }
    function success(replayStatus, date) { return { type: cubeConstants.REPLAY_STATUS_FETCHED, data: replayStatus, date: date } }
}

function getAnalysis(collectionId, replayId) {
    return async dispatch => {
        // dispatch(request());
        try {
            let analysis = await cubeService.fetchAnalysis(collectionId, replayId);
            dispatch(success(analysis, Date.now()));
        } catch (error) {
            // dispatch(failure("Failed to getTestIds", Date.now()));
        }
    }
    function success(analysis, date) { return { type: cubeConstants.ANALYSIS_FETCHED, data: analysis, date: date } }
}

function getReport(collectionId, replayId) {
    return async dispatch => {
        // dispatch(request());
        try {
            let report = await cubeService.fetchReport(collectionId, replayId);
            dispatch(success(report, Date.now()));
        } catch (error) {
            // dispatch(failure("Failed to getTestIds", Date.now()));
        }
    }
    function success(analysis, date) { return { type: cubeConstants.REPORT_FETCHED, data: analysis, date: date } }
}
