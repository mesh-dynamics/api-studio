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

import { cubeConstants } from "../constants";
import { cubeService } from "../services";
import { processInstanceList } from "../utils/lib/common-utils";
import { httpClientActions } from "./httpClientActions";

export const cubeActions = {
    getApps,
    refreshAppList,
    getAppImages,
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
    // getAnalysis,
    // getReport,
    getTimelineData,
    hideServiceGraph,
    hideTestConfig,
    showTCSetup,
    showTCInfo,
    clearPreviousData,
    clearGolden,
    setPathResultsParams,
    getCollectionUpdateOperationSet,
    updateRecordingOperationSet,
    updateGoldenSet,
    pushToOperationSet,
    pushToMOS,
    pushToOperations,
    pushNewOperationKeyToOperations,
    addToRuleBook,
    addToDefaultRuleBook,
    removeFromRuleBook,
    removeFromNOS,
    removeFromOperations,
    getNewTemplateVerInfo,
    getJiraBugs,
    hideGoldenVisibility,
    clearPathResultsParams,
    clearTimeline,
    getAnalysisStatus,
    removeReplayFromTimeline,
    removeSelectedGoldenFromTestIds,
    hideHttpClient,
    resetCubeToInitialState,
    setDoNotShowGettingStartedAgain,
    setSelectedAppObject,
    setOtherInstanceEndPoint,
};

function clearPreviousData() {
    return async dispatch => {
        dispatch(clearPrev());
    };

    function clearPrev() {
        return { type: cubeConstants.CLEAR_PREVIOUS_DATA, data: null };
    }
}
async function getAppList(){
    let appsList = await cubeService.fetchAppsList();
    //This is temporary fix. We need to change all usage of these variables in UI first then remove following statement of copy to parent
    appsList.forEach(app=> {
        app.name = app.app.name;
        app.displayName = app.app.displayName;
        app.id = app.app.id;
        app.customer = app.app.customer;
        app.tracer = (app.configuration?.tracer || "meshd").toLowerCase() // default to meshd tracer
    });
    return appsList;
}

function getAppImages() {
    return async (dispatch, getState) => {
        try {
            let appImages = await cubeService.fetchAppsImages();
            dispatch(success(appImages, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to get Images", Date.now()));
        }
    };
    function success(appImages, date) { return { type: cubeConstants.APP_IMAGES_SUCCESS, data: appImages, date: date } }
    function failure(message, date) { return { type: cubeConstants.APP_IMAGES_FAILURE, err: message, date: date } }
}
function refreshAppList(){
    return async (dispatch, getState) => {
        const {cube: {selectedApp}} = getState()
        const appsList = await getAppList();
        dispatch({ type: cubeConstants.APPS_SUCCESS, data: appsList, date: Date.now() }); 
        dispatch(cubeActions.getAppImages());
        const selectedAppObj = appsList.find((app) => app.app.name==selectedApp)
        dispatch(cubeActions.setSelectedAppObject(selectedAppObj))
    }
}

function getApps () {
    return async (dispatch, getState) => {
        dispatch(request());
        try {
            const { selectedApp , selectedAppObj} = getState().cube;
            if(!selectedApp ||  !selectedAppObj || !selectedAppObj.app) {
                const appsList = await getAppList();
                dispatch(success(appsList, Date.now()));
                const defaultSelectedApp = appsList[0]
                dispatch(cubeActions.setSelectedApp(defaultSelectedApp.name));
                dispatch(cubeActions.getGraphDataByAppId(defaultSelectedApp.id));
                dispatch(cubeActions.getTimelineData(defaultSelectedApp.name));
                dispatch(cubeActions.getTestConfigByAppId(defaultSelectedApp.id));
                dispatch(cubeActions.getTestIds(defaultSelectedApp.name));
                dispatch(cubeActions.getAppImages());
            }
        } catch (error) {
            dispatch(failure("Failed to getApps", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.APPS_REQUEST }; }
    function success(appsList, date) { return { type: cubeConstants.APPS_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.APPS_FAILURE, err: message, date: date }; }
}

function pushToOperationSet(os, index) {
    return {type: cubeConstants.PUSH_TO_OS, data: {os: os, ind: index}};
}

function pushToMOS(obj) {
    return {type: cubeConstants.PUSH_TO_MOS, data: obj};
}

function clearGolden() {
    return {type: cubeConstants.CLEAR_GOLDEN, data: null};
}

function clearTimeline() {
    return {type: cubeConstants.CLEAR_TIMELINE, data: null};
}

function pushToOperations(operation, key) {
    return {type: cubeConstants.PUSH_TO_OPERATIONS, data: {op: operation, key}};
}

function pushNewOperationKeyToOperations(operation, key ) {
    return {type: cubeConstants.NEW_KEY_PUSH_TO_OPERATIONS, data: {op: operation, key}};
}

function addToRuleBook(key, value, templateMatchType) {
    return {type: cubeConstants.ADD_TO_RULE_BOOK, data: {key: key, val: value, templateMatchType }};
}

function addToDefaultRuleBook(key, value, templateMatchType) {
    return {type: cubeConstants.ADD_TO_DEFAULT_RULE_BOOK, data: {key: key, val: value, templateMatchType}}
}

function removeFromRuleBook(key) {
    return {type: cubeConstants.REMOVE_FROM_RULE_BOOK, data: key};
}

function removeFromNOS(index, length, indexMOS) {
    if (length > 1) {
        return {type: cubeConstants.REMOVE_FROM_OPERATION_GOLDEN, data: {index: index, indexMOS: indexMOS}};
    } else {
        return {type: cubeConstants.REMOVE_ENTIRE_OPERATIONS_GOLDEN_OBJ, data: {index: index, indexMOS: indexMOS}};
    }
}

function removeFromOperations(index, length, key) {
    if (length > 1) {
        return {type: cubeConstants.REMOVE_FROM_OPERATIONS, data: {index: index, key: key}};
    } else {
        return {type: cubeConstants.REMOVE_ENTIRE_OPERATIONS_OBJ, data: {index: index, key: key}};
    }
}


function getCollectionUpdateOperationSet(app) {
    return async (dispatch, getState) => {
        const { user: { customer_name: customerId } } = getState().authentication;
        try {
            let collectionUpdateOperationSetId = await cubeService.getCollectionUpdateOperationSet(app, customerId);
            dispatch(success(collectionUpdateOperationSetId, Date.now()));
        } catch (error) {
            console.error("Failed to getCollectionUpdateOperationSet", Date.now());
        }
    }

    function success(appsList, date) { return { type: cubeConstants.COLLECTION_UOS_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.COLLECTION_UOS_FAILURE, err: message, date: date }; }
}

function getNewTemplateVerInfo(app, currentTemplateVer) {
    return async (dispatch, getState) => {
        const { user: { customer_name: customerId } } = getState().authentication;
        try {
            let newTemplateVerInfo = await cubeService.getNewTemplateVerInfo(customerId, app, currentTemplateVer);
            dispatch(success(newTemplateVerInfo, Date.now()));
        } catch (error) {
            console.error("Failed to getNewTemplateVerInfo", Date.now());
        }
    }

    function success(appsList, date) { return { type: cubeConstants.TEMPLATE_VER_SUCCESS, data: appsList, date: date }; }
    function failure(message, date) { return { type: cubeConstants.TEMPLATE_VER_FAILURE, err: message, date: date }; }
}

function updateRecordingOperationSet() {
    return { type: cubeConstants.GOLDEN_REQUEST };
}

function updateGoldenSet(golden) {
    return { type: cubeConstants.NEW_GOLDEN_ADDED, data: golden };
}

function hideTestConfig(bool) {
    return {type: cubeConstants.HIDE_TEST_CONFIG, data: bool};
}

function hideHttpClient(bool) {
    return {type: cubeConstants.HIDE_HTTP_CLIENT, data: bool};
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

function hideGoldenVisibility(bool){
    return {type: cubeConstants.HIDE_GOLDEN_VISIBILITY, data: bool};
}

function getInstances () {
    return async dispatch => {
        dispatch(request());
        try {
            const instanceList = await cubeService.getInstanceList();
            const iList = processInstanceList(instanceList);
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
            dispatch(setForceCompleting(true))
            await cubeService.forceCompleteReplay(fcId);
            dispatch(success(fcId), Date.now());
            dispatch(setForceCompleting(false))
        } catch (error) {
            console.error(error);
        }
    }

    function success(fcId, date) { return { type: cubeConstants.CLEAR_FORCE_COMPLETE, data: fcId, date: date }; }
    function setForceCompleting(value) { return {type: cubeConstants.SET_FORCE_COMPLETING_REPLAY, data: value}}
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
            console.error(error);
            dispatch(cubeActions.setTestConfig(null));
            dispatch(failure("Failed to getTestConfigByAppId", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TEST_CONFIG } }
    function success(gd, date) { return { type: cubeConstants.TEST_CONFIG_SUCCESS, data: gd, date: date } }
    function failure(message, date) { return { type: cubeConstants.TEST_CONFIG_FAILURE, err: message, date: date } }
}

function setTestConfig(tc) {
    return {type: cubeConstants.SET_TEST_CONFIG, data: tc};
}

function setDoNotShowGettingStartedAgain(value) {
    return {type: cubeConstants.SET_GETTING_STARTED_SCREEN, data: value};
}

function setSelectedApp (app) {
    return (dispatch) => {
        dispatch({type: cubeConstants.SET_SELECTED_APP, data: app})
        
        // now fetch things that depend upon the app
        setTimeout(() => {
            dispatch(httpClientActions.fetchEnvironments());
            dispatch(httpClientActions.fetchMockConfigs())
            dispatch(httpClientActions.loadFromHistory());
            dispatch(httpClientActions.loadUserCollections());
            dispatch(httpClientActions.loadProtoDescriptor());
            dispatch(httpClientActions.addAllUserCollections([]));
        });
    }
}

function setSelectedAppObject (appObject) {
    return (dispatch) => {
        dispatch({type: cubeConstants.SET_SELECTED_APP_OBJECT, data: appObject})
    }
}

function setSelectedInstance ( instance ) {
    return {type: cubeConstants.SET_SELECTED_INSTANCE, data: instance}
}

function setPathResultsParams ( pathResultsParams ) {
    return {type: cubeConstants.SET_PATH_RESULTS_PARAMS, data: pathResultsParams}
}

function clearPathResultsParams () {
    return {type: cubeConstants.CLEAR_PATH_RESULTS_PARAMS }
}

function setGolden ( golden ) {
    return {type: cubeConstants.SET_GOLDEN, data: golden}
}

function setJiraBugs( jiraBugs ) {
    return {type: cubeConstants.SET_JIRA_BUGS, data: jiraBugs}
}

function getTestIds (app) {
    return async (dispatch, getState) => {
        const { user } = getState().authentication;
        dispatch(request());
        try {
            const data = await cubeService.fetchCollectionList(user, app, "Golden");
            const collections = data.recordings;
            dispatch(success(collections, Date.now()));
        } catch (error) {
            dispatch(failure("Failed to getTestIds", Date.now()));
        }
    };

    function request() { return { type: cubeConstants.TESTIDS_REQUEST } }
    function success(collections, date) { return { type: cubeConstants.TESTIDS_SUCCESS, data: collections, date: date } }
    function failure(message, date) { return { type: cubeConstants.TESTIDS_FAILURE, err: message, date: date } }
}

function setSelectedTestIdAndVersion ( testIdLabel, version, golden, name ) {
    return {type: cubeConstants.SET_SELECTED_TESTID, data: {collec: testIdLabel, ver: version, golden: golden, name: name}};
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

function getTimelineData(app = "Cube", userId = "ALL", endDate = new Date(), startDate = null, clearTimeline = false) {
    return async (dispatch, getState) => {
        const { authentication : { user } } = getState();
        try {
            let timeline = await cubeService.fetchTimelineData(user, app, userId, endDate, startDate);
            if(clearTimeline){
                dispatch(cubeActions.clearTimeline())
            };
            dispatch(success(timeline, Date.now()));
        } catch (error) {
            console.log("Error Fetching Time line data", error);
        }
    };

    function success(timeline, date) { return { type: cubeConstants.TIMELINE_DATA_SUCCESS, data: timeline, date: date } }
}

function clearReplayStatus() {
    return {type: cubeConstants.CLEAR_REPLAY_STATUS, data: null};
}

function getReplayStatus(replayId, isLocalReplay) {
    return async dispatch => {
        try {
            dispatch({type: cubeConstants.FETCHING_REPLAY_STATUS, data: true})
            let replayStatus = await cubeService.checkStatusForReplay(replayId, isLocalReplay);
            dispatch(success(replayStatus, Date.now()));
        } catch (error) {
            console.error("Error getting replay status: " + error);
        }
        dispatch({type: cubeConstants.FETCHING_REPLAY_STATUS, data: false})
    }
    function success(replayStatus, date) { return { type: cubeConstants.REPLAY_STATUS_FETCHED, data: replayStatus, date: date } }
}

function getAnalysisStatus(replayId, app) {
    return async dispatch => {
        try {
            dispatch({type: cubeConstants.FETCHING_ANALYSIS_STATUS, data: true})
            const analysisStatus = await cubeService.fetchAnalysisStatus(replayId);
            dispatch(success(analysisStatus.data, Date.now()));
        } catch (error) {
            console.error("Error getting analysis status: " + error);
        }
        dispatch({type: cubeConstants.FETCHING_ANALYSIS_STATUS, data: false})
    }
    function success(analysisStatus, date) { return { type: cubeConstants.ANALYSIS_STATUS_FETCHED, data: analysisStatus, date: date } }
}

function getJiraBugs(replayId, apiPath) {
    return async dispatch => {
        try {
            let jiraBugs =  await cubeService.fetchJiraBugData(replayId, apiPath);
            dispatch(setJiraBugs(jiraBugs));
        } catch (error) {
            console.log("Error caught in fetch", error);
        }
    }
}

function removeReplayFromTimeline(replayId) {
    return {type: cubeConstants.REMOVE_REPLAY_FROM_TIMELINE, data: replayId};
}

function removeSelectedGoldenFromTestIds (selectedGolden) {
    return {type: cubeConstants.REMOVE_SELECTED_GOLDEN_FROM_TESTIDS, data: selectedGolden};
}

function resetCubeToInitialState () {
    return {type: cubeConstants.RESET_CUBE_TO_INITIAL_STATE}
}

function setOtherInstanceEndPoint(value) {
    return {type: cubeConstants.SET_OTHER_INSTANCE_END_POINT, data: value}
}
/**
 * Doesn't look like these are being used
 */
// function getAnalysis(collectionId, replayId, app) {
//     return async dispatch => {
//         try {
//             let analysis = await cubeService.fetchAnalysis(collectionId, replayId);
//             dispatch(success(analysis, Date.now()));
//             //dispatch(cubeActions.clearTimeline());
//             //dispatch(cubeActions.getTimelineData(app));
//         } catch (error) {
//         }
//     }
//     function success(analysis, date) { return { type: cubeConstants.ANALYSIS_FETCHED, data: analysis, date: date } }
// }

// function getReport(collectionId, replayId) {
//     return async dispatch => {
//         try {
//             let report = await cubeService.fetchReport(collectionId, replayId);
//             dispatch(success(report, Date.now()));
//         } catch (error) {
//         }
//     }
//     function success(analysis, date) { return { type: cubeConstants.REPORT_FETCHED, data: analysis, date: date } }
// }
