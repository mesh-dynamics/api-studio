import { httpClientConstants } from "../constants/httpClientConstants";
import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import cryptoRandomString from 'crypto-random-string';

/* const tabId = uuidv4();
const isoDate = new Date().toISOString();
const timestamp = new Date(isoDate).getTime();
const traceId = cryptoRandomString({length: 32});
const spanId = cryptoRandomString({length: 16}); */
const initialState = { 
    tabs: [/* { 
        id: tabId,
        requestId: "",
        tabName: "",
        httpMethod: "get",
        httpURL: "",
        headers: [],
        queryStringParams: [],
        bodyType: "formData",
        formData: [],
        rawData: "",
        rawDataType: "json",
        responseStatus: "NA",
        responseStatusText: "",
        responseHeaders: "",
        responseBody: "",
        recordedResponseHeaders: "",
        recordedResponseBody: "",
        responseBodyType: "json",
        outgoingRequestIds: [],
        paramsType: "showQueryParams",
        eventData: [
            {
                customerId: "",
                app: "",
                service: "",
                instanceId: "devtool",
                collection: "NA",
                traceId: traceId,
                spanId: spanId,
                parentSpanId: null,
                runType: "Manual",
                runId: null,
                timestamp: timestamp,
                reqId: "NA",
                apiPath: "",
                eventType: "HTTPRequest",
                payload: [
                    "HTTPRequestPayload",
                    {
                        hdrs: {},
                        queryParams: {},
                        formParams: {},
                        method: "",
                        path: "",
                        pathSegments: []
                    }
                ],
                recordingType: "UserGolden",
                metaData: {
    
                }
            },
            {
                customerId: "",
                app: "",
                service: "",
                instanceId: "devtool",
                collection: "NA",
                traceId: traceId,
                spanId: null,
                parentSpanId: null,
                runType: "Manual",
                runId: null,
                timestamp: timestamp,
                reqId: "NA",
                apiPath: "",
                eventType: "HTTPResponse",
                payload: [
                    "HTTPResponsePayload",
                    {
                        hdrs: {},
                        body: {},
                        status: "",
                        statusCode: ""
                    }
                ],
                recordingType: "UserGolden",
                metaData: {

                }
            }
        ],
        showOutgoingRequestsBtn: false,
        showSaveBtn: true,
        outgoingRequests: [],
        diffLayoutData: [],
        showCompleteDiff: false,
        isOutgoingRequest: false,
        service: "",
        recordingIdAddedFromClient: "",
        collectionIdAddedFromClient: "",
        traceIdAddedFromClient: "",
        recordedHistory: null,
        clearIntervalHandle: null,
        selectedTraceTableReqTabId: "",
        selectedTraceTableTestReqTabId: "",
        requestRunning: false,
        showTrace: null,
    } */],
    toggleTestAndOutgoingRequests: true,
    selectedTabKey: "",
    app: "",
    historyCursor: null,
    active: false,
    userApiTraceHistory: [],
    cubeRunHistory: {},
    userCollections: [],
    userCollectionId: "",
    userHistoryCollection: null,
    showSaveModal: false,
    selectedSaveableTabId: "",
    collectionName: "",
    collectionLabel: "",
    modalErroSaveMessage: "",
    modalErroSaveMessageIsError: false,
    modalErroCreateCollectionMessage: "",
    environmentList: [],
    envStatusText: "",
    envStatusIsError: false,
    showEnvList: true,
    selectedEnvironment: "",
    showAddMockReqModal: false,
    mockReqServiceName: "",
    mockReqApiPath: "",
    modalErrorAddMockReqMessage: "",
    selectedTabIdToAddMockReq: "",

    mockConfigList: [],
    mockConfigStatusText: "",
    mockConfigStatusIsError: false,
    showMockConfigList: true,
    selectedMockConfig: "",
}

const getTabIndexGivenTabId = (tabId, tabs) => {
    if(!tabs) return -1;
    return tabs.findIndex((e) => e.id === tabId);
}

export const httpClient = (state = initialState, { type, data }) => {
    switch (type) {

        case httpClientConstants.DELETE_PARAM_IN_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if(eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = eachOutgoingTab[data.type].filter((e) => e.id !== data.id);
                            }
                        })
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.ADD_PARAM_TO_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if(eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = [...eachOutgoingTab[data.type], {
                                    id: uuidv4(),
                                    name: "",
                                    value: "",
                                    description: "",
                                    selected: true,
                                }];
                            }
                        })
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.DELETE_PARAM_IN_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type] = eachTab[data.type].filter((e) => e.id !== data.id);
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.ADD_PARAM_TO_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type] = [...eachTab[data.type], {
                            id: uuidv4(),
                            name: "",
                            value: "",
                            description: "",
                            selected: true,
                        }];
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_PARAM_IN_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            const selectedTabIndex = tabs.findIndex(tab => tab.id === selectedTabKey);
            const selectedOutgoingTabIndex = tabs[selectedTabIndex]["outgoingRequests"].findIndex(tab => tab.id === data.tabId);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][data.type];
            if(_.isArray(params)) {
                let specificParamArr = params.filter((e) => e.id === data.id);
                if(specificParamArr.length > 0) {
                    specificParamArr[0][data.key] = data.value;
                }
            } else {
                params = data.value;
            }
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = params;
                                if(data.type === "httpURL") eachOutgoingTab.tabName = params;
                            }
                        })
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_PARAM_IN_TAB: {
            let {tabs} = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if(tabIndex < 0) return state;
            let params = tabs[tabIndex][data.type];
            if(_.isArray(params)) {
                let specificParamArr = params.filter((e) => e.id === data.id);
                if(specificParamArr.length > 0) {
                    specificParamArr[0][data.key] = data.value;
                }
            } else {
                params = data.value;
            }
            //this.setState({[type]: params})
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type] = params;
                        if(data.type === "httpURL") eachTab.tabName = params;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_ALL_PARAMS_IN_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            const selectedTabIndex = tabs.findIndex(tab => tab.id === selectedTabKey);
            const selectedOutgoingTabIndex = tabs[selectedTabIndex]["outgoingRequests"].findIndex(tab => tab.id === data.tabId);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][data.type];
            if(_.isArray(params)) {
                params.forEach((param) => {param[data.key]=data.value})
            } else {
                params = data.value;
            }
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = params;
                                if(data.type === "httpURL") eachOutgoingTab.tabName = params;
                            }
                        })
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_ALL_PARAMS_IN_TAB: {
            let {tabs} = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if(tabIndex < 0) return state;
            let params = tabs[tabIndex][data.type];
            if(_.isArray(params)) {
                params.forEach((param) => {param[data.key]=data.value})
            } else {
                params = data.value;
            }
            //this.setState({[type]: params})
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type] = params;
                        if(data.type === "httpURL") eachTab.tabName = params;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = data.value;
                            }
                        })
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB: {
            let {tabs} = state;
            // this.setState({[type]: value});
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type] = data.value;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.PRE_DRIVE_REQUEST: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseStatus"] = data.responseStatus;
                        eachTab["showCompleteDiff"] = data.showCompleteDiff;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.POST_SUCCESS_DRIVE_REQUEST: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseHeaders"] = data.responseHeaders;
                        eachTab["responseBody"] = data.responseBody;
                        eachTab["responseStatus"] = data.responseStatus;
                        eachTab["responseStatusText"] = data.responseStatusText;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.POST_ERROR_DRIVE_REQUEST: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseStatus"] = data.responseStatus;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.ADD_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: [...tabs, {
                    id: data.tabId,
                    tabName: data.tabName,
                    ...data.reqObject,
                    selectedTraceTableReqTabId: data.tabId
                }],
                selectedTabKey: data.selectedTabKey,
                app: data.app
            }
        }

        case httpClientConstants.ADD_OUTGOING_REQUESTS_TO_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["outgoingRequests"] = data.outgoingRequests;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.ADD_USER_HISTORY_COLLECTION: {
            return {
                ...state,
                userHistoryCollection: data.userHistoryCollection
            }
        }

        case httpClientConstants.ADD_CUBE_RUN_HISTORY: {
            return {
                ...state,
                userApiTraceHistory: data.apiTraces,
                cubeRunHistory: data.cubeRunHistory
            }
        }
        case httpClientConstants.DELETE_CUBE_RUN_HISTORY: {
            let cubeRunHistory = {};
            Object.keys(state.cubeRunHistory).forEach((historyDate) => {
              cubeRunHistory[historyDate] = state.cubeRunHistory[historyDate].filter((traceList) => {
                if (traceList.children) {
                    traceList.children = traceList.children.filter(
                    (traceItem) => (traceItem.requestEventId != data)
                  );
                }
                return !(traceList.requestEventId == data  || traceList.traceIdAddedFromClient == data);
              });
            });
            return {
                ...state,
                cubeRunHistory: cubeRunHistory
            }
        }

        case httpClientConstants.ADD_USER_COLLECTIONS: {
            return {
                ...state,
                userCollections: data.userCollections
            }
        }
        case httpClientConstants.DELETE_USER_COLLECTION: {
            const userCollections = state.userCollections.filter( u=> u.rootRcrdngId !== data);
            return {
                ...state,
                userCollections
            }
        }

        case httpClientConstants.POST_SUCCESS_SAVE_TO_COLLECTION: {
            let {tabs} = state;
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroSaveMessage: data.modalErroSaveMessage,
                modalErroSaveMessageIsError: false,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        if(data.clearIntervalHandle) eachTab["clearIntervalHandle"] = data.clearIntervalHandle;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.POST_ERROR_SAVE_TO_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroSaveMessage: data.modalErroSaveMessage,
                modalErroSaveMessageIsError: true
            }
        }

        case httpClientConstants.CATCH_ERROR_SAVE_TO_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroSaveMessage: data.modalErroSaveMessage,
                modalErroSaveMessageIsError: true
            }
        }

        case httpClientConstants.POST_SUCCESS_LOAD_RECORDED_HISTORY: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["recordedHistory"] = data.recordedHistory;
                        eachTab["selectedTraceTableTestReqTabId"] = data.recordedHistory.id;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.CLOSE_SAVE_MODAL: {
            return {
                ...state,
                showSaveModal : data.showSaveModal
            }
        }

        case httpClientConstants.SHOW_SAVE_MODAL: {
            return {
                ...state,
                showSaveModal: data.showSaveModal, 
                collectionName: data.collectionName, 
                collectionLabel: data.collectionLabel, 
                selectedSaveableTabId: data.selectedSaveableTabId, 
                modalErroSaveMessage: data.modalErroSaveMessage, 
                modalErroSaveMessageIsError: data.modalErroSaveMessageIsError,
                modalErroCreateCollectionMessage: data.modalErroCreateCollectionMessage
            }
        }

        case httpClientConstants.SET_INACTIVE_HISTORY_CURSOR: {
            return {
                ...state,
                historyCursor : data.historyCursor,
                active: data.active
            }
        }

        case httpClientConstants.SET_ACTIVE_HISTORY_CURSOR: {
            return {
                ...state,
                historyCursor : data.historyCursor
            }
        }

        case httpClientConstants.POST_SUCCESS_CREATE_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroCreateCollectionMessage: data.modalErroCreateCollectionMessage
            }
        }

        case httpClientConstants.POST_ERROR_CREATE_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroCreateCollectionMessage: data.modalErroCreateCollectionMessage
            }
        }

        case httpClientConstants.CATCH_ERROR_CREATE_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroCreateCollectionMessage: data.modalErroCreateCollectionMessage
            }
        }

        case httpClientConstants.SET_SELECTED_TAB_KEY: {
            return {
                ...state,
                selectedTabKey : data.selectedTabKey
            }
        }

        case httpClientConstants.REMOVE_TAB: {
            return {
                ...state,
                tabs: data.tabs,
                selectedTabKey : data.selectedTabKey
            }
        }

        case httpClientConstants.SET_UPDATED_MODAL_USER_COLLECTION_DETAILS: {
            return {
                ...state,
                [data.name]: data.value
            }
        }

        // envvar
        case httpClientConstants.SET_ENVIRONMENT_LIST: {
            return {
                ...state,
                environmentList: data,
            }
        }

        case httpClientConstants.SET_ENV_STATUS_TEXT: {
            return {
                ...state,
                envStatusText: data.text,
                envStatusIsError: data.isError,
            }
        }

        case httpClientConstants.RESET_ENV_STATUS_TEXT: {
            return {
                ...state,
                envStatusText: "",
            }
        }
        
        case httpClientConstants.SHOW_ENV_LIST: {
            return {
                ...state,
                showEnvList: data,
            }
        }

        case httpClientConstants.SET_SELECTED_ENVIRONMENT: {
            return {
                ...state,
                selectedEnvironment: data,
            }
        }

        case httpClientConstants.RESET_RUN_STATE: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["recordedHistory"] = null
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.SET_AS_REFERENCE: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab = {
                            ...data.tab
                        };
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.CLOSE_ADD_MOCK_REQ_MODAL: {
            return {
                ...state,
                showAddMockReqModal: data.showAddMockReqModal, 
                mockReqServiceName: data.mockReqServiceName, 
                mockReqApiPath: data.mockReqApiPath, 
                selectedTabIdToAddMockReq: data.selectedTabIdToAddMockReq
            }
        }

        case httpClientConstants.SET_UPDATED_MODAL_MOCK_REQ_DETAILS: {
            return {
                ...state,
                [data.name]: data.value
            }
        }

        case httpClientConstants.SHOW_ADD_MOCK_REQ_MODAL: {
            return {
                ...state,
                showAddMockReqModal: data.showAddMockReqModal, 
                mockReqServiceName: data.mockReqServiceName, 
                mockReqApiPath: data.mockReqApiPath, 
                selectedTabIdToAddMockReq: data.selectedTabIdToAddMockReq
            }
        }

        case httpClientConstants.SET_SELECTED_TRACE_TABLE_REQ_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.selectedTraceTableReqTabId = data.selectedTraceTableReqTabId
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.SET_SELECTED_TRACE_TABLE_TEST_REQ_TAB: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.selectedTraceTableTestReqTabId = data.selectedTraceTableTestReqTabId
                    }
                    return eachTab; 
                })
            }
        }
        
        case httpClientConstants.SET_REQUEST_RUNNING: {            
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["requestRunning"] = true
                    }
                    return eachTab;
                })
            }
        }
        
        case httpClientConstants.UNSET_REQUEST_RUNNING: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                        if (eachTab.id === data.tabId) {
                            eachTab["requestRunning"] = false;
                            eachTab["abortRequest"] = null;
                        }
                        return eachTab;
                    })
            }
        }
        
        case httpClientConstants.CREATE_DUPLICATE_TAB: {
            let {tabs} = state;
            const tabToClone = _.find(tabs, {id: data.tabId});
            const newTab = _.cloneDeep(tabToClone);
            newTab.id = uuidv4();
            newTab.selectedTraceTableReqTabId = newTab.id;
            return {
                ...state,
                tabs: [...tabs, newTab],
            }
        }

        case httpClientConstants.TOGGLE_SHOW_TRACE: {            
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.showTrace = !eachTab.showTrace
                    }
                    return eachTab;
                })
            }
        }

        // mock configs
        case httpClientConstants.SET_MOCK_CONFIG_LIST: {
            return {
                ...state,
                mockConfigList: data,
            }
        }

        case httpClientConstants.SET_MOCK_CONFIG_STATUS_TEXT: {
            return {
                ...state,
                mockConfigStatusText: data.text,
                mockConfigStatusIsError: data.isError,
            }
        }

        case httpClientConstants.RESET_MOCK_CONFIG_STATUS_TEXT: {
            return {
                ...state,
                mockConfigStatusText: "",
            }
        }
        
        case httpClientConstants.SHOW_MOCK_CONFIG_LIST: {
            return {
                ...state,
                showMockConfigList: data,
            }
        }

        case httpClientConstants.SET_SELECTED_MOCK_CONFIG: {
            return {
                ...state,
                selectedMockConfig: data,
            }
        }

        default:
            return state;
    }

}