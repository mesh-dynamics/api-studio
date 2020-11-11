import { cubeService } from "../services";
import { httpClientConstants } from "../constants/httpClientConstants";
import _ from "lodash";
import { getDefaultTraceApiFilters } from "../utils/api-catalog/api-catalog-utils";
import arrayToTree from 'array-to-tree';
import {setDefaultMockContext} from '../helpers/httpClientHelpers'

export const httpClientActions = {
    resetHttpClientToInitialState: () => ({ type: httpClientConstants.RESET_HTTP_CLIENT_TO_INITIAL_STATE }),

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

    unsetHasChangedAll: (tabId) => ({type: httpClientConstants.UNSET_HAS_CHANGED_ALL, data: {tabId}}),

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

    setHistoryLoading: (isLoading) => {
        return {type: httpClientConstants.SET_HISTORY_LOADING, data: isLoading};
    },
    setCollectionLoading: (isLoading) => {
        return {type: httpClientConstants.SET_COLLECTION_LOADING, data: isLoading};
    },

    addCubeRunHistory: (apiTraces, cubeRunHistory, historyTabState) => {
        return {type: httpClientConstants.ADD_CUBE_RUN_HISTORY, data: {apiTraces, cubeRunHistory, historyTabState}};
    },
    
    deleteCubeRunHistory: (userCollectionId) => {
        return {type: httpClientConstants.DELETE_CUBE_RUN_HISTORY, data: userCollectionId};
    },
    setCollectionTabState: (collectionTabState) => {
        return {type: httpClientConstants.SET_COLLECTION_TAB_STATE, data: collectionTabState};
    },

    addUserCollections: (userCollections) => {
        return {type: httpClientConstants.ADD_USER_COLLECTIONS, data: {userCollections, isCollectionLoading: false}};
    },
    deleteUserCollection: (userCollectionId) => {
        return {type: httpClientConstants.DELETE_USER_COLLECTION, data: userCollectionId};
    },

    postSuccessLoadRecordedHistory: (tabId, recordedHistory) => {
        return {type: httpClientConstants.POST_SUCCESS_LOAD_RECORDED_HISTORY, data: {tabId, recordedHistory}};
    },

    setInactiveHistoryCursor: (historyCursor, active) => {
        return {type: httpClientConstants.SET_INACTIVE_HISTORY_CURSOR, data: {historyCursor, active}};
    },

    setActiveHistoryCursor: (historyCursor) => {
        return {type: httpClientConstants.SET_ACTIVE_HISTORY_CURSOR, data: {historyCursor}};
    },

    setSelectedTabKey: (selectedTabKey) => {
        return {type: httpClientConstants.SET_SELECTED_TAB_KEY, data: {selectedTabKey}};
    },

    removeTab: (tabs, selectedTabKey) => {
        return {type: httpClientConstants.REMOVE_TAB, data: {tabs, selectedTabKey}};
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


    loadCollectionTrace: (selectedCollectionId) => async (dispatch, getState) => {
        const {
            httpClient: { userCollections }, 
            cube: { selectedApp: app },
            authentication: { user: { customer_name: customerId } } 
        } = getState();
        
        const selectedCollection = userCollections.find(
            (eachCollection) => eachCollection.id === selectedCollectionId
        );
        try {
            if(selectedCollection){            
                cubeService.loadCollectionTraces(customerId, selectedCollection.collec, app, selectedCollection.id).then(
                (apiTraces) => {
                    selectedCollection.apiTraces = apiTraces;
                    dispatch(httpClientActions.addUserCollections(userCollections));
                },
                (err) => {
                    console.error("err: ", err);
                });
            }
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },

    loadHistoryApiCall : async(customerId, app, collection, endTime, numResults)=> {
        
        const filterData = {
            ...getDefaultTraceApiFilters(),
            app, endTime, 
            collectionName: collection.collec,
            depth: 100,
            numResults
        };
        const res = await cubeService.fetchAPITraceData(customerId, filterData);
        const apiTraces = res.response;
        const count = res.numFound;
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
                eachApiTraceEvent["recordingIdAddedFromClient"] = collection.id;
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
        let currentEndTime = null;
        if(apiTraces.length > 0){
            const lastApiTrace =apiTraces[apiTraces.length -1];
            if(lastApiTrace.res && lastApiTrace.res.length > 0){
                const timestamp = lastApiTrace.res[lastApiTrace.res.length -  1].reqTimestamp;
                var epochInitialTime = new Date(0); 
                epochInitialTime.setUTCSeconds(timestamp);
                currentEndTime = epochInitialTime.toISOString();
            }
        }
        return {apiTraces, cubeRunHistory, count, endTime: currentEndTime}
    },

    loadFromHistory: () => async (dispatch, getState) => {
        const { 
            cube: { selectedApp: app }, 
            httpClient: { historyTabState }, 
            authentication: { user } 
        } = getState();

        const { customer_name: customerId } = user;

        try {
            const response = await cubeService.fetchCollectionList(user, app, "History", true);
            const serverRes = response.recordings;
            const fetchedUserHistoryCollection = serverRes.find((eachCollection) => (eachCollection.recordingType === "History"))

            if(!fetchedUserHistoryCollection) {
                throw new Error("User history collection not present")
            } else {
                dispatch(httpClientActions.addUserHistoryCollection(fetchedUserHistoryCollection));
            
                dispatch(httpClientActions.setHistoryLoading(true));
                const {apiTraces, cubeRunHistory, count, endTime}  = await httpClientActions.loadHistoryApiCall(customerId, app, fetchedUserHistoryCollection, null, historyTabState.numResults)
                
                const initialHistoryTabState = {
                    ...historyTabState, 
                    currentPage: 0,
                    oldPagesData:[{
                        endTime
                    }],
                    count
                }
                dispatch(httpClientActions.addCubeRunHistory(apiTraces, cubeRunHistory, initialHistoryTabState));
            }
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error", error);
        }
    },

    refreshHistory: ()=> async(dispatch, getState)=>{
        //loadFromHistory: Use to forcefully reset the state of history Tab and load recent data for first page
        //refreshHistory: Optionaly load the data. If user is on different page in History tab, then will not load the data. 
        //       User will either reload History, or move to first page
        //       This will use a redux state in httpClient: historyTabState
        const { httpClient: { historyTabState } } = getState();
        if(historyTabState.currentPage == 0){
            dispatch(httpClientActions.loadFromHistory());
        }

    },

    historyTabNextPage: ()=> async(dispatch, getState)=>{
        const { 
            cube: { selectedApp: app }, 
            httpClient:{ historyTabState, userHistoryCollection }, 
            authentication: { user: { customer_name: customerId } } 
        } = getState();

        try {    
            const currentPageEndTime = historyTabState.oldPagesData[historyTabState.currentPage];
            dispatch(httpClientActions.setHistoryLoading(true));
            const {apiTraces, cubeRunHistory, count, endTime}  = await httpClientActions.loadHistoryApiCall(customerId, app, userHistoryCollection, currentPageEndTime.endTime, historyTabState.numResults);
            
            const initialHistoryTabState = {
                ...historyTabState,
                currentPage: historyTabState.currentPage + 1,
                oldPagesData:[
                    ...historyTabState.oldPagesData, 
                    {
                        endTime
                }],
            }
            dispatch(httpClientActions.addCubeRunHistory(apiTraces, cubeRunHistory, initialHistoryTabState));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },

    historyTabFirstPage: ()=> async(dispatch, getState)=>{

        const { 
            cube: { selectedApp: app }, 
            httpClient: { historyTabState, userHistoryCollection }, 
            authentication: { user: { customer_name: customerId } } 
        } = getState();

        try {            
            dispatch(httpClientActions.setHistoryLoading(true));
            const {apiTraces, cubeRunHistory, count, endTime}  = await httpClientActions.loadHistoryApiCall(customerId, app, userHistoryCollection, null, historyTabState.numResults);
            
            const initialHistoryTabState = {
                ...historyTabState,
                currentPage: 0,
                oldPagesData:[
                    {
                        endTime
                }],
                count
            }
            dispatch(httpClientActions.addCubeRunHistory(apiTraces, cubeRunHistory, initialHistoryTabState));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },
    historyTabPrevPage: ()=> async(dispatch, getState)=>{
        const { 
            cube: { selectedApp: app }, 
            httpClient:{ historyTabState, userHistoryCollection }, 
            authentication: { user: { customer_name: customerId } }  
        } = getState();

        try {            
            if(historyTabState.currentPage == 1){
                dispatch(httpClientActions.historyTabFirstPage());
            }else{            
                dispatch(httpClientActions.setHistoryLoading(true));
                const currentPageEndTime = historyTabState.oldPagesData[historyTabState.currentPage-2];
                const {apiTraces, cubeRunHistory, count, endTime}  = await httpClientActions.loadHistoryApiCall(customerId, app, userHistoryCollection, currentPageEndTime.endTime);
                
                const initialHistoryTabState = {
                    ...historyTabState,
                    currentPage: historyTabState.currentPage - 1,
                    oldPagesData:[
                        ...historyTabState.oldPagesData.splice(0,historyTabState.oldPagesData.length-2),
                        { endTime }
                    ],
                }
                dispatch(httpClientActions.addCubeRunHistory(apiTraces, cubeRunHistory, initialHistoryTabState));
            }
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error", error);
        }
    },

    collectionTabNextPage: ()=> async(dispatch, getState)=>{
        const { httpClient:{collectionTabState} } = getState(); 
        try {    
             dispatch(httpClientActions.setCollectionLoading(true));
             dispatch(httpClientActions.setCollectionTabState({...collectionTabState, currentPage: (collectionTabState.currentPage + 1)})); 
            dispatch(httpClientActions.loadUserCollections(false));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },
    collectionTabFirstPage: ()=> async(dispatch, getState)=>{
        const { httpClient:{collectionTabState} } = getState(); 
        try {    
             dispatch(httpClientActions.setCollectionLoading(true));
            dispatch(httpClientActions.loadUserCollections(true));
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },
    collectionTabPrevPage: ()=> async(dispatch, getState)=>{

        const { httpClient:{collectionTabState} } = getState(); 
        try {    
            if(collectionTabState.currentPage == 1){
                dispatch(httpClientActions.collectionTabFirstPage());
            }else{ 
                dispatch(httpClientActions.setCollectionLoading(true));
                dispatch(httpClientActions.setCollectionTabState({...collectionTabState, currentPage: (collectionTabState.currentPage - 1)}));
                dispatch(httpClientActions.loadUserCollections(false));
            }
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    },

    loadUserCollections: (resetToFirstPage = true) => async (dispatch, getState) => {
        const { cube: {selectedApp}, httpClient: {collectionTabState}, authentication: { user } } = getState();
        let app = selectedApp;
        try {
            const currentPage = resetToFirstPage ? 0: collectionTabState.currentPage;
            const response = await cubeService.fetchCollectionList(user, app, "UserGolden", true, collectionTabState.numResults, currentPage * collectionTabState.numResults);
            const serverRes = response.recordings;
            const userCollections = serverRes.filter((eachCollection) => (eachCollection.recordingType !== "History"))
            if(resetToFirstPage){
                dispatch(httpClientActions.setCollectionTabState({...collectionTabState, currentPage: 0, count: response.numFound }));
            }

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

    setSelectedMockConfig: (selectedMockConfig) => dispatch => {
        dispatch({type: httpClientConstants.SET_SELECTED_MOCK_CONFIG, data: selectedMockConfig})
        setDefaultMockContext({mockConfigName: selectedMockConfig})
    },

    fetchMockConfigs: () => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Loading..."))
        const { 
            cube: { selectedApp }, 
            authentication: { user: { customer_name: customerId } } 
        } = getState();
        try {
            const mockConfigList = await cubeService.getAllMockConfigs(customerId, selectedApp);
            dispatch(httpClientActions.setMockConfigList(mockConfigList))
            dispatch(httpClientActions.resetMockConfigStatusText())
            setDefaultMockContext({mockConfigList})
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response?.data.message, true))
        }  
    },

    saveMockConfig: (mockConfig) => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Saving..."))
        const { 
            cube: { selectedApp }, 
            authentication: { user: { customer_name: customerId } } 
        } = getState();

        try {
            await cubeService.insertNewMockConfig(customerId, selectedApp, mockConfig);
            dispatch(httpClientActions.fetchMockConfigs())
            dispatch(httpClientActions.resetMockConfigStatusText())
            dispatch(httpClientActions.showMockConfigList(true));
        } catch (e) {
            dispatch(httpClientActions.setMockConfigStatusText(e.response.data.message, true))
        } 
    },

    updateMockConfig: (mockId, mockConfig) => async (dispatch, getState) => {
        dispatch(httpClientActions.setMockConfigStatusText("Updating..."));
        const { 
            cube: { selectedApp }, 
            authentication: { user: { customer_name: customerId } } 
        } = getState();

        try {
            await cubeService.updateMockConfig(customerId, selectedApp, mockId, mockConfig);
            dispatch(httpClientActions.fetchMockConfigs());
            dispatch(httpClientActions.resetMockConfigStatusText());
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

    setMockContextLookupCollection: (lookupCollection) => (dispatch) => { 
        dispatch({type: httpClientConstants.SET_MOCK_CONTEXT_LOOKUP_COLLECTION, data: lookupCollection})
        // update proxy context
        setDefaultMockContext({lookupCollection})
    },

    setMockContextSaveToCollection: (saveToCollection) => (dispatch) => { 
        dispatch({type: httpClientConstants.SET_MOCK_CONTEXT_SAVE_TO_COLLECTION, data: saveToCollection})
        // update proxy context
        setDefaultMockContext({saveToCollection})
    },

    updateAbortRequest: (tabId, abortRequest) => {
        return {type: httpClientConstants.UPDATE_ABORT_REQUEST, data: {tabId, abortRequest}};
    },

}