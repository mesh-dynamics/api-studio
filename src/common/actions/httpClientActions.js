import { cubeService } from "../services";
import { httpClientConstants } from "../constants/httpClientConstants";
import _ from "lodash";
import arrayToTree from 'array-to-tree';
import {setDefaultMockContext} from '../helpers/httpClientHelpers'

export const httpClientActions = {
    deleteParamInSelectedOutgoingTab: (tabId, type, id) => {
        return {type: httpClientConstants.DELETE_PARAM_IN_OUTGOING_TAB, data: {tabId, type, id}};
    },

    addParamToSelectedOutgoingTab: (tabId, type) => {
        return {type: httpClientConstants.ADD_PARAM_TO_OUTGOING_TAB, data: {tabId, type}};
    },

    deleteParamInSelectedTab: (tabId, type, id) => {
        return {type: httpClientConstants.DELETE_PARAM_IN_TAB, data: {tabId, type, id}};
    },

    addParamToSelectedTab: (tabId, type) => {
        return {type: httpClientConstants.ADD_PARAM_TO_TAB, data: {tabId, type}};
    },

    updateParamInSelectedOutgoingTab: (tabId, type, key, value, id) => {
        return {type: httpClientConstants.UPDATE_PARAM_IN_OUTGOING_TAB, data: {tabId, type, key, value, id}};
    },

    updateParamInSelectedTab: (tabId, type, key, value, id) => {
        return {type: httpClientConstants.UPDATE_PARAM_IN_TAB, data: {tabId, type, key, value, id}};
    },

    updateAllParamsInSelectedOutgoingTab: (tabId, type, key, value) => {
        return {type: httpClientConstants.UPDATE_ALL_PARAMS_IN_OUTGOING_TAB, data: {tabId, type, key, value}};
    },

    updateAllParamsInSelectedTab: (tabId, type, key, value) => {
        return {type: httpClientConstants.UPDATE_ALL_PARAMS_IN_TAB, data: {tabId, type, key, value}};
    },

    updateBodyOrRawDataTypeInOutgoingTab: (tabId, type, value) => {
        return {type: httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB, data: {tabId, type, value}};
    },

    updateBodyOrRawDataTypeInTab: (tabId, type, value) => {
        return {type: httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB, data: {tabId, type, value}};
    },

    preDriveRequest: (tabId, responseStatus, showCompleteDiff) => {
        return {type: httpClientConstants.PRE_DRIVE_REQUEST, data: {tabId, responseStatus, showCompleteDiff}}; 
    },

    postSuccessDriveRequest: (tabId, responseStatus, responseStatusText, responseHeaders, responseBody) => {
        return {type: httpClientConstants.POST_SUCCESS_DRIVE_REQUEST, data: {tabId, responseStatus, responseStatusText, responseHeaders, responseBody}}; 
    },

    postErrorDriveRequest: (tabId, responseStatus) => {
        return {type: httpClientConstants.POST_ERROR_DRIVE_REQUEST, data: {tabId, responseStatus}}; 
    },

    addTab: (tabId, reqObject, app, selectedTabKey, tabName) => {
        return {type: httpClientConstants.ADD_TAB, data: {tabId, reqObject, app, selectedTabKey, tabName}}; 
    },

    addOutgoingRequestsToTab: (tabId, outgoingRequests) => {
        return {type: httpClientConstants.ADD_OUTGOING_REQUESTS_TO_TAB, data: {tabId, outgoingRequests}};
    },

    addUserHistoryCollection: (userHistoryCollection) => {
        return {type: httpClientConstants.ADD_USER_HISTORY_COLLECTION, data: {userHistoryCollection}};
    },

    addCubeRunHistory: (apiTraces, cubeRunHistory) => {
        return {type: httpClientConstants.ADD_CUBE_RUN_HISTORY, data: {apiTraces, cubeRunHistory}};
    },
    deleteCubeRunHistory: (userCollectionId) => {
        return {type: httpClientConstants.DELETE_CUBE_RUN_HISTORY, data: userCollectionId};
    },

    addUserCollections: (userCollections) => {
        return {type: httpClientConstants.ADD_USER_COLLECTIONS, data: {userCollections}};
    },
    deleteUserCollection: (userCollectionId) => {
        return {type: httpClientConstants.DELETE_USER_COLLECTION, data: userCollectionId};
    },
    postSuccessSaveToCollection: (tabId, showSaveModal, modalErroSaveMessage, clearIntervalHandle) => {
        return {type: httpClientConstants.POST_SUCCESS_SAVE_TO_COLLECTION, data: {tabId, showSaveModal, modalErroSaveMessage, clearIntervalHandle}};
    },

    postErrorSaveToCollection: (showSaveModal, modalErroSaveMessage) => {
        return {type: httpClientConstants.POST_ERROR_SAVE_TO_COLLECTION, data: {showSaveModal, modalErroSaveMessage}};
    },

    catchErrorSaveToCollection: (showSaveModal, modalErroSaveMessage) => {
        return {type: httpClientConstants.CATCH_ERROR_SAVE_TO_COLLECTION, data: {showSaveModal, modalErroSaveMessage}};
    },

    postSuccessLoadRecordedHistory: (tabId, recordedHistory) => {
        return {type: httpClientConstants.POST_SUCCESS_LOAD_RECORDED_HISTORY, data: {tabId, recordedHistory}};
    },

    closeSaveModal: (showSaveModal) => {
        return {type: httpClientConstants.CLOSE_SAVE_MODAL, data: {showSaveModal}};
    },

    showSaveModal: (selectedSaveableTabId, showSaveModal, collectionName, collectionLabel, modalErroSaveMessage,modalErroSaveMessageIsError, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.SHOW_SAVE_MODAL, data: {selectedSaveableTabId, showSaveModal, collectionName, collectionLabel, modalErroSaveMessage,modalErroSaveMessageIsError, modalErroCreateCollectionMessage}};
    },

    setInactiveHistoryCursor: (historyCursor, active) => {
        return {type: httpClientConstants.SET_INACTIVE_HISTORY_CURSOR, data: {historyCursor, active}};
    },

    setActiveHistoryCursor: (historyCursor) => {
        return {type: httpClientConstants.SET_ACTIVE_HISTORY_CURSOR, data: {historyCursor}};
    },

    postSuccessCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.POST_SUCCESS_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    postErrorCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.POST_ERROR_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    catchErrorCreateCollection: (showSaveModal, modalErroCreateCollectionMessage) => {
        return {type: httpClientConstants.CATCH_ERROR_CREATE_COLLECTION, data: {showSaveModal, modalErroCreateCollectionMessage}};
    },

    setSelectedTabKey: (selectedTabKey) => {
        return {type: httpClientConstants.SET_SELECTED_TAB_KEY, data: {selectedTabKey}};
    },

    removeTab: (tabs, selectedTabKey) => {
        return {type: httpClientConstants.REMOVE_TAB, data: {tabs, selectedTabKey}};
    },

    setUpdatedModalUserCollectionDetails: (name, value) => {
        return {type: httpClientConstants.SET_UPDATED_MODAL_USER_COLLECTION_DETAILS, data: {name, value}};
    },

    setAsReference: (tabId, tab) => {
        return {type: httpClientConstants.SET_AS_REFERENCE, data: {tabId, tab}};
    },

    closeAddMockReqModal: (selectedTabIdToAddMockReq, showAddMockReqModal, mockReqServiceName, mockReqApiPath, modalErrorAddMockReqMessage) => {
        return {type: httpClientConstants.CLOSE_ADD_MOCK_REQ_MODAL, data: {selectedTabIdToAddMockReq, showAddMockReqModal, mockReqServiceName, mockReqApiPath, modalErrorAddMockReqMessage}};
    },

    setUpdatedModalMockReqDetails: (name, value) => {
        return {type: httpClientConstants.SET_UPDATED_MODAL_MOCK_REQ_DETAILS, data: {name, value}};
    },

    showAddMockReqModal: (selectedTabIdToAddMockReq, showAddMockReqModal, mockReqServiceName, mockReqApiPath, modalErrorAddMockReqMessage) => {
        return {type: httpClientConstants.SHOW_ADD_MOCK_REQ_MODAL, data: {selectedTabIdToAddMockReq, showAddMockReqModal, mockReqServiceName, mockReqApiPath, modalErrorAddMockReqMessage}};
    },

    setSelectedTraceTableReqTabId: (selectedTraceTableReqTabId, tabId) => {
        return {type: httpClientConstants.SET_SELECTED_TRACE_TABLE_REQ_TAB, data: {selectedTraceTableReqTabId, tabId}};
    },
    
    setSelectedTraceTableTestReqId: (selectedTraceTableTestReqTabId, tabId) => {
        return{type: httpClientConstants.SET_SELECTED_TRACE_TABLE_TEST_REQ_TAB, data: {selectedTraceTableTestReqTabId, tabId}};
    },

    setEnvironmentList: (environmentList) => ({type: httpClientConstants.SET_ENVIRONMENT_LIST, data: environmentList}),

    fetchEnvironments: () => async (dispatch) => {
        dispatch(httpClientActions.setEnvStatusText("Loading..."))
        try {
            const environmentList = await cubeService.getAllEnvironments();
            dispatch(httpClientActions.setEnvironmentList(environmentList))
            dispatch(httpClientActions.resetEnvStatusText())
        } catch (e) {
            dispatch(httpClientActions.setEnvStatusText(e.response.data.message, true))
        }  
    },

    saveEnvironment: (environment) => async (dispatch) => {
        dispatch(httpClientActions.setEnvStatusText("Saving..."))
        try {
            await cubeService.insertNewEnvironment(environment);
            dispatch(httpClientActions.fetchEnvironments())
            dispatch(httpClientActions.resetEnvStatusText())
            dispatch(httpClientActions.showEnvList(true));
        } catch (e) {
            dispatch(httpClientActions.setEnvStatusText(e.response.data.message, true))
        } 
    },

    updateEnvironment: (environment) => async (dispatch) => {
        dispatch(httpClientActions.setEnvStatusText("Updating..."))
        try {
            await cubeService.updateEnvironment(environment);
            dispatch(httpClientActions.fetchEnvironments())
            dispatch(httpClientActions.resetEnvStatusText())
            dispatch(httpClientActions.showEnvList(true));
        } catch (e) {
            dispatch(httpClientActions.setEnvStatusText(e.response.data.message, true))
        } 
    },

    removeEnvironment: (id, name) => async (dispatch, getState) => {
        const {httpClient: {selectedEnvironment}} = getState();
        dispatch(httpClientActions.setEnvStatusText("Removing..."))
        try {
            await cubeService.deleteEnvironment(id)
            if (name === selectedEnvironment) {
                dispatch(httpClientActions.setSelectedEnvironment(""))
            }
            dispatch(httpClientActions.fetchEnvironments())
        } catch (e) {
            dispatch(httpClientActions.setEnvStatusText(e.response.data.message, true))
        }
    },

    setEnvStatusText: (text, isError=false) => ({type: httpClientConstants.SET_ENV_STATUS_TEXT, data: {text, isError}}),

    resetEnvStatusText: () => ({type: httpClientConstants.RESET_ENV_STATUS_TEXT}),

    showEnvList: (show) => ({type: httpClientConstants.SHOW_ENV_LIST, data: show}),

    setSelectedEnvironment: (selectedEnvironment) => ({type: httpClientConstants.SET_SELECTED_ENVIRONMENT, data: selectedEnvironment}),

    resetRunState: (tabId) => ({type: httpClientConstants.RESET_RUN_STATE, data: {tabId}}),

    setReqRunning: (tabId) => ({type: httpClientConstants.SET_REQUEST_RUNNING, data: {tabId}}),

    unsetReqRunning: (tabId) => (dispatch) => {
        console.log("Resetting mock context");
        setDefaultMockContext()
        dispatch({type: httpClientConstants.UNSET_REQUEST_RUNNING, data: {tabId}})
    },

    createDuplicateTab: (tabId) => ({type: httpClientConstants.CREATE_DUPLICATE_TAB, data: {tabId}}),

    toggleShowTrace: (tabId) => ({type: httpClientConstants.TOGGLE_SHOW_TRACE, data: {tabId}}),

    loadFromHistory: () => async (dispatch, getState) => {
        const { cube: {selectedApp} } = getState();
        let app = selectedApp;
        try {
            const serverRes = await cubeService.fetchCollectionList(app, "History", true)
            const {httpClient: {userHistoryCollection}} = getState();
            const fetchedUserHistoryCollection = serverRes.find((eachCollection) => (eachCollection.recordingType === "History"))

            if(!fetchedUserHistoryCollection) {
                throw new Error("User history collection not present")
            }

            dispatch(httpClientActions.addUserHistoryCollection(fetchedUserHistoryCollection));

            const startTime = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
            const res = await cubeService.fetchAPITraceData(app, startTime, null, null, null, null, null, fetchedUserHistoryCollection.collec, 100)
            const apiTraces = res.response;
            const cubeRunHistory = {};
            apiTraces.sort((a, b) => {
                return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
            });
            apiTraces.forEach((eachApiTrace) => {
                const timeStamp = eachApiTrace.res[0].reqTimestamp,
                    objectKey = new Date(timeStamp * 1000).toDateString();
                eachApiTrace.res.map((eachApiTraceEvent) => {
                    eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                    eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                    eachApiTraceEvent["toggled"] = false;
                    eachApiTraceEvent["recordingIdAddedFromClient"] = fetchedUserHistoryCollection.id;
                    eachApiTraceEvent["traceIdAddedFromClient"] = eachApiTrace.traceId;
                    eachApiTraceEvent["collectionIdAddedFromClient"] = eachApiTrace.collection;
                });

                if (objectKey in cubeRunHistory) {
                    const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                        customID: "spanId", parentProperty: "parentSpanId"
                    });
                    cubeRunHistory[objectKey].push({
                        ...apiFlatArrayToTree[0]
                    });
                } else {
                    cubeRunHistory[objectKey] = [];
                    const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                        customID: "spanId", parentProperty: "parentSpanId"
                    });
                    cubeRunHistory[objectKey].push({
                        ...apiFlatArrayToTree[0]
                    });
                }
            });
            dispatch(httpClientActions.addCubeRunHistory(apiTraces, cubeRunHistory));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error", error);
        }
    },

    loadUserCollections: () => async (dispatch, getState) => {
        const { cube: {selectedApp} } = getState();
        let app = selectedApp;
        try {
            const serverRes = await cubeService.fetchCollectionList(app, "UserGolden", true)
            const userCollections = serverRes.filter((eachCollection) => (eachCollection.recordingType !== "History"))
            dispatch(httpClientActions.addUserCollections(userCollections));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },

    // mock config

    setMockConfigList: (mockConfigList) => ({type: httpClientConstants.SET_MOCK_CONFIG_LIST, data: mockConfigList}),

    setMockConfigStatusText: (text, isError=false) => ({type: httpClientConstants.SET_MOCK_CONFIG_STATUS_TEXT, data: {text, isError}}),

    resetMockConfigStatusText: () => ({type: httpClientConstants.RESET_MOCK_CONFIG_STATUS_TEXT}),

    showMockConfigList: (show) => ({type: httpClientConstants.SHOW_MOCK_CONFIG_LIST, data: show}),

    setSelectedMockConfig: (selectedMockConfig) => ({type: httpClientConstants.SET_SELECTED_MOCK_CONFIG, data: selectedMockConfig}),

    fetchMockConfigs: () => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Loading..."))
        const state = getState();
        const { selectedApp } = state.cube;
        try {
            const mockConfigList = await cubeService.getAllMockConfigs(selectedApp);
            dispatch(httpClientActions.setMockConfigList(mockConfigList))
            dispatch(httpClientActions.resetMockConfigStatusText())
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response?.data.message, true))
        }  
    },

    saveMockConfig: (mockConfig) => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Saving..."))
        const state = getState();
        const { selectedApp } = state.cube;
        try {
            await cubeService.insertNewMockConfig(selectedApp, mockConfig);
            dispatch(httpClientActions.fetchMockConfigs())
            dispatch(httpClientActions.resetMockConfigStatusText())
            dispatch(httpClientActions.showMockConfigList(true));
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response.data.message, true))
        } 
    },

    updateMockConfig: (id, mockConfig) => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Updating..."))
        const state = getState();
        const { selectedApp } = state.cube;
        try {
            await cubeService.updateMockConfig(selectedApp, id, mockConfig);
            dispatch(httpClientActions.fetchMockConfigs())
            dispatch(httpClientActions.resetMockConfigStatusText())
            dispatch(httpClientActions.showMockConfigList(true));
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response.data.message, true))
        } 
    },

    removeMockConfig: (id, name) => async (dispatch, getState) => {
        const {httpClient: {selectedMockConfig}} = getState()
        dispatch(httpClientActions.setMockConfigStatusText("Removing..."))
        try {
            await cubeService.deleteMockConfig(id)
            if (name === selectedMockConfig) {
                dispatch(httpClientActions.setSelectedMockConfig(""))
            }
            dispatch(httpClientActions.fetchMockConfigs())
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response.data.message, true))
        }
    },

}