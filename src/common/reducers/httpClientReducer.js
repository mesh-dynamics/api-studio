import { httpClientConstants } from "../constants/httpClientConstants";
import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';

const initialState = { 
    tabs: [{ 
        id: "",
        requestId: "",
        tabName: "",
        httpMethod: "get",
        httpURL: "http://www.mocky.io/v2/5ed952b7310000f4dec4ed0a",
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
        eventData: null,
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
        clearIntervalHandle: null
    }],
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
    modalErroCreateCollectionMessage: "",
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
                                    description: ""
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
                            description: ""
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
                    ...data.reqObject
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

        case httpClientConstants.ADD_USER_COLLECTIONS: {
            return {
                ...state,
                userCollections: data.userCollections
            }
        }

        case httpClientConstants.POST_SUCCESS_SAVE_TO_COLLECTION: {
            let {tabs} = state;
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroSaveMessage: data.modalErroSaveMessage,
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
                modalErroSaveMessage: data.modalErroSaveMessage
            }
        }

        case httpClientConstants.CATCH_ERROR_SAVE_TO_COLLECTION: {
            return {
                ...state,
                showSaveModal : data.showSaveModal,
                modalErroSaveMessage: data.modalErroSaveMessage
            }
        }

        case httpClientConstants.POST_SUCCESS_LOAD_RECORDED_HISTORY: {
            let {tabs} = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["recordedHistory"] = data.recordedHistory;
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
        
        default:
            return state;
    }

}