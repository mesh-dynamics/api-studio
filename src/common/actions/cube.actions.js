import { cubeConstants } from '../constants';
import { cubeService } from '../services'

export const cubeActions = {
    getApps,
    setSelectedApp,
    getInstances,
    getTestIds,
    setGolden,
    getTestConfigByAppId,
    setTestConfig,
    setSelectedTestIdAndVersion,
    setSelectedInstance,
    forceCompleteReplay,
    getGraphData,
    getGraphDataByAppId,
    getReplayStatus,
    clearReplayStatus,
    getAnalysis,
    getReport,
    getTimelineData,
    hideServiceGraph,
    hideTestConfig,
    showTCSetup,
    showTCInfo,
    clear,
    clearGolden,
    setPathResultsParams,
    getCollectionUpdateOperationSet,
    updateRecordingOperationSet,
    updateGoldenSet,
    pushToOperationSet,
    pushToOperations,
    removeFromNOS,
    removeFromOperations,
    getNewTemplateVerInfo,
    updateTemplateOperationSet,
    createJiraIssue,
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
            dispatch(cubeActions.setSelectedApp(appsList[0].name));
            dispatch(cubeActions.getGraphDataByAppId(appsList[0].id));
            dispatch(cubeActions.getTimelineData(appsList[0].name));
            dispatch(cubeActions.getTestConfigByAppId(appsList[0].id));
            dispatch(cubeActions.getTestIds(appsList[0].name));
        } catch (error) {
            dispatch(failure("Failed to getApps", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST }; }
    function success(appsList, date) { return { type: cubeConstants.APPS_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date }; }
}

function pushToOperationSet(os) {
    return {type: cubeConstants.PUSH_TO_OS, data: os};
}

function clearGolden() {
    return {type: cubeConstants.CLEAR_GOLDEN, data: null};
}

function pushToOperations(o) {
    return {type: cubeConstants.PUSH_TO_OPERATIONS, data: o};
}

function removeFromNOS(index) {
    return {type: cubeConstants.REMOVE_FROM_OPERATIONSETS, data: index};
}

function removeFromOperations(index) {
    return {type: cubeConstants.REMOVE_FROM_OPERATIONS, data: index};
}

function updateTemplateOperationSet(templateVer, body) {
    return async dispatch => {
        try {
            let updateTOS = await cubeService.updateTemplateOperationSet(templateVer, body);
            dispatch(success(updateTOS, Date.now()));
        } catch (error) {
            console.error("Failed to getCollectionUpdateOperationSet", Date.now());
        }
    }

    function success(updateTOS, date) { return { type: cubeConstants.UPDATE_TOS_SUCCESS, data: updateTOS, date: date }; }
    function failure(message, date) { return { type: cubeConstants.COLLECTION_UOS_FAILURE, err: message, date: date }; }
}

function getCollectionUpdateOperationSet(app) {
    return async dispatch => {
        try {
            let collectionUpdateOperationSetId = await cubeService.getCollectionUpdateOperationSet(app);
            dispatch(success(collectionUpdateOperationSetId, Date.now()));
        } catch (error) {
            console.error("Failed to getCollectionUpdateOperationSet", Date.now());
        }
    }

    function success(appsList, date) { return { type: cubeConstants.COLLECTION_UOS_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.COLLECTION_UOS_FAILURE, err: message, date: date }; }
}

function getNewTemplateVerInfo(app, currentTemplateVer) {
    return async dispatch => {
        try {
            let newTemplateVerInfo = await cubeService.getNewTemplateVerInfo(app, currentTemplateVer);
            dispatch(success(newTemplateVerInfo, Date.now()));
        } catch (error) {
            console.error("Failed to getNewTemplateVerInfo", Date.now());
        }
    }

    function success(appsList, date) { return { type: cubeConstants.TEMPLATE_VER_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.TEMPLATE_VER_FAILURE, err: message, date: date }; }
}

function updateRecordingOperationSet(rosData, replayId, collectionUpdOpSetId, templateVer, recordingId, app) {
    return async dispatch => {
        dispatch(request());
        try {
            let updateRes = await cubeService.updateRecordingOperationSet(rosData);
            dispatch(cubeActions.updateGoldenSet(replayId, collectionUpdOpSetId, templateVer, recordingId, app));
        } catch (error) {
            console.error("Failed to updateRecordingOperationSet", Date.now());
        }
    }
    function request() { return { type: cubeConstants.GOLDEN_REQUEST }; }
}

function updateGoldenSet(replayId, collectionUpdOpSetId, templateVer, recordingId, app) {
    return async dispatch => {
        try {
            let updateRes = await cubeService.updateGoldenSet(replayId, collectionUpdOpSetId, templateVer, recordingId);
            dispatch(success(updateRes, Date.now()));
            dispatch(cubeActions.getTestIds(app));
        } catch (error) {
            console.error("Failed to updateGoldenSet", Date.now());
            dispatch(cubeActions.clearGolden());
            alert("Unable to update golden set");
        }
    }
    function success(updateRes, date) { return { type: cubeConstants.NEW_GOLDEN_ADDED, data: updateRes, date: date }; }
}

function hideTestConfig(bool) {
    return {type: cubeConstants.HIDE_TEST_CONFIG, data: bool};
}

function showTCSetup(bool) {
    return {type: cubeConstants.TEST_CONFIG_SETUP, data: bool};
}

function showTCInfo(bool) {
    return {type: cubeConstants.TEST_CONFIG_VIEW, data: bool};
}

function hideServiceGraph(bool) {
    return {type: cubeConstants.REPLAY_VIEW, data: bool};
}

function getInstances () {
    return async dispatch => {
        dispatch(request());
        try {
            let iList = await cubeService.getInstanceList();
            let il = iList.map(item => {
                item.name = item.name.toLowerCase();
                return item;
            });
            dispatch(success(il, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getInstanceList", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST }; }
    function success(iList, date) { return { type: cubeConstants.INSTANCE_SUCCESS, data: iList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date }; }
}

function forceCompleteReplay(fcId) {
    return async dispatch => {
        try {
            let isComplete = await cubeService.forceCompleteReplay(fcId);
            dispatch(success(fcId), Date.now());
        } catch (error) {
            console.error(error);
        }
    }

    function success(fcId, date) { return { type: cubeConstants.CLEAR_FORCE_COMPLETE, data: fcId, date: date }; }
}

function getTestConfigByAppId(appId) {
    return async dispatch => {
        try {
            let gd = await cubeService.getTestConfigByAppId(appId);
            for (const gtc of gd) {
                gtc.paths = gtc.testPaths;
                gtc.mocks = gtc.testMockServices;
            }
            dispatch(success(gd, Date.now()));
            dispatch(cubeActions.setTestConfig(gd[0]));
        } catch (error) {
            dispatch(failure("Failed to getTestConfig", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TEST_CONFIG } }
    function success(gd, date) { return { type: cubeConstants.TEST_CONFIG_SUCCESS, data: gd, date: date } }
    function failure(message, date) { return { type: cubeConstants.TEST_CONFIG_FAILURE, err: message, date: date } }
}

function setTestConfig(tc) {
    return {type: cubeConstants.SET_TEST_CONFIG, data: tc};
}

function setSelectedApp ( appLabel ) {
    return {type: cubeConstants.SET_SELECTED_APP, data: appLabel}
}

function setSelectedInstance ( instance ) {
    return {type: cubeConstants.SET_SELECTED_INSTANCE, data: instance}
}

function setPathResultsParams ( pathResultsParams ) {
    return {type: cubeConstants.SET_PATH_RESULTS_PARAMS, data: pathResultsParams}
}

function setGolden ( golden ) {
    return {type: cubeConstants.SET_GOLDEN, data: golden}
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

function setSelectedTestIdAndVersion ( testIdLabel, version, golden ) {
    return {type: cubeConstants.SET_SELECTED_TESTID, data: {collec: testIdLabel, ver: version, golden: golden}};
}

function getGraphDataByAppId(appId) {
    return async dispatch => {
        dispatch(request());
        try {
            let gd = await cubeService.getGraphDataByAppId(appId);
            dispatch(success(gd, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getGraphData", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.GRAPH_REQUEST } }
    function success(gd, date) { return { type: cubeConstants.GRAPH_REQUEST_SUCCESS, data: gd, date: date } }
    function failure(message, date) { return { type: cubeConstants.GRAPH_REQUEST_FAILURE, err: message, date: date } }
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

function getTimelineData(app = 'Cube', userId = 'ALL', endDate = new Date()) {
    return async dispatch => {
        try {
            let timeline = await cubeService.fetchTimelineData(app, userId, endDate);
            dispatch(success(timeline, Date.now()));
        } catch (error) {

        }
    };

    function success(timeline, date) { return { type: cubeConstants.TIMELINE_DATA_SUCCESS, data: timeline, date: date } }
}

function clearReplayStatus() {
    return {type: cubeConstants.CLEAR_REPLAY_STATUS, data: null};
}

function getReplayStatus(collectionId, replayId, app) {
    return async dispatch => {
        try {
            let replayStatus = await cubeService.checkStatusForReplay(collectionId, replayId, app);
            dispatch(success(replayStatus, Date.now()));
            if (replayStatus && (replayStatus.status == 'Completed' || replayStatus.status == 'Error')) {
                dispatch(cubeActions.getAnalysis(replayStatus.collection, replayStatus.replayId, replayStatus.app));
            }
        } catch (error) {
        }
    }
    function success(replayStatus, date) { return { type: cubeConstants.REPLAY_STATUS_FETCHED, data: replayStatus, date: date } }
}

function getAnalysis(collectionId, replayId, app) {
    return async dispatch => {
        try {
            let analysis = await cubeService.fetchAnalysis(collectionId, replayId);
            dispatch(success(analysis, Date.now()));
            dispatch(cubeActions.getTimelineData(app));
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

function createJiraIssue() {
    return async dispatch => {
        try {
            let response = await cubeService.createJiraIssue();
            dispatch(success(response, Date.now()));
        } catch (error) {
        }
    }
    // TODO
    function success(resp, date) { return { type: cubeConstants.REPORT_FETCHED, data: resp, date: date } }
}
