import { httpClientConstants } from "../constants/httpClientConstants";
import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import cryptoRandomString from 'crypto-random-string';
import { ICollectionDetails, ICubeRunHistory, IHttpClientStoreState, IHttpClientTabDetails } from "./state.types";


export interface IHttpClientAction{
    type: string,
    data:any;
  }

  /*
    eachTab[data.type]: this is not proper as per typescript. data.type may be key which doesn't belong into eachTab
    To remove compile errors, as of now adding a type. But slowly this pattern needs to be improved.
    Adding below interface only to ask TS to consider data.type as one of these props, which belongs to IHttpClientTabDetails type
  */

 export type IHttpClientTabDetailsFieldNames = 
 "eventData" | "formData" | "headers" | "outgoingRequestIds"
 | "outgoingRequests" | "queryStringParams";

/* const tabId = uuidv4();
const isoDate = new Date().toISOString();
const timestamp = new Date(isoDate).getTime();
const traceId = cryptoRandomString({length: 32});
const spanId = cryptoRandomString({length: 16}); */
const initialState : IHttpClientStoreState = { 
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
    historyTabState:{        
        currentPage: 0,
        oldPagesData:[],
        numResults:15,
        count:0
    },
    isHistoryLoading: false,
    collectionTabState:{  
        currentPage : 0,
        numResults:10,
        count:0
    },
    isCollectionLoading: false,
    mockContextLookupCollection: "",
    mockContextSaveToCollection: {},
    
}

const getTabIndexGivenTabId = (tabId:string, tabs: IHttpClientTabDetails[]) => {
    if(!tabs) return -1;
    return tabs.findIndex((e) => e.id === tabId);
}

export const httpClient = (state = initialState, { type, data }: IHttpClientAction) => {
    switch (type) {

        case httpClientConstants.DELETE_PARAM_IN_OUTGOING_TAB: {
            let {tabs, selectedTabKey} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if(eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = eachOutgoingTab[data.type].filter((e: any) => e.id !== data.id);
                                eachOutgoingTab.hasChanged = true;
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
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = eachTab[data.type as IHttpClientTabDetailsFieldNames].filter((e: any) => e.id !== data.id);
                        eachTab.hasChanged = true;
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
                        eachTab[data.type  as IHttpClientTabDetailsFieldNames] = [...eachTab[data.type  as IHttpClientTabDetailsFieldNames], {
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
                                eachOutgoingTab.hasChanged = true;
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
            let params = tabs[tabIndex][data.type  as IHttpClientTabDetailsFieldNames];
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
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = params as any[];
                        if(data.type === "httpURL") eachTab.tabName = params as unknown as string;
                        eachTab.hasChanged = true;
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
                                eachOutgoingTab.hasChanged = true;
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
            let params = tabs[tabIndex][data.type  as IHttpClientTabDetailsFieldNames];
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
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = params as any[];
                        if(data.type === "httpURL") eachTab.tabName = params as unknown as string;
                        eachTab.hasChanged = true;
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
                                eachOutgoingTab.hasChanged = true;
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
                        eachTab[data.type  as IHttpClientTabDetailsFieldNames] = data.value;
                        eachTab.hasChanged = true;
                    }
                    return eachTab; 
                })
            }
        }

        case httpClientConstants.UNSET_HAS_CHANGED_ALL: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.hasChanged = false;
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            eachOutgoingTab.hasChanged = false;
                        })
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
        case httpClientConstants.SET_HISTORY_LOADING: {
            return {
                ...state,
                isHistoryLoading: data
            }
        }
        case httpClientConstants.SET_COLLECTION_LOADING: {
            return {
                ...state,
                isCollectionLoading: data
            }
        }
        case httpClientConstants.SET_COLLECTION_TAB_STATE: {
            return {
                ...state,
                collectionTabState: data
            }
        }

        case httpClientConstants.ADD_CUBE_RUN_HISTORY: {
            return {
                ...state,
                userApiTraceHistory: data.apiTraces,
                cubeRunHistory: data.cubeRunHistory,
                historyTabState: data.historyTabState,
                isHistoryLoading: false
            }
        }
        case httpClientConstants.DELETE_CUBE_RUN_HISTORY: {
            let cubeRunHistory: ICubeRunHistory = {};
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
                userCollections: data.userCollections,
                isCollectionLoading: data.isCollectionLoading
            }
        }
        case httpClientConstants.DELETE_USER_COLLECTION: {
            let deletedCollection: ICollectionDetails;
            const userCollections = state.userCollections.filter( collection => { 
                if(collection.rootRcrdngId === data){
                    deletedCollection = collection;
                } 
                return collection.rootRcrdngId !== data;
            });
            const tabs = state.tabs.map( tab => {
                if(deletedCollection && tab.collectionIdAddedFromClient === deletedCollection.collec){
                    return {...tab, 
                    collectionIdAddedFromClient : "",
                    recordingIdAddedFromClient : ""
                    }
                }
                return tab;
            });
            return {
                ...state,
                userCollections,
                tabs
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
            const newTab = _.cloneDeep(tabToClone)!;
            newTab.id = uuidv4();
            newTab.selectedTraceTableReqTabId = newTab.id;
            newTab.abortRequest = null;
            newTab.requestRunning = false;
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
        
        case httpClientConstants.RESET_HTTP_CLIENT_TO_INITIAL_STATE: {
            return initialState;
        }

        case httpClientConstants.SET_MOCK_CONTEXT_LOOKUP_COLLECTION: {
            return {
                ...state,
                mockContextLookupCollection: data, // empty -> History
            }
        }

        case httpClientConstants.SET_MOCK_CONTEXT_SAVE_TO_COLLECTION: {
            return {
                ...state,
                mockContextSaveToCollection: data, // empty -> History
            }
        }

        case httpClientConstants.UPDATE_ABORT_REQUEST: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                        if (eachTab.id === data.tabId) {
                            eachTab["abortRequest"] = data.abortRequest;
                        }
                        return eachTab;
                    })
            }
        }

        default:
            return state;
    }

}