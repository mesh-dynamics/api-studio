import { cubeConstants } from '../constants';
import { cubeService } from '../services'

export const cubeActions = {
    getApps,
    setSelectedApp,
    getInstances,
    getTestIds,
    setGateway,
    setSelectedTestId,
    setSelectedInstance,
    getReplayId,
    getGraphData,
    startReplay,
    getReplayStatus,
    getAnalysis,
    getReport,
    getTimelineData,
    getDiffData,
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

function getInstances () {
    return async dispatch => {
        dispatch(request());
        try {
            let iList = await cubeService.getInstanceList();
            dispatch(success(iList, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getInstanceList", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST }; }
    function success(iList, date) { return { type: cubeConstants.INSTANCE_SUCCESS, data: iList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date }; }
}

function setGateway ( gateway ) {
    return {type: cubeConstants.SET_GATEWAY, data: gateway}
}

function setSelectedApp ( appLabel ) {
    return {type: cubeConstants.SET_SELECTED_APP, data: appLabel}
}

function setSelectedInstance ( instance ) {
    return {type: cubeConstants.SET_SELECTED_INSTANCE, data: instance}
}

function getDiffData(replayId, recordReqId, replayReqId) {
    return async dispatch => {
        try {
            let diffData = await cubeService.getDiffData(replayId, recordReqId, replayReqId);
            dispatch(success(diffData, Date.now()));
        } catch (error) {
        }
    };

    function success(diffData, date) { return { type: cubeConstants.DIFF_SUCCESS, data: diffData, date: date }; }
}

function getTestIds (app) {
    return async dispatch => {
        dispatch(request());
        try {
            let collections = await cubeService.fetchCollectionList(app);
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

function getGraphData (app) {
    return async dispatch => {
        dispatch(request());
        try {
            let gd = await cubeService.getGraphData(app);
            dispatch(success(gd, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getGraphData", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.GRAPH_REQUEST } }
    function success(gd, date) { return { type: cubeConstants.GRAPH_REQUEST_SUCCESS, data: gd, date: date } }
    function failure(message, date) { return { type: cubeConstants.GRAPH_REQUEST_FAILURE, err: message, date: date } }
}

function getReplayId(testIdLabel, app, instance) {
    return async dispatch => {
        try {
            let replayId = await cubeService.getReplayId(testIdLabel, app, instance);
            dispatch(success(replayId, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getReplayId", Date.now()));
        }
    };

    function success(replayId, date) { return { type: cubeConstants.REPLAY_ID_SUCCESS, data: replayId, date: date } }
    function failure(message, date) { return { type: cubeConstants.TESTIDS_FAILURE, err: message, date: date } }
}

function getTimelineData(collectionId, replayData) {
    return async dispatch => {
        try {
            let timeline = await cubeService.fetchTimelineData(collectionId, replayData);
            dispatch(success(timeline, Date.now()));
        } catch (error) {

        }
    };

    function success(timeline, date) { return { type: cubeConstants.TIMELINE_DATA_SUCCESS, data: timeline, date: date } }
}

function startReplay(collectionId, replayId, app) {
    return async dispatch => {
        try {
            dispatch(success(Date.now()));
            let startReplay = await cubeService.startReplay(collectionId, replayId, app);
            dispatch(success(replayId, Date.now()));
        } catch (error) {
        }
    }
    function success(date) { return { type: cubeConstants.REPLAY_STARTED, date: date } }
}

function getReplayStatus(collectionId, replayId, app) {
    return async dispatch => {
        try {
            let replayStatus = await cubeService.checkStatusForReplay(collectionId, replayId, app);
            dispatch(success(replayStatus, Date.now()));
        } catch (error) {
        }
    }
    function success(replayStatus, date) { return { type: cubeConstants.REPLAY_STATUS_FETCHED, data: replayStatus, date: date } }
}

function getAnalysis(collectionId, replayId) {
    return async dispatch => {
        try {
            let analysis = await cubeService.fetchAnalysis(collectionId, replayId);
            dispatch(success(analysis, Date.now()));
        } catch (error) {
        }
    }
    function success(analysis, date) { return { type: cubeConstants.ANALYSIS_FETCHED, data: analysis, date: date } }
}

function getReport(collectionId, replayId) {
    return async dispatch => {
        try {
            let report = await cubeService.fetchReport(collectionId, replayId);
            dispatch(success(report, Date.now()));
        } catch (error) {
        }
    }
    function success(analysis, date) { return { type: cubeConstants.REPORT_FETCHED, data: analysis, date: date } }
}
