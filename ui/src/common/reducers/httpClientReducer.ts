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

import { httpClientConstants } from "../constants/httpClientConstants";
import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { updateHeaderBasedOnContentType } from '../utils/http_client/utils';
import { setGrpcDataFromDescriptor, getGrpcSchema } from '../utils/http_client/grpc-utils';
import { ICollectionDetails, ICubeRunHistory, IHttpClientStoreState, IHttpClientTabDetails } from "./state.types";
import { deriveTabNameFromTabObject, getMergedContextMap } from "../utils/http_client/httpClientUtils";
export interface IHttpClientAction {
    type: string,
    data: any;
}

/*
  eachTab[data.type]: this is not proper as per typescript. data.type may be key which doesn't belong into eachTab
  To remove compile errors, as of now adding a type. But slowly this pattern needs to be improved.
  Adding below interface only to ask TS to consider data.type as one of these props, which belongs to IHttpClientTabDetails type
*/

export type IHttpClientTabDetailsFieldNames =
    "eventData" | "formData" | "multipartData" | "headers" | "outgoingRequestIds"
    | "outgoingRequests" | "queryStringParams";

/* const tabId = uuidv4();
const isoDate = new Date().toISOString();
const timestamp = new Date(isoDate).getTime();
const traceId = cryptoRandomString({length: 32});
const spanId = cryptoRandomString({length: 16}); */
const initialState: IHttpClientStoreState = {
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
    historyTabState: {
        currentPage: 0,
        oldPagesData: [],
        numResults: 15,
        count: 0
    },
    isHistoryLoading: false,
    collectionTabState: {
        currentPage: 0,
        numResults: 10,
        count: 0,
        timeStamp: 0
    },
    isCollectionLoading: false,
    mockContextLookupCollection: "",
    mockContextSaveToCollection: {},
    uiPref:{},
    historyPathFilterText: "",
    appGrpcSchema: {},
    contextMap: {},
    collectionsCache: [],
    generalSettings:{},
    sidebarTabActiveKey: 1,
}

const getTabIndexGivenTabId = (tabId: string, tabs: IHttpClientTabDetails[]) => {
    if (!tabs) return -1;
    return tabs.findIndex((e) => e.id === tabId);
}

export const httpClient = (state = initialState, { type, data }: IHttpClientAction) : IHttpClientStoreState => {
    switch (type) {

        case httpClientConstants.DELETE_PARAM_IN_OUTGOING_TAB: {
            let { tabs, selectedTabKey } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
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
            let { tabs, selectedTabKey } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                const type =( data.type === "multipartDataFile" ? "multipartData": data.type);
                                const isFile = data.type === "multipartDataFile";
                                eachOutgoingTab[type] = [...eachOutgoingTab[type], {
                                    id: uuidv4(),
                                    name: "",
                                    value: "",
                                    description: "",
                                    selected: true,
                                    isFile
                                }];
                            }
                        })
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.DELETE_PARAM_IN_TAB: {
            let { tabs } = state;
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
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        const type =( data.type === "multipartDataFile" ? "multipartData": data.type);
                        const isFile = data.type === "multipartDataFile";
                        eachTab[type  as IHttpClientTabDetailsFieldNames] = [...eachTab[type  as IHttpClientTabDetailsFieldNames], {
                            id: uuidv4(),
                            name: "",
                            value: "",
                            description: "",
                            selected: true,
                            isFile
                        }];
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_PARAM_IN_OUTGOING_TAB: {
            let { tabs, selectedTabKey } = state;
            const selectedTabIndex = tabs.findIndex(tab => tab.id === selectedTabKey);
            const selectedOutgoingTabIndex = tabs[selectedTabIndex]["outgoingRequests"].findIndex(tab => tab.id === data.tabId);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][data.type];
            let changed = false;
            if (_.isArray(params)) {
                params = (params as Array<any>).map( param => {
                    if((param.id === data.id) && (param[data.key] !== data.value)){
                        changed = true;
                        return {...param, [data.key] : data.value}
                    }else{
                        return param;
                    }
                })
            } else {
                if(params !== data.value) {
                    params = data.value;
                    changed = true;
                }
            }
            return changed ? {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = params;
                                if (data.type === "httpURL") eachOutgoingTab.tabName = deriveTabNameFromTabObject(eachOutgoingTab);
                                eachOutgoingTab.hasChanged = true;
                            }
                        })
                    }
                    return eachTab;
                })
            } : state;
        }

        case httpClientConstants.UPDATE_PARAM_IN_TAB: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            tabs[tabIndex] = {...tabs[tabIndex]};
            let changed = false;
            let params = tabs[tabIndex][data.type as IHttpClientTabDetailsFieldNames];
            if (_.isArray(params)) {
                params = (params as Array<any>).map( param => {
                    if((param.id === data.id) && (param[data.key] !== data.value)){
                        changed = true;
                        return {...param, [data.key] : data.value}
                    }else{
                        return param;
                    }
                })
            } else {
                if(params !== data.value) {
                    params = data.value;
                    changed = true;
                }
            }

            return changed ? {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        if(eachTab[data.type] != params && params == "showBody"){
                            eachTab['headers'] = updateHeaderBasedOnContentType(eachTab.headers, "bodyType", eachTab.bodyType, eachTab);
                        }
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = params as any[];
                        if (data.type === "httpURL") eachTab.tabName = deriveTabNameFromTabObject(eachTab);
                        eachTab.hasChanged = true;
                        return {
                            ...eachTab
                        }
                    }
                    return eachTab;
                })
            } : state;
        }

        case httpClientConstants.UPDATE_ALL_PARAMS_IN_OUTGOING_TAB: {
            let { tabs, selectedTabKey } = state;
            const selectedTabIndex = tabs.findIndex(tab => tab.id === selectedTabKey);
            const selectedOutgoingTabIndex = tabs[selectedTabIndex]["outgoingRequests"].findIndex(tab => tab.id === data.tabId);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][data.type];
            if (_.isArray(params)) {
                params.forEach((param) => { param[data.key] = data.value })
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
                                if (data.type === "httpURL") eachOutgoingTab.tabName = deriveTabNameFromTabObject(eachOutgoingTab);
                                eachOutgoingTab.hasChanged = true;
                            }
                        })
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_ALL_PARAMS_IN_TAB: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            let params = tabs[tabIndex][data.type as IHttpClientTabDetailsFieldNames];
            if (_.isArray(params)) {
                params.forEach((param) => { param[data.key] = data.value })
            } else {
                params = data.value;
            }
            //this.setState({[type]: params})
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = params as any[];
                        if (data.type === "httpURL") eachTab.tabName = deriveTabNameFromTabObject(eachTab);
                        eachTab.hasChanged = true;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_OUTGOING_TAB: {
            let { tabs, selectedTabKey } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = data.value;
                                eachOutgoingTab['headers'] = updateHeaderBasedOnContentType(eachOutgoingTab.headers, data.type, data.value, eachOutgoingTab);
                                eachOutgoingTab.hasChanged = true;
                            }
                        })
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_BODY_OR_RAWA_DATA_TYPE_IN_TAB: {
            let { tabs } = state;
            // this.setState({[type]: value});
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type  as IHttpClientTabDetailsFieldNames] = data.value;
                        eachTab['headers'] = updateHeaderBasedOnContentType(eachTab.headers, data.type, data.value, eachTab);
                        eachTab.hasChanged = true;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.REPLACE_ALL_PARAMS_IN_TAB: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab[data.type as IHttpClientTabDetailsFieldNames] = data.params;
                        eachTab.hasChanged = true;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.REPLACE_ALL_PARAMS_IN_OUTGOING_TAB: {
            let { tabs, selectedTabKey } = state;
            const selectedTabIndex = tabs.findIndex(tab => tab.id === selectedTabKey);
            const selectedOutgoingTabIndex = tabs[selectedTabIndex]["outgoingRequests"].findIndex(tab => tab.id === data.tabId);
            if (selectedOutgoingTabIndex < 0) return state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === data.tabId) {
                                eachOutgoingTab[data.type] = data.params;
                                eachOutgoingTab.hasChanged = true;
                            }
                        })
                    }
                    return eachTab;
                }),
            }
        }


        case httpClientConstants.UPDATE_HTTP_STATUS_IN_TAB: {
            let { tabs } = state;
            const { clientTabId, tabId, status, statusText} = data;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === clientTabId) {
                        if(clientTabId == tabId){
                            //Status of main tab has changed
                            return {
                                ...eachTab, 
                                recordedResponseStatus: status,
                                // responseStatusText: statusText,
                                hasChanged: true
                            }
                        }else{
                            //status in outgoing request has changed
                            eachTab.outgoingRequests = eachTab.outgoingRequests.map((eachOutgoingTab) => {
                                if (eachOutgoingTab.id === tabId) {
                                    return {
                                        ...eachOutgoingTab,
                                        recordedResponseStatus : status,
                                        // responseStatusText: statusText,
                                        hasChanged : true
                                    }
                                }
                                return eachOutgoingTab;
                            });
                            return {...eachTab}
                        }
                       
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.MERGE_STATE: {
            return {
                ...state,
                ...data
            }
        }
        case httpClientConstants.UPDATE_REQUEST_METADATA_TAB: {
            return {
                ...state,
                tabs: state.tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        //Status of main tab has changed
                        const eventData = eachTab.eventData || [];
                        const requestEvent = eventData.find(event => event.eventType == "HTTPRequest");
                        if(requestEvent){
                            requestEvent.metaData = {...(requestEvent.metaData || {}), ...data.metaData}
                        }
                        let tab = {
                            ...eachTab, 
                            eventData: [...eventData],
                            hasChanged: true
                        }
                        tab.tabName = deriveTabNameFromTabObject(tab);
                        return tab;
                    }
                    else{
                        return eachTab;
                    }
                })
            }
        }

        case httpClientConstants.UNSET_HAS_CHANGED_ALL: {
            let { tabs } = state;
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
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseStatus"] = data.responseStatus;
                        eachTab["showCompleteDiff"] = data.showCompleteDiff;
                        eachTab["currentRunId"] = data.runId;
                        eachTab["progressState"] = httpClientConstants.PRE_DRIVE_REQUEST;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.POST_SUCCESS_DRIVE_REQUEST: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseHeaders"] = data.responseHeaders;
                        eachTab["responseBody"] = data.responseBody;
                        eachTab["responseStatus"] = data.responseStatus;
                        eachTab["responseStatusText"] = data.responseStatusText;
                        eachTab["progressState"] = httpClientConstants.POST_SUCCESS_DRIVE_REQUEST;
                        eachTab["responsePayloadState"] = "WrappedEncoded";
                        eachTab["responseTrailers"] = data.responseTrailers;
                        eachTab["authorized"] = data.authorized;
                    }
                    return eachTab;
                })
            }
        }
        case httpClientConstants.AFTER_RESPONSE_RECEIVED_DATA: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["responseBody"] = data.responseBody;
                        eachTab["progressState"] = httpClientConstants.AFTER_RESPONSE_RECEIVED_DATA;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_EVENT_DATA_IN_TAB: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["eventData"] = data.value
                        // eachTab.hasChanged = true; //TODO:Check with Sid
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_GENERAL_SETTINGS: {

            return {
                ...state,
                generalSettings: {...state.generalSettings, [data.key] : data.value }
            }
        }

        case httpClientConstants.UPDATE_CONTEXT_MAP_AFTER_RESPONSE: {

            return {
                ...state,
                contextMap: getMergedContextMap( state.contextMap, data.value, true)
            }
        }

        case httpClientConstants.UPDATE_CONTEXT_MAP: {

            return {
                ...state,
                contextMap: getMergedContextMap( state.contextMap, data.value)
            }
        }

        case httpClientConstants.DELETE_CONTEXT_MAP: {
            return {
                ...state,
                contextMap:{}
            }
        }

        case httpClientConstants.ADD_CACHED_COLLECTIONS: {
            const collections = [...(state.collectionsCache || []), ...data.collections];
            return {
                ...state,
                collectionsCache: collections
            }
        }

        case httpClientConstants.POST_ERROR_DRIVE_REQUEST: {
            let { tabs } = state;
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
            let { tabs } = state;
            return {
                ...state,
                tabs: [...tabs, {
                    id: data.tabId,
                    tabName: deriveTabNameFromTabObject(data.reqObject),
                    ...data.reqObject,
                    selectedTraceTableReqTabId: data.tabId,
                    isHighlighted: data.tabId != data.selectedTabKey,
                }],
                selectedTabKey: data.selectedTabKey,
                app: data.app
            }
        }

        case httpClientConstants.ADD_OUTGOING_REQUESTS_TO_TAB: {
            let { tabs } = state;
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
        case httpClientConstants.SET_HISTORY_PATH_FILTER: {
            return {
                ...state,
                historyPathFilterText: data
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
                    return !(traceList.requestEventId == data || traceList.traceIdAddedFromClient == data);
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

        case httpClientConstants.ADD_ALL_USER_COLLECTIONS: {
            return {
                ...state,
                allUserCollections: data.userCollections
            }
        }
        case httpClientConstants.DELETE_USER_COLLECTION: {
            let deletedCollection: ICollectionDetails;
            const userCollections = state.userCollections.filter(collection => {
                if (collection.id === data) {
                    deletedCollection = collection;
                }
                return collection.id !== data;
            });
            const tabs = state.tabs.map(tab => {
                if (deletedCollection && tab.collectionIdAddedFromClient === deletedCollection.collec) {
                    return {
                        ...tab,
                        collectionIdAddedFromClient: "",
                        recordingIdAddedFromClient: ""
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
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId && eachTab.currentRunId == data.runId) {
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
                historyCursor: data.historyCursor,
                active: data.active
            }
        }

        case httpClientConstants.SET_ACTIVE_HISTORY_CURSOR: {
            return {
                ...state,
                historyCursor: data.historyCursor
            }
        }

        case httpClientConstants.SET_SELECTED_TAB_KEY: {
            return {
                ...state,
                selectedTabKey: data.selectedTabKey
            }
        }

        case httpClientConstants.REMOVE_TAB: {
            return {
                ...state,
                tabs: data.tabs,
                selectedTabKey: data.selectedTabKey
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
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["recordedHistory"] = null;
                        //Reset the fields here, which should not persist from prev run
                        const httpRequestEvent = _.find(eachTab.eventData, {eventType: "HTTPRequest"});
                        const httpResponseEvent = _.find(eachTab.eventData, {eventType: "HTTPResponse"});
                        if(httpRequestEvent){
                            httpRequestEvent.payloadFields = [];
                        }
                        if(httpResponseEvent){
                            httpResponseEvent.payloadFields = [];
                        }
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.SET_AS_REFERENCE: {
            let { tabs } = state;
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
            let { tabs } = state;
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
            let { tabs } = state;
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
            let { tabs } = state;
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
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId && eachTab.currentRunId === data.runId) {
                        eachTab["requestRunning"] = false;
                        eachTab["abortRequest"] = null;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.CREATE_DUPLICATE_TAB: {
            let { tabs } = state;
            const tabToCloneIndex = _.findIndex(tabs, { id: data.tabId });
            const tabToClone = tabs[tabToCloneIndex];
            const newTab = _.cloneDeep(tabToClone)!;
            newTab.id = uuidv4();
            newTab.selectedTraceTableReqTabId = newTab.id;
            newTab.abortRequest = null;
            newTab.requestRunning = false;
            newTab.isHighlighted = true;
            newTab.recordingIdAddedFromClient = ""
            newTab.collectionIdAddedFromClient = ""

            newTab.outgoingRequests.forEach((request) => {
                request.recordingIdAddedFromClient = ""
                request.collectionIdAddedFromClient = ""
            })
            return {
                ...state,
                tabs: [...tabs.slice(0, tabToCloneIndex + 1), newTab, ...tabs.slice(tabToCloneIndex + 1)],
            }
        }

        case httpClientConstants.TOGGLE_SHOW_TRACE: {
            let { tabs } = state;
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

        case httpClientConstants.TOGGLE_HIDE_INTERNAL_HEADERS: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        return { ...eachTab, hideInternalHeaders : !eachTab.hideInternalHeaders }
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
            let { tabs } = state;
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

        case httpClientConstants.SET_TAB_IS_HIGHLIGHTED: {
            let { tabs } = state;
            const { tabId, isHighlighted } = data;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab["isHighlighted"] = isHighlighted;
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_UI_PREFERENCE: {
            let { tabs } = state;
            const { tabId, isHighlighted } = data;
            return {
                ...state,
                uiPref: { ...state.uiPref, [data.key]: data.value }
            } as IHttpClientStoreState
        }
        case httpClientConstants.UPDATE_ADD_TO_SERVICE: {
            const { service } = data;
            return {
                ...state,
                serviceToAddAction: service,
                showMockConfigList: !!service
            }
        }
        case httpClientConstants.DELETE_OUTGOING_REQ: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    let outgoingRequests = eachTab.outgoingRequests;
                    if (eachTab.id === data.tabId) {
                        outgoingRequests = eachTab.outgoingRequests.filter((eachOutgoingTab) => {
                            return eachOutgoingTab.id !== data.outgoingReqTabId;
                        })
                        eachTab.hasChanged = true;
                    }
                    return { ...eachTab, outgoingRequests: [...outgoingRequests] };
                })
            }
        }
        case httpClientConstants.SET_PROTO_DESCRIPTOR_VALUES: {
            let { tabs } = state;
            return {
                ...state,
                appGrpcSchema: data,
                tabs: tabs.map(eachTab => {
                            eachTab.grpcData = setGrpcDataFromDescriptor(data, eachTab.grpcData);
                            eachTab.grpcConnectionSchema = getGrpcSchema(data, eachTab.grpcConnectionSchema);
                            return eachTab;
                        })
            }
        }
        case httpClientConstants.UPDATE_GRPC_CONNECTION_DETAILS_IN_TAB: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.grpcConnectionSchema = data.value;
                        eachTab.tabName = deriveTabNameFromTabObject(eachTab);
                        eachTab.hasChanged = true;
                    }
                    return eachTab;
                })
            }
        }
        case httpClientConstants.UPDATE_GRPC_CONNECTION_DETAILS_IN_OUTGOING_TAB: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if(eachTab.id === data.tabId) {
                        const outgoingRequests = eachTab.outgoingRequests.map(eachOutgoingRequestTab => {
                            if(eachOutgoingRequestTab.id === data.outgoingRequestTabId) {
                                eachOutgoingRequestTab.grpcConnectionSchema = data.value;
                            }

                            return eachOutgoingRequestTab;
                        })

                        return { ...eachTab, outgoingRequests: [...outgoingRequests] };
                    }

                    return eachTab;
                })
            }
        }
        case httpClientConstants.UPDATE_REQUEST_TYPE_IN_SELECTED_OUTGOING_TAB: {
            let { tabs } = state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if(eachTab.id === data.tabId) {
                        const outgoingRequests = eachTab.outgoingRequests.map(eachOutgoingRequestTab => {
                            if(eachOutgoingRequestTab.id === data.outgoingRequestTabId) {
                                eachOutgoingRequestTab.bodyType = data.value.bodyType;
                                eachOutgoingRequestTab.paramsType = data.value.paramsType;
                                eachOutgoingRequestTab.grpcData = setGrpcDataFromDescriptor(state.appGrpcSchema, eachTab.grpcData);
                                eachOutgoingRequestTab.grpcConnectionSchema = getGrpcSchema(state.appGrpcSchema, eachOutgoingRequestTab.grpcConnectionSchema);
                                eachOutgoingRequestTab.eventData[0].payload[0] = data.value.payloadRequestEventName
                                if(eachOutgoingRequestTab.eventData.length > 1){
                                    eachOutgoingRequestTab.eventData[1].payload[1] = data.value.payloadResponseEventName;
                                }
                            }
                            return eachOutgoingRequestTab;
                        })

                        return { ...eachTab, outgoingRequests: [...outgoingRequests] };
                    }

                    return eachTab;
                })
            }
        }
        case httpClientConstants.UPDATE_REQUEST_TYPE_IN_SELECTED_TAB: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.hasChanged = true;
                        eachTab.bodyType = data.value.bodyType;
                        eachTab.paramsType = data.value.paramsType;
                        eachTab.grpcData = setGrpcDataFromDescriptor(state.appGrpcSchema, eachTab.grpcData);
                        eachTab.grpcConnectionSchema = getGrpcSchema(state.appGrpcSchema, eachTab.grpcConnectionSchema);
                        //Request Event
                        eachTab.eventData[0].payload[0] = data.value.payloadRequestEventName;
                        if(eachTab.eventData.length > 1){
                            //Response Event
                            eachTab.eventData[1].payload[1] = data.value.payloadResponseEventName;
                        }
                        eachTab.tabName = deriveTabNameFromTabObject(eachTab);
                    }
                    return eachTab;
                })
            }
        }
    
        case httpClientConstants.UPDATE_TAB_WITH_NEW_DATA: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            tabs[tabIndex] = {...tabs[tabIndex]};
            const {reqData, collectionId, recordingId, collectionName} = data;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.requestId = reqData.newReqId;
                        eachTab.collectionIdAddedFromClient = collectionId;
                        eachTab.collectionNameAddedFromClient = collectionName;
                        eachTab.traceIdAddedFromClient = reqData.newTraceId;
                        eachTab.recordingIdAddedFromClient = recordingId;
                        eachTab.eventData[0].reqId = reqData.newReqId;
                        eachTab.eventData[0].traceId = reqData.newTraceId;
                        eachTab.eventData[0].collection = collectionId;
                        eachTab.eventData[1].reqId = reqData.newReqId;
                        eachTab.eventData[1].traceId = reqData.newTraceId;
                        eachTab.eventData[1].collection = reqData.collec;
                    
                        return {
                            ...eachTab
                        }
                    }
                    return eachTab;
                })
            }
        }

        case httpClientConstants.UPDATE_OUTGOING_TAB_WITH_NEW_DATA: {
            let { tabs } = state;
            const tabIndex = tabs.findIndex(tab => tab.id === data.tabId);
            if (tabIndex < 0) return state;
            tabs[tabIndex] = {...tabs[tabIndex]};
            const {reqData, collectionId, recordingId} = data;
            return {
                ...state,
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === data.tabId) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if(eachOutgoingTab.id === data.outgoingTabId) { 
                                eachOutgoingTab.requestId = reqData.newReqId;
                                eachOutgoingTab.collectionIdAddedFromClient = collectionId;
                                eachOutgoingTab.traceIdAddedFromClient = reqData.newTraceId;
                                eachOutgoingTab.recordingIdAddedFromClient = recordingId;
                                eachOutgoingTab.eventData[0].reqId = reqData.newReqId;
                                eachOutgoingTab.eventData[0].traceId = reqData.newTraceId;
                                eachOutgoingTab.eventData[0].collection = collectionId;
                                eachOutgoingTab.eventData[1].reqId = reqData.newReqId;
                                eachOutgoingTab.eventData[1].traceId = reqData.newTraceId;
                                eachOutgoingTab.eventData[1].collection = reqData.collec;
                            }
                            return {
                                ...eachOutgoingTab
                            }
                        })
                    }
                    return {...eachTab};
                })
            }
        }

        case httpClientConstants.CHANGE_TAB_POSITION : {
            let { tabs } = state;
            tabs = [...tabs];
            tabs.splice(data.toPos, 0, tabs.splice(data.fromPos, 1)[0])
            return {
                ...state,
                tabs
            }
        }

        case httpClientConstants.SET_SIDEBAR_TAB_ACTIVE_KEY: {
            return { 
                ...state,
                sidebarTabActiveKey: data,
            }
        }

        default:
            return state;
    }

}