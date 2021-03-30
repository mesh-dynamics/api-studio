import React, { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Tabs, Tab, Panel, Label, Modal, Button, ControlLabel, Glyphicon } from 'react-bootstrap';
import { applyEnvVars, getCurrentEnvironment, getRenderEnvVars, getCurrentEnvVars } from "../../utils/http_client/envvar";
import EnvironmentConfig from './EnvironmentSection';
import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { stringify, parse } from 'query-string';
import cryptoRandomString from 'crypto-random-string';
import urlParser from 'url-parse';
import * as URL from "url";
import { Collection } from 'postman-collection';

import AppManager from '../../components/Navigation/AppManager.tsx';

import { cubeActions } from "../../actions";
import { cubeService } from "../../services";
import api from '../../api';
import config from '../../config';
import { ipcRenderer } from '../../helpers/ipc-renderer';

import HttpClient from "./HttpClient";
import ResponsiveTabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";

import MockConfigUtils from '../../utils/http_client/mockConfigs.utils';
import TabDataFactory from "../../utils/http_client/TabDataFactory";

import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { httpClientActions } from "../../actions/httpClientActions";
import { 
    generateRunId,
    extractParamsFromRequestEvent,
    getTraceDetailsForCurrentApp, 
    getTracerForCurrentApp, 
    hasTabDataChanged, 
    formatHttpEventToTabObject,    
    preRequestToFetchableConfig, 
    getCurrentMockConfig,
    isLocalhostUrl,
    getHostName,
} from "../../utils/http_client/utils.js";
import * as httpClientTabUtils from "../../utils/http_client/httpClientTabs.utils.js";
import { getContextMapKeyValues, getDefaultServiceName, joinPaths, isTrueOrUndefined } from "../../utils/http_client/httpClientUtils";
import { 
    extractGrpcBody,
    applyGrpcDataToRequestObject,
    getRequestUrlFromSchema,
    setGrpcDataFromDescriptor,
    getConnectionSchemaFromMetadataOrApiPath
} from "../../utils/http_client/grpc-utils"; 
import { parseCurlCommand } from '../../utils/http_client/curlparser';
import { getParameterCaseInsensitive, Base64Binary } from '../../../shared/utils';

import SplitSlider  from '../../components/SplitSlider.tsx';

import commonConstants from '../../utils/commonConstants';
import {setDefaultMockContext} from '../../helpers/httpClientHelpers'
import SideBarTabs from "./SideBarTabs";

class HttpClientTabs extends Component {

    constructor(props, context) {
        super(props, context);

        this.state = { 
            showErrorModal: false,
            errorMsg: "",
            importedToCollectionId: "",
            serializedCollection: "",
            modalErrorImportCollectionMessage: "",
            showImportModal: false,
            curlCommand: "",
            modalErrorImportFromCurlMessage: "",
        };
        this.addTab = this.addTab.bind(this);
        this.handleTabChange = this.handleTabChange.bind(this);
        this.handleRemoveTab = this.handleRemoveTab.bind(this);

        this.addOrRemoveParam = this.addOrRemoveParam.bind(this);
        this.updateParam = this.updateParam.bind(this);
        this.updateGrpcConnectData = this.updateGrpcConnectData.bind(this);
        this.updateBodyOrRawDataType = this.updateBodyOrRawDataType.bind(this);
        this.updateRequestTypeOfTab = this.updateRequestTypeOfTab.bind(this);

        this.driveRequest = this.driveRequest.bind(this);
        this.showOutgoingRequests = this.showOutgoingRequests.bind(this);

        this.handleRowClick = this.handleRowClick.bind(this);
        this.setAsReference = this.setAsReference.bind(this);
        this.addMockRequest = this.addMockRequest.bind(this);

        this.handleTestRowClick = this.handleTestRowClick.bind(this);
        this.handleAddMockReqModalClose = this.handleAddMockReqModalClose.bind(this);
        this.handleAddMockReqInputChange = this.handleAddMockReqInputChange.bind(this);
        this.handleAddMockReq = this.handleAddMockReq.bind(this);
        this.showAddMockReqModal = this.showAddMockReqModal.bind(this);

        this.handleImportModalClose = this.handleImportModalClose.bind(this);
        this.handleImportFromCurlInputChange = this.handleImportFromCurlInputChange.bind(this);
        this.handleImportFromCurl = this.handleImportFromCurl.bind(this);
        this.handleImportModalShow = this.handleImportModalShow.bind(this);


        this.handleImportCollectionInputChange = this.handleImportCollectionInputChange.bind(this);
        this.handleImportCollection = this.handleImportCollection.bind(this);
        this.handleImportedToCollectionIdChange = this.handleImportedToCollectionIdChange.bind(this);

        this.updateAbortRequest = this.updateAbortRequest.bind(this);
        this.initiateAbortRequest = this.initiateAbortRequest.bind(this);
        this.handleDeleteOutgoingReq = this.handleDeleteOutgoingReq.bind(this);
        
    }


    setAsReference(tabId) {
        const {httpClient: {tabs}} = this.props;
        const { dispatch } = this.props;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const tabToBeUpdated = tabs[tabIndex];

        let updatedTab = httpClientTabUtils.setAsReferenceForEachRequest(tabToBeUpdated);
        updatedTab = _.cloneDeep(updatedTab);
        dispatch(httpClientActions.setAsReference(tabId, updatedTab));
        setTimeout(() => {
            // this.saveToCollection(false, tabId, tabToBeUpdated.recordingIdAddedFromClient, "UserGolden");
        }, 0);
    }

    // Has been imported from shared directory. Do not remove. Keeping it as fallback.
    // In this file it seems to be used in import from curl and collection
    // getParameterCaseInsensitive (object, key) {
    //     return object[
    //         Object.keys(object)
    //         .find(k => k.toLowerCase() === key.toLowerCase())
    //     ];
    // }

    importFromCurl(curlCommand) {
        const { user } = this.props;
        if(!curlCommand) return;
        try {
            const parsedCurl = parseCurlCommand(curlCommand);
            const { cube: {selectedApp} } = this.props;
            let app = selectedApp;
            if(!selectedApp) {
                const parsedUrlObj = URL.parse(window.location.href, true);
                app = parsedUrlObj.query.app;
            }
            const urlWithoutQuery = parsedCurl.urlWithoutQuery,
                url = parsedCurl.url;
            const parsedUrl = URL.parse(url);
            let apiPath = parsedUrl.pathname ? parsedUrl.pathname : parsedUrl.host;
            let service = getDefaultServiceName();
            let defaultParamsType = "showQueryParams";
            const traceDetails = getTraceDetailsForCurrentApp()
            const customerId = user.customer_name;
            const eventData = httpClientTabUtils.generateEventdata(app, customerId, traceDetails, service, apiPath);
            let headers = [], queryParams = [], formData = [], multipartData=[], rawData = "", rawDataType = "", bodyType = "";
            for (let eachHeader in parsedCurl.headers) {
                headers.push({
                    id: uuidv4(),
                    name: eachHeader,
                    value: parsedCurl.headers[eachHeader],
                    description: "",
                    selected: true,
                });
                defaultParamsType = "showHeaders";
            }
            if(parsedCurl.cookieString) {
                headers.push({
                    id: uuidv4(),
                    name: "Cookie",
                    value: parsedCurl.cookieString,
                    description: "",
                    selected: true,
                });
            }
            for (let eachQueryParam in parsedCurl.query) {
                queryParams.push({
                    id: uuidv4(),
                    name: eachQueryParam,
                    value: parsedCurl.query[eachQueryParam],
                    description: "",
                    selected: true,
                });
                defaultParamsType = "showQueryParams";
            }
            let contentTypeHeader = _.isObject(parsedCurl.headers) ? getParameterCaseInsensitive(parsedCurl.headers, "content-type") : "";
            if(contentTypeHeader && contentTypeHeader.indexOf("json") > -1) {
                rawData = parsedCurl.data;
                rawDataType = "json";
                bodyType = "rawData";
                if(rawData){
                    defaultParamsType = "showBody";
                }
            } else if(contentTypeHeader && contentTypeHeader.indexOf("application/x-www-form-urlencoded") > -1) {
                const formParams = parse(parsedCurl.data);
                for (let eachFormParam in formParams) {
                    formData.push({
                        id: uuidv4(),
                        name: eachFormParam,
                        value: formParams[eachFormParam],
                        description: "",
                        selected: true,
                    });
                    rawDataType = "";
                }
                defaultParamsType = "showBody";
                bodyType = "formData";
            } else if(parsedCurl.multipartUploads){
                for (let eachFormParam in parsedCurl.multipartUploads) {
                    multipartData.push({
                        id: uuidv4(),
                        name: eachFormParam,
                        value: parsedCurl.multipartUploads[eachFormParam],
                        description: "",
                        selected: true,
                    });
                    rawDataType = "";
                }
                bodyType = "multipartData";
                defaultParamsType = "showBody";
            } else {
                rawData = parsedCurl.data;
                rawDataType = "text";
                bodyType = "rawData";
                if(rawData){
                    defaultParamsType = "showBody";
                }
            }
            let reqObj = {
                requestId: "NA",
                tabName: urlWithoutQuery,
                httpMethod: parsedCurl.method,
                httpURL: urlWithoutQuery,
                httpURLShowOnly: url,
                headers: headers,
                queryStringParams: queryParams,
                bodyType: bodyType,
                formData: formData,
                multipartData: multipartData,
                rawData: rawData,
                rawDataType: rawDataType,
                paramsType: defaultParamsType,
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                recordedResponseStatus: "",
                responseBodyType: "",
                outgoingRequestIds: [],
                eventData: eventData,
                showOutgoingRequestsBtn: false,
                showSaveBtn: true,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: service,
                recordingIdAddedFromClient: "",
                collectionIdAddedFromClient: "",
                traceIdAddedFromClient: "",
                recordedHistory: null,
                selectedTraceTableReqTabId: "",
                selectedTraceTableTestReqTabId: "",
                requestRunning: false,
                grpcData: {},
            }
            const savedTabId = this.addTab(null, reqObj, app);
            setTimeout(() => {
                this.setState({
                    showImportModal: false,
                    curlCommand: "",
                    modalErrorImportFromCurlMessage: ""
                });
            }, 1000);
        } catch (err) {
            console.error("err: ", err);
            this.setState({
                showImportModal: true,
                curlCommand: curlCommand,
                modalErrorImportFromCurlMessage: err
            });
        }
    }

    addMockRequest(tabId) {
        const { 
            dispatch, 
            user: { customer_name: customerId }, 
            httpClient: { tabs, mockReqServiceName, mockReqApiPath }
        } = this.props;

        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const tabToBeUpdated = tabs[tabIndex];

        const gatewayEventData = tabToBeUpdated.eventData;

        if(gatewayEventData && gatewayEventData.length > 0 ) {
            let httpRequestEventTypeIndex = gatewayEventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
            let gatewayHttpResponseEvent = gatewayEventData[httpResponseEventTypeIndex];
            let gatewayHttpRequestEvent = gatewayEventData[httpRequestEventTypeIndex];

            const traceId = gatewayHttpRequestEvent.traceId;
            const app = gatewayHttpRequestEvent.app,
                parentSpanId = gatewayHttpRequestEvent.spanId,
                spanId = cryptoRandomString({length: 16}),
                runId = gatewayHttpRequestEvent.runId,
                instanceId = gatewayHttpRequestEvent.instanceId,
                collection = gatewayHttpRequestEvent.collectionId,
                timestamp = Date.now() / 1000;

            let outgoingRequests = [];
            
            let httpResponseEvent = {
                customerId: customerId,
                app: app,
                service: mockReqServiceName,
                instanceId: instanceId,
                collection: collection,
                traceId: traceId,
                spanId: null,
                parentSpanId: null,
                runType: "DevTool",
                runId: runId,
                timestamp: timestamp,
                reqId: "NA",
                apiPath: mockReqApiPath,
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
            };
            
            let httpRequestEvent = {
                customerId: customerId,
                app: app,
                service: mockReqServiceName,
                instanceId: instanceId,
                collection: collection,
                traceId: traceId,
                spanId: spanId,
                parentSpanId: parentSpanId,
                runType: "DevTool",
                runId: runId,
                timestamp: timestamp,
                reqId: "NA",
                apiPath: mockReqApiPath,
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
            };

            let reqObj = {
                id: uuidv4(),
                requestId: "",
                tabName: mockReqApiPath,
                httpMethod: "get",
                httpURL: mockReqApiPath,
                httpURLShowOnly: mockReqApiPath,
                headers: [],
                queryStringParams: [],
                bodyType: "formData",
                formData: [],
                grpcData: {},
                multipartData: [],
                rawData: "",
                rawDataType: "json",
                paramsType: "showQueryParams",
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                recordedResponseStatus: "",
                responseBodyType: "",
                outgoingRequestIds: [],
                eventData: [...httpRequestEvent, ...httpResponseEvent],
                showOutgoingRequestsBtn: false,
                showSaveBtn: true,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: true,
                service: mockReqServiceName,
                recordingIdAddedFromClient: "",
                collectionIdAddedFromClient: "",
                traceIdAddedFromClient: "",
                recordedHistory: null,
                grpcConnectionSchema: {
                    app,
                    service: "",
                    endpoint: "",
                    method: ""
                }
            }

            if(tabToBeUpdated.outgoingRequests && _.isArray(tabToBeUpdated.outgoingRequests)) {
                outgoingRequests = tabToBeUpdated.outgoingRequests.slice();
                outgoingRequests.push(reqObj);
            } else {
                outgoingRequests = [reqObj];
            }
            dispatch(httpClientActions.addOutgoingRequestsToTab(tabId, outgoingRequests));
        }
        this.handleAddMockReqModalClose();
    }

    handleRowClick(isOutgoingRequest, selectedTraceTableReqTabId, tabId) {
        const { dispatch, httpClient: { tabs } } = this.props;
        
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const currentTableReqId = tabs[tabIndex].selectedTraceTableReqTabId;
        if(selectedTraceTableReqTabId !== currentTableReqId){
            dispatch(httpClientActions.setSelectedTraceTableReqTabId(selectedTraceTableReqTabId, tabId));
        }
    }

    handleTestRowClick(selectedTraceTableTestReqTabId, tabId) {
        const { dispatch, httpClient: { tabs } } = this.props;
        
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const currentTableReqId = tabs[tabIndex].selectedTraceTableTestReqTabId;
        if(selectedTraceTableTestReqTabId !== currentTableReqId) {
            dispatch(httpClientActions.setSelectedTraceTableTestReqId(selectedTraceTableTestReqTabId, tabId));
        }
    }

    handleAddMockReqModalClose() {
        const { dispatch } = this.props;
        dispatch(httpClientActions.closeAddMockReqModal("", false, "", "", ""));
    }

    handleAddMockReqInputChange(evt) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.setUpdatedModalMockReqDetails(evt.target.name, evt.target.value));
    }

    handleAddMockReq() {
        const { dispatch } = this.props;
        const {httpClient: {selectedTabIdToAddMockReq}} = this.props;
        this.addMockRequest(selectedTabIdToAddMockReq);
    }

    showAddMockReqModal(tabId) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.showAddMockReqModal(tabId, true, "", "", ""));
    }

    handleImportModalClose() {
        this.setState({
            showImportModal: false,
            curlCommand: "",
            modalErrorImportFromCurlMessage: "",
            importedToCollectionId: "",
            serializedCollection: "",
            modalErrorImportCollectionMessage: ""
        });
    }

    handleImportFromCurlInputChange(evt) {
        this.setState({
            curlCommand: evt.target.value
        });
    }

    handleImportFromCurl() {
        const { curlCommand } = this.state;
        this.importFromCurl(curlCommand);
    }

    handleImportModalShow() {
        this.setState({
            showImportModal: true,
            curlCommand: "",
            modalErrorImportFromCurlMessage: "",
            importedToCollectionId: "",
            serializedCollection: "",
            modalErrorImportCollectionMessage: ""
        });
    }

    handleImportCollectionInputChange(evt) {
        this.setState({
            serializedCollection: evt.target.value
        });
    }

    handleImportedToCollectionIdChange(evt) {
        this.setState({
            importedToCollectionId: evt.target.value
        });
    }

    handleImportCollection() {
        const {importedToCollectionId, serializedCollection } = this.state;
        const { user, dispatch } = this.props;
        if(!serializedCollection || !importedToCollectionId) return;
        this.setState({
            showImportModal: true,
            modalErrorImportCollectionMessage: "Saving..."
        });
        const httpEventPairs = [];
        try {
            const collectionToImport = new Collection(JSON.parse(serializedCollection));
            const flattenedCollection = httpClientTabUtils.flattenCollection(collectionToImport);
            flattenedCollection.map((eachMember) => {
                const { cube: {selectedApp} } = this.props;
                const url = eachMember.request.url.getRaw(),
                    method = eachMember.request.method,
                    requestHeaders = eachMember.request.getHeaders(),
                    requestBody = eachMember.request.body,
                    requestBodyUrlEncodedParams = requestBody && requestBody.mode === "urlencoded" ? requestBody.urlencoded.all() : {};
                if(!url) return;
                
                let app = selectedApp;
                if(!selectedApp) {
                    const parsedUrlObj = URL.parse(window.location.href, true);
                    app = parsedUrlObj.query.app;
                }
                const parsedUrl = URL.parse(url),
                    parsedQueryParams = parse(parsedUrl.search);
                let apiPath = parsedUrl.pathname ? parsedUrl.pathname : parsedUrl.host;
                let service = parsedUrl.host ? parsedUrl.host : "NA";
                const traceDetails = getTraceDetailsForCurrentApp()
                let {traceKeys, traceIdDetails: {traceId, traceIdForEvent}, spanId, parentSpanId} = traceDetails;
                const customerId = user.customer_name;
                
                let headers = {}, queryParams = {}, formData = {}, rawData = "";
                for (let eachHeader in requestHeaders) {
                    if(_.isArray(requestHeaders[eachHeader])) {
                        headers[eachHeader] = requestHeaders[eachHeader];
                    } else {
                        headers[eachHeader] = [requestHeaders[eachHeader]];
                    }
                }
                for (let eachQueryParam in parsedQueryParams) {
                    queryParams[eachQueryParam] = _.isArray(parsedQueryParams[eachQueryParam]) ? parsedQueryParams[eachQueryParam] : [parsedQueryParams[eachQueryParam]] ;
                }
                if(requestBody && requestBody.mode) {
                    let contentTypeHeader = _.isObject(requestHeaders) ? getParameterCaseInsensitive(requestHeaders, "content-type") : "";
                    if(contentTypeHeader && contentTypeHeader.indexOf("application/json") > -1 && requestBody.mode === "raw" && requestBody.raw) {
                        try {
                            rawData = JSON.parse(requestBody.raw);
                        } catch (ex) {
                            // need to fix for form params
                            rawData = requestBody.raw;
                        }
                    } else if(requestBody.mode === "urlencoded") {
                        for (let eachFormParam of requestBodyUrlEncodedParams) {
                            if(_.isArray(eachFormParam.value)) {
                                formData[eachFormParam.key] = eachFormParam.value;
                            } else {
                                formData[eachFormParam.key] = [eachFormParam.value];
                            }
                        }
                    } else if(requestBody.mode === "raw") {
                        rawData = requestBody.raw;
                    }
                }
                const eventData = httpClientTabUtils.generateEventdata(app, customerId, traceDetails, service, unescape(apiPath), method, headers, queryParams, formData, rawData);
                httpEventPairs.push({
                    request: eventData[0],
                    response: eventData[1]
                });
                return eachMember;
            })
        } catch (err) {
            console.error("err: ", err);
        }
        
        const apiConfig = {};
        cubeService.storeUserReqResponse(importedToCollectionId, httpEventPairs, apiConfig)
            .then((serverRes) => {
                this.setState({
                    showImportModal: true,
                    modalErrorImportCollectionMessage: "Saved."
                });
                dispatch(httpClientActions.loadCollectionTrace(importedToCollectionId));
            }, (error) => {
                console.error("error: ", error);
                this.setState({
                    showImportModal: true,
                    modalErrorImportCollectionMessage: error
                });
            })
        return httpEventPairs;
    }


    getTabIndexGivenTabId (tabId, tabs) {
        if(!tabs) return -1;
        return tabs.findIndex((e) => e.id === tabId);
    }

    addOrRemoveParam(isOutgoingRequest, tabId, type, op, id) {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            if(op === "delete") {
                dispatch(httpClientActions.deleteParamInSelectedOutgoingTab(tabId, type, id));
            } else {
                dispatch(httpClientActions.addParamToSelectedOutgoingTab(tabId, type));
            }
        } else {
            if(op === "delete") {
                dispatch(httpClientActions.deleteParamInSelectedTab(tabId, type, id));
            } else {
                dispatch(httpClientActions.addParamToSelectedTab(tabId, type));
            }
        }
    }

    updateGrpcConnectData(isOutgoingRequest, tabId, value, currentSelectedTabId) {
        const { dispatch, httpClient: {tabs}} = this.props;

        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateGrpcConnectDetailsInSelectedOutgoingTab(currentSelectedTabId, tabId, value));
        } else {

            let tabsToProcess = tabs;
            const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
            const tabToProcess = tabsToProcess[tabIndex];
            if(tabToProcess.requestId == "NA" ||tabToProcess.requestId == ""){
                const url = getRequestUrlFromSchema(value);
                dispatch(httpClientActions.updateAllParamsInSelectedTab(tabId, "httpURLShowOnly", "httpURLShowOnly", value.service+"/"+value.method));
                dispatch(httpClientActions.updateAllParamsInSelectedTab(tabId, "service", "service", getHostName(url) ));
            }
            dispatch(httpClientActions.updateGrpcConnectDetailsInSelectedTab(tabId, value));
        }
    }
    
    updateParamsInSync(tabId, type, value){
        const {dispatch, httpClient: {tabs}} = this.props;

        //Keep other params in sync, when one changes
        if (type === "httpURL" || type == "requestPathURL" || type == "service"){
    
            let tabsToProcess = tabs;
            const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
            const tabToProcess = tabsToProcess[tabIndex];
            
            if(type !== "service" && (tabToProcess.requestId == "NA" ||tabToProcess.requestId == "")){
                const {apiPath, service : generatedService} = httpClientTabUtils.getApiPathAndServiceFromUrl(value);
                dispatch(httpClientActions.updateParamInSelectedTab(tabId, "httpURLShowOnly", "httpURLShowOnly", apiPath));
                if(type === "httpURL" ){
                    dispatch(httpClientActions.updateParamInSelectedTab(tabId, "requestPathURL", "requestPathURL", apiPath));
                }
            }

            if(type == "requestPathURL" || (type == "service" && value != getDefaultServiceName())){
                let currentTabService = type == "requestPathURL" ? tabToProcess.service : value;
                let requestPathURL = type == "requestPathURL" ? value : tabToProcess.requestPathURL;

                const mockConfigUtils = new MockConfigUtils({
                    selectedMockConfig: this.props.httpClient.selectedMockConfig,
                    mockConfigList: this.props.httpClient.mockConfigList,
                });
                const currentService = mockConfigUtils.getCurrentService(currentTabService);
                const domain = currentService?.url || currentTabService;
                dispatch(httpClientActions.updateParamInSelectedTab(tabId, "httpURL", "httpURL", joinPaths(domain, requestPathURL || "") ));
            }
        }
    }


    updateParam(isOutgoingRequest, tabId, type, key, value, id) {
        const {dispatch, httpClient: {tabs}} = this.props;
        
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateParamInSelectedOutgoingTab(tabId, type, key, value, id));
        } else {
            dispatch(httpClientActions.updateParamInSelectedTab(tabId, type, key, value, id));
            this.updateParamsInSync(tabId, type, value);
        }
    }

    updateAllParams = (isOutgoingRequest, tabId, type, key, value) => {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateAllParamsInSelectedOutgoingTab(tabId, type, key, value));
        } else {
            dispatch(httpClientActions.updateAllParamsInSelectedTab(tabId, type, key, value));
        }
    }

    updateRequestTypeOfTab(isOutgoingRequest, currentSelectedTabId, currentSelectedRequestTabId, value) {
        const { dispatch } = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateRequestTypeOfSelectedOutgoingTab(currentSelectedTabId, currentSelectedRequestTabId, value));
        } else {
            dispatch(httpClientActions.updateRequestTypeOfSelectedTab(currentSelectedRequestTabId, value));
        }
    }

    updateBodyOrRawDataType(isOutgoingRequest, tabId, type, value) {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateBodyOrRawDataTypeInOutgoingTab(tabId, type, value));
        } else {
            dispatch(httpClientActions.updateBodyOrRawDataTypeInTab(tabId, type, value));
        } 
    }

    replaceAllParams = (isOutgoingRequest, tabId, type, params) => {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.replaceAllParamsInSelectedOutgoingTab(tabId, type, params));
        } else {
            dispatch(httpClientActions.replaceAllParamsInSelectedTab(tabId, type, params));
        }
    }

    showOutgoingRequests(tabId, traceId, collectionId, recordingId, outgoingEvents ) {    
        const { 
            dispatch,
            httpClient: { tabs, appGrpcSchema },
            cube: { selectedApp: app },
        } = this.props; 
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);

        const reqIdArray = tabs[tabIndex]["outgoingRequestIds"];
        if(tabs[tabIndex]["outgoingRequests"] && tabs[tabIndex]["outgoingRequests"].length > 0) {
            return;
        };
        if (reqIdArray && reqIdArray.length > 0) {
            let outgoingRequests = [];

            //Sort reqIdArray based on reqTimeStamp of HTTPRequest events
            const sortedReqIds = outgoingEvents.filter( outgoingEvent => outgoingEvent.eventType == "HTTPRequest")
                .sort((u,v)=> u.timestamp - v.timestamp)
                .map(outgoingEvent => outgoingEvent.reqId);
            reqIdArray.sort((reqId1, reqId2) => sortedReqIds.indexOf(reqId1) - sortedReqIds.indexOf(reqId2));

            for (let eachReqId of reqIdArray) {
                const reqResPair = outgoingEvents.filter(eachReq => eachReq.reqId === eachReqId);                    
                if (reqResPair.length > 0) {
                    const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                    const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                    const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                    const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                    const tabDataFactory = new TabDataFactory(httpRequestEvent, httpResponseEvent);
                    const reqObject = tabDataFactory.getReqObjForOutgoingRequest(recordingId, collectionId, traceId, appGrpcSchema);
                    const tabId = uuidv4();
                    outgoingRequests.push({
                        id: tabId,
                        requestId: eachReqId,
                        eventData: reqResPair,
                        tabName: reqObject.httpURLShowOnly ? reqObject.httpURLShowOnly : "New",
                        ...reqObject
                    })
                }
            }
            dispatch(httpClientActions.addOutgoingRequestsToTab(tabId, outgoingRequests));
        }
    }

    async driveRequestHandleResponse(tabId, runId, reqTimestamp, httpRequestURLRendered, currentEnvironment, responseStatus, responseStatusText, fetchedResponseHeaders, bodyData, responseTrailers={}, preRequestResult){
        const {httpClient: { userHistoryCollection}, dispatch} = this.props;
        const resTimestamp = Date.now() / 1000;
    
        dispatch(httpClientActions.postSuccessDriveRequest(tabId, responseStatus, responseStatusText, JSON.stringify(fetchedResponseHeaders), bodyData, responseTrailers));
        this.saveToHistoryAndLoadTrace(tabId, userHistoryCollection.id, runId, reqTimestamp, resTimestamp, httpRequestURLRendered, currentEnvironment, preRequestResult);
    }
     async driveRequestHandleError(error, tabId, runId){
         const {dispatch} = this.props;
         console.error(error);
         dispatch(httpClientActions.postErrorDriveRequest(tabId, error.message));
         dispatch(httpClientActions.unsetReqRunning(tabId, runId));
         if(error.message !== commonConstants.USER_ABORT_MESSAGE && error.message !== commonConstants.USER_ABORT_ELECTRON){                
             this.showErrorAlert(`Could not get any response. There was an error connecting: ${error}`);
         }
     }

    async driveRequest(isOutgoingRequest, tabId) {
        const {httpClient: {tabs, selectedTabKey, userHistoryCollection, mockConfigList, selectedMockConfig, mockContextLookupCollection, mockContextSaveToCollection, selectedEnvironment, contextMap, generalSettings  }} = this.props;
        const { cube: { selectedApp }, user } = this.props;
        const { dispatch } = this.props;
        const userId = user.username;
        const customerId = user.customer_name;
        let tabsToProcess = tabs;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        if(tabIndex < 0) return;
        dispatch(httpClientActions.resetRunState(tabId))
        // generate a new run id every time a request is run
        const runId = generateRunId();
        const mockConfig = getCurrentMockConfig(mockConfigList, selectedMockConfig);
        const traceId = tabs[tabIndex].traceIdAddedFromClient;
        const parentSpanId = tabToProcess.eventData[0].spanId;
        const tracer = getTracerForCurrentApp()
        if(PLATFORM_ELECTRON) {
            const mockContext = {
                collectionId: userHistoryCollection.collec,
                // recordingId: this.state.tabs[tabIndex].recordingIdAddedFromClient,
                recordingCollectionId: tabs[tabIndex].collectionIdAddedFromClient || (mockContextLookupCollection || userHistoryCollection.collec),
                recordingId: userHistoryCollection.id,
                traceId: traceId,
                selectedApp,
                customerName: customerId,
                runId: runId,
                config: mockConfig,
                parentSpanId: parentSpanId, // parent spanId for egress requests
                tracer: tracer,
            }

            console.log("Setting mock context for this request: ", mockContext)
            ipcRenderer.send('mock_context_change', mockContext);
        }
        const { headers, queryStringParams, bodyType, rawDataType } = tabToProcess;
        const httpReqestHeaders = httpClientTabUtils.extractHeaders(headers);

        const httpRequestQueryStringParams = httpClientTabUtils.extractQueryStringParams(queryStringParams);
        let httpRequestBody;
        const isGrpc = httpClientTabUtils.isgRPCRequest(tabToProcess);
        if (bodyType === "formData") {
            const { formData } = tabToProcess;
            httpRequestBody = httpClientTabUtils.extractBody(formData);
        }
        if (bodyType === "rawData") {
            const { rawData } = tabToProcess;
            httpRequestBody = httpClientTabUtils.extractBody(rawData);
        }
        if (isGrpc) {
            const { grpcData, grpcConnectionSchema } = tabToProcess;
            httpRequestBody = extractGrpcBody(grpcData, grpcConnectionSchema);
            // NOTE: extracting body seems to be something intended for non-grpc request types
            // with its extraction for formdata etc. Skipping this implementation but keeping the code
            // if(!isValidJSON(grpcData)){
            //     const errorMessage = "Grpc data should be valid JSON object";
            //     throw new Error(errorMessage);
            // }
            // httpRequestBody = httpClientTabUtils.extractBody(grpcData, grpc);
        }
        const httpMethod = httpClientTabUtils.getHttpMethod(tabToProcess);
        const httpRequestURL = bodyType === 'grpcData' 
            ? getRequestUrlFromSchema(tabToProcess.grpcConnectionSchema) 
            : tabToProcess.httpURL;

        let fetchConfig = {
            method: httpMethod,
            headers: httpReqestHeaders
        }
                
        if (httpMethod !== "GET".toLowerCase() && httpMethod !== "HEAD".toLowerCase()) {
            fetchConfig["body"] = httpRequestBody;
        }

        dispatch(httpClientActions.preDriveRequest(tabId, "WAITING...", false, runId));
        dispatch(httpClientActions.setReqRunning(tabId));


        //Check if some more code can be moved to Utils

        // render environment variables
        let httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered;
        let currentEnvironment, currentEnvironmentVars;
        currentEnvironment = getCurrentEnvironment();
        currentEnvironmentVars = getCurrentEnvVars();
        let preRequestResult = {};

        const reqTimestamp = Date.now() / 1000;
        let queryStringValue = "";
        try {

            const reqResPair = tabToProcess.eventData;
            const formattedData = httpClientTabUtils.getReqResFromTabData(selectedApp, reqResPair, tabToProcess, runId, "History", reqTimestamp, null, httpRequestURLRendered, currentEnvironment, currentEnvironmentVars);
            const preRequestData = {
                requestEvent : formattedData.request,
                environmentName: selectedEnvironment,
                injectionConfigVersion: `Default${selectedApp}`,
                contextMap:  getContextMapKeyValues(contextMap),
            }
            preRequestResult = await cubeService.fetchPreRequest(userHistoryCollection.id, runId, preRequestData, selectedApp, tabToProcess.abortRequest.cancelToken);
        
            [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] = preRequestToFetchableConfig(preRequestResult, httpRequestURL);
            queryStringValue = httpRequestQueryStringParamsRendered.toString();
        } catch (e) {
            console.error(e);
            //Fallback to old way of generating request
            try{
                [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] 
                = applyEnvVars(httpRequestURL, httpRequestQueryStringParams, fetchConfig);
                queryStringValue = stringify(httpRequestQueryStringParamsRendered);
            }
            catch(error){
                this.showErrorAlert(`${e}`); // prompt user for error in env vars
                dispatch(httpClientActions.postErrorDriveRequest(tabId, error.message));
                dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                return
            }
        }
        let fetchUrlRendered = httpRequestURLRendered + (queryStringValue.length ? "?" + queryStringValue : "");
        let fetchedResponseHeaders = {};
        fetchConfigRendered.signal = tabToProcess.abortRequest.signal;

        const value = generalSettings && generalSettings[commonConstants.ALLOW_CERTIFICATE_VALIDATION];
        fetchConfigRendered.isAllowCertiValidation = isTrueOrUndefined(value);
        
        if(PLATFORM_ELECTRON) {
            const requestApi = window.require('electron').remote.getGlobal("requestApi");
            requestApi.push(tabId + runId, {fetchConfigRendered});
            ipcRenderer.on('drive_request_error', (event, reqTabId, reqRunId, reqError) => {
                if(reqTabId === tabId && reqRunId === runId) {
                    const responseApi = window.require('electron').remote.getGlobal("responseApi");
                    this.driveRequestHandleError(reqError, tabId, runId);
                    responseApi.remove(tabId + runId);
                }
            });
            ipcRenderer.on('drive_request_completed', async (event, reqTabId, reqRunId, responseTrailersStr) => {
                if(reqTabId === tabId && reqRunId === runId) {
                    try {
                        const responseApi = window.require('electron').remote.getGlobal("responseApi");
                        let response = responseApi.get(tabId + runId);
                        responseApi.remove(tabId + runId);

                        let responseStatus = response.status;
                        const responseStatusText = response.statusText;

                        const responseTrailers = JSON.parse(responseTrailersStr)

                        fetchedResponseHeaders = response.headers.toJSON()
                        if(bodyType === 'grpcData') {
                            // in case of grpc, status will be in trailers or in headers
                            responseStatus = responseTrailers["grpc-status"]
                            if (responseStatus == undefined) {
                                // check headers
                                responseStatus = fetchedResponseHeaders["grpc-status"]
                            }
                        }
                        
                        let bodyData = "";
                        if(fetchedResponseHeaders["content-type"] == "application/grpc"){
                            bodyData = Buffer.from(await response.arrayBuffer()).toString("base64");
                        } else {
                            bodyData = await response.text();
                        }

                        this.driveRequestHandleResponse(tabId, runId, reqTimestamp, httpRequestURLRendered, currentEnvironment, responseStatus, responseStatusText, fetchedResponseHeaders, bodyData, responseTrailers, preRequestResult);
                    } catch (error) {
                        this.driveRequestHandleError(error, tabId, runId);
                    }
                }
            });
            ipcRenderer.send('drive_request_initiate', {
                tabId,
                runId,
                url: fetchUrlRendered,
                bodyType,
            });
        } else {
            fetchConfigRendered.signal = tabToProcess.abortRequest.signal;
            return fetch(fetchUrlRendered, fetchConfigRendered).then(async(response) => {
                const responseStatus = response.status;
                const responseStatusText = response.statusText;

                for (const header of response.headers) {
                    fetchedResponseHeaders[header[0]] = header[1];
                }

                const bodyData = await response.text();

                this.driveRequestHandleResponse(tabId, runId, reqTimestamp, httpRequestURLRendered, currentEnvironment, responseStatus, responseStatusText, fetchedResponseHeaders, bodyData, {}, preRequestResult);
            
            })
            .catch((error) => {
                this.driveRequestHandleError(error, tabId, runId);                
            });
        }        
    }

    updateAbortRequest(tabId, abortRequest) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.updateAbortRequest(tabId, abortRequest));
    }


    initiateAbortRequest(tabId, runId) {
        const { 
            httpClient: {
                tabs: tabsToProcess
            }, 
            dispatch
        } = this.props;

        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        tabToProcess.abortRequest?.stopRequest();
        if(PLATFORM_ELECTRON) {
            ipcRenderer.send('request_abort', {
                tabId,
                runId
            });
        }
    }

    handleTabChange(tabKey) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.setSelectedTabKey(tabKey));
        dispatch(httpClientActions.setTabIsHighlighted(tabKey, false));
    }

    onPositionChange = (fromPos, toPos) => {
        const { dispatch, httpClient: {tabs} } = this.props;
        if(fromPos > -1 && fromPos < tabs.length && toPos > -1 && toPos < tabs.length){
            dispatch(httpClientActions.changeTabPosition(fromPos, toPos));
        }
    }

    handleRemoveTab(key, evt) {
        evt.stopPropagation();
        const { dispatch } = this.props;
        // current tabs
        const {httpClient: {tabs}} = this.props;
        const currentTabs = tabs;
    
        // find index to remove
        const indexToRemove = currentTabs.findIndex(tab => tab.id === key);
        const tabToProcess = currentTabs[indexToRemove];

        // create a new array without [indexToRemove] item
        const newTabs = [...currentTabs.slice(0, indexToRemove), ...currentTabs.slice(indexToRemove + 1)];

        const nextSelectedIndex = newTabs[indexToRemove] ? indexToRemove : indexToRemove - 1;
        let nextTabId = "";
        if (!newTabs[nextSelectedIndex]) {
            dispatch(httpClientActions.removeTab(newTabs, nextTabId));
            nextTabId = this.addTab(null, null, null, true)
        }else{
            nextTabId = newTabs[nextSelectedIndex].id;
            newTabs[nextSelectedIndex].isHighlighted = false;
            dispatch(httpClientActions.removeTab(newTabs, nextTabId));
        }
        
    }

    handleDeleteOutgoingReq(outgoingReqTabId, tabId) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.deleteOutgoingReq(outgoingReqTabId, tabId));
    }

    saveToHistoryAndLoadTrace = (tabId, recordingId, runId="", reqTimestamp="", resTimestamp="", urlEnvVal="", currentEnvironment="", preRequestResult) => {
        const { 
            httpClient: { 
                historyTabState, 
                tabs: tabsToProcess,
                selectedEnvironment,                
            }, 
            cube: { selectedApp },
            dispatch
        } = this.props;

        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        
        if (!tabToProcess.eventData) {
            dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            return;
        }
        
        const reqResPair = tabToProcess.eventData;
        
        try {
            if (reqResPair.length > 0) {
                const data = [];
                const reqRespData = httpClientTabUtils.getReqResFromTabData(selectedApp, reqResPair, tabToProcess, runId, "History", reqTimestamp, resTimestamp, urlEnvVal, currentEnvironment);
                httpClientTabUtils.updateRequestDataPerPreRequest(preRequestResult, reqRespData);
                data.push(reqRespData);
                const apiConfig = {
                    cancelToken: tabToProcess.abortRequest.cancelToken
                }
                cubeService.afterResponse(recordingId, data, apiConfig, selectedEnvironment, selectedApp)
                    .then((serverRes) => {
                        try {
                            const parsedTraceReqData = serverRes.data.response && serverRes.data.response.length > 0 ? serverRes.data.response[0] : {};
                            const requestEvent = JSON.parse(parsedTraceReqData.requestEvent);
                            if(requestEvent.payload[0] == "GRPCRequestPayload"){
                                const responseEvent = JSON.parse(parsedTraceReqData.responseEvent);
                                const eventData = [requestEvent, responseEvent];
                                dispatch(httpClientActions.updateEventDataInSelectedTab(tabId, eventData ));                                
                                dispatch(httpClientActions.afterResponseReceivedData(tabId, JSON.stringify(responseEvent.payload[1].body, undefined, 4)));
                            }
                            const apiPath = _.trimStart(data[0].request.apiPath, '/') || "/";
                            const isLocalhost = isLocalhostUrl(tabToProcess.httpURL);
                            this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig);
                            if(!isLocalhost){
                                setTimeout(() => {
                                    this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig, true);
                                }, 5000);
                            }

                            dispatch(httpClientActions.updateContextMap(JSON.parse(parsedTraceReqData.extractionMap)));
                        } catch (error) {
                            console.error("Error ", error);
                            throw new Error(error);
                        }
                        setTimeout(() => {
                            if(historyTabState.currentPage == 0){
                                dispatch(httpClientActions.refreshHistory());
                            }
                        }, 2000);
                    }, (error) => {
                        dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                        console.error("error: ", error);
                    })
            }else{
                dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            } 
        } catch (error) {
            console.error("Error ", error);
            dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            throw new Error(error);
        }        
    }
    
    handleChange(evt) {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setUpdatedModalUserCollectionDetails(evt.target.name, evt.target.value));
    }


    loadSavedTrace(tabId, traceId, reqId, runId, apiPath, apiConfig, isRefetchTrace) {
        const { 
            dispatch,
            cube: { selectedApp: app },
            user: { customer_name: customerId },
            httpClient: { tabs, userHistoryCollection}, // seems like tabs is no longer used
        } = this.props;

        if(!app) {
            const parsedUrlObj = urlParser(window.location.href, true);
            app = parsedUrlObj.query.app;
        }
        const historyCollectionId = userHistoryCollection.collec;
        const apiTraceUrl = `${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${historyCollectionId}&apiPath=${apiPath}&runId=${runId}`;
        api.get(apiTraceUrl, apiConfig)
            .then((res) => {
                res.response.sort((a, b) => {
                    return b.res.length - a.res.length;
                });
                const apiTrace = res.response[0];
                const reqIdArray = [];
                if(apiTrace && apiTrace.res.length > 0){
                    // reqIdArray.push(apiTrace.res[0].requestEventId);
                    apiTrace.res.reverse();//.pop();
                    apiTrace.res.forEach((eachApiTraceEvent) => {
                        reqIdArray.push(eachApiTraceEvent.requestEventId);
                    });
                }

                if (reqIdArray && reqIdArray.length > 0) {
                    const eventTypes = [];
                    cubeService.fetchAPIEventData(customerId, app, reqIdArray, eventTypes, apiConfig).then((result) => {
                        
                        const { httpClient: {tabs} } = this.props;
                        const currentTabRecordedHistory = tabs.find(tab => tab.id == tabId).recordedHistory;
                        if(result && result.numResults > 0) {
                            const ingressReqResPair = result.objects.filter(eachReq => eachReq.apiPath === apiPath);
                            let ingressReqObj;
                            if (ingressReqResPair.length > 0) {
                                let existingId = "";
                                if(isRefetchTrace && currentTabRecordedHistory && currentTabRecordedHistory.requestId == reqId){
                                    existingId = currentTabRecordedHistory.id;
                                }
                                ingressReqObj = httpClientTabUtils.formatHttpEventToReqResObject(reqId, ingressReqResPair, false, existingId);
                            }
                            for (let eachReqId of reqIdArray) {
                                const reqResPair = result.objects.filter(eachReq => {
                                    return (eachReq.reqId === eachReqId && eachReq.apiPath !== apiPath);
                                });
                                if (reqResPair.length > 0 && eachReqId !== reqId) {
                                    let existingId = "";
                                    if(currentTabRecordedHistory && isRefetchTrace && currentTabRecordedHistory.outgoingRequests){
                                        const matchigRequest = _.find(currentTabRecordedHistory.outgoingRequests, { requestId : eachReqId});
                                        if(matchigRequest){
                                            existingId = matchigRequest.id;
                                        }
                                    }
                                    let reqObject = httpClientTabUtils.formatHttpEventToReqResObject(eachReqId, reqResPair, true, existingId);
                                    ingressReqObj.outgoingRequests.push(reqObject);
                                }
                            }
                            dispatch(httpClientActions.postSuccessLoadRecordedHistory(tabId, ingressReqObj, runId));
                            if(!isRefetchTrace) dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                        }else if(isRefetchTrace) {
                            dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                        }
                    });
                }else{
                    dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                }
            }, (err) => {
                console.error("err: ", err);
                dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            })
    }


    addTab(evt, reqObject, givenApp, isSelected = true) {
        const { dispatch, user, httpClient: {selectedTabKey} } = this.props;
        const tabId = uuidv4();
        const { app } = this.state;
        const appAvailable = givenApp ? givenApp : app ? app : "";
        const traceDetails = getTraceDetailsForCurrentApp()
        let {traceKeys, traceIdDetails: {traceId, traceIdForEvent}, spanId, parentSpanId} = traceDetails;
        if (!reqObject) {
            const { cube: { selectedApp } } = this.props;
            const customerId = user.customer_name;
            const eventData = httpClientTabUtils.generateEventdata(selectedApp, customerId, traceDetails);
            const traceHeaders = []
            traceHeaders.push({
                description: "",
                id: uuidv4(),
                name: traceKeys.traceIdKey,
                selected: true,
                value: traceId
            })

            if(traceKeys.spanIdKey) {
                traceHeaders.push({
                    description: "",
                    id: uuidv4(),
                    name: traceKeys.spanIdKey,
                    selected: true,
                    value: spanId
                })
            }
            
            const parentSpanIdHeaders = traceKeys.parentSpanIdKeys.map((key) => (
                {
                    description: "",
                    id: uuidv4(),
                    name: key,
                    selected: true,
                    value: parentSpanId
                }
            ))
            
            traceHeaders.push(...parentSpanIdHeaders)

            reqObject = {
                httpMethod: "get",
                httpURL: "",
                httpURLShowOnly: "",
                headers: [...traceHeaders],
                queryStringParams: [],
                bodyType: "formData",
                formData: [],
                multipartData: [],
                rawData: "",
                rawDataType: "json",
                paramsType: "showQueryParams",
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                responsePayloadState: "WrappedEncoded",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                recordedResponseStatus: "",
                responseBodyType: "",
                requestId: "NA",
                outgoingRequestIds: [],
                eventData: eventData,
                showOutgoingRequestsBtn: false,
                showSaveBtn: true,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: getDefaultServiceName(),
                recordingIdAddedFromClient: "",
                collectionIdAddedFromClient: "",
                traceIdAddedFromClient: traceIdForEvent,
                recordedHistory: null,
                grpcData: {},
                grpcConnectionSchema: {
                    app,
                    service: "",
                    endpoint: "",
                    method: ""
                }
            };
        } else {
            // add trace headers if not present
            if(!_.find(reqObject.headers, {name: traceKeys.traceIdKey})) {
                reqObject.headers.push({
                    description: "",
                    id: uuidv4(),
                    name: traceKeys.traceIdKey,
                    selected: true,
                    value: traceId
                })
            }

            if(traceKeys.spanIdKey && !_.find(reqObject.headers, {name: traceKeys.spanIdKey})) {
                reqObject.headers.push({
                    description: "",
                    id: uuidv4(),
                    name: traceKeys.spanIdKey,
                    selected: true,
                    value: spanId
                })
            }

            traceKeys.parentSpanIdKeys.forEach((key) => {
                if(!_.find(reqObject.headers, {name: key})) {
                    reqObject.headers.push({
                        description: "",
                        id: uuidv4(),
                        name: key,
                        selected: true,
                        value: parentSpanId
                    })
                }
            })

            if(!reqObject.grpcConnectionSchema) {
                reqObject['grpcConnectionSchema'] = {
                    app,
                    service: "",
                    endpoint: "",
                    method: ""
                };
            }

        }
        
        const nextSelectedTabId = isSelected ?  tabId : selectedTabKey;
        
        
        dispatch(httpClientActions.addTab(tabId, reqObject, appAvailable, nextSelectedTabId, reqObject.httpURL ? reqObject.httpURL : "New"));
        return tabId;
    }

    getRequestIds() {
        const { apiCatalog: { httpClientRequestIds } } = this.props;
        return httpClientRequestIds;
    }

    updateDimensions = () => {
        this.setState({ showEnvPopoverOverlay: false});
    };

    componentWillMount() {
        const { dispatch } = this.props;

        if(PLATFORM_ELECTRON) {
            ipcRenderer.on('get_config', (event, appConfig) => {
                ipcRenderer.removeAllListeners('get_config');
                
                config.localReplayBaseUrl = `http://localhost:${appConfig.replayDriverPort}/rs`;
                config.apiBaseUrl= `${appConfig.domain}/api`;
                config.recordBaseUrl= `${appConfig.domain}/api/cs`;
                config.replayBaseUrl= `${appConfig.domain}/api/rs`;
                config.analyzeBaseUrl= `${appConfig.domain}/api/as`;               
                
                dispatch(httpClientActions.fetchEnvironments())
            });
        }
    }

    componentDidMount() {
        const { 
            dispatch,
            cube: { selectedApp },
            httpClient: { tabs, selectedTabKey },
            user: { customer_name: customerId },
        } = this.props;

        window.addEventListener('resize', this.updateDimensions);
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
        
        dispatch(httpClientActions.loadFromHistory());
        dispatch(httpClientActions.loadUserCollections());
        dispatch(httpClientActions.loadProtoDescriptor());
        
        const requestIds = this.getRequestIds(), reqIdArray = Object.keys(requestIds);
        if (reqIdArray && reqIdArray.length > 0) {
            /*
                reqIdArray: string array of request IDs, which needs to be displayed at HttpClient
                Step1: Remove all reqIdArray values which are already opened, and push them into tabsToHighlight to highlight next
                Step2: Highlight all existing tabs if there are some reqIds, which are not currently opened/exists
                       If there is no reqIds to be opened new, then current selected tabs should not be highlighted
                Step3: Process all new reqIds to open a new tab
            */
            const tabsToHighlight = [];

            tabs.forEach(eachTab => {
                const indx = reqIdArray.findIndex((eachReq) => eachReq === eachTab.requestId);
                if(indx > -1) {
                    delete requestIds[reqIdArray[indx]];
                    reqIdArray.splice(indx, 1);
                    tabsToHighlight.push(eachTab.id);
                }
            });

            tabsToHighlight.forEach( tabId => {
                if(selectedTabKey !== tabId || reqIdArray.length > 0) {
                    dispatch(httpClientActions.setTabIsHighlighted(tabId, true));
                }
            });
            if(reqIdArray.length > 0){
                const allReqIds = [];
                reqIdArray.forEach((reqId) => {
                    allReqIds.push(reqId);
                    const outgoingIds = requestIds[reqId] || [];
                    allReqIds.push(...outgoingIds);
                })
                const eventTypes = [];
                cubeService.fetchAPIEventData(customerId, selectedApp, allReqIds, eventTypes).then((result) => {
                    if (result && result.numResults > 0) {
                        for (let eachReqId of reqIdArray) {
                            const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                            if (reqResPair.length > 0) {
                                let reqObject = formatHttpEventToTabObject(eachReqId, requestIds, reqResPair);
                                const savedTabId = this.addTab(null, reqObject, selectedApp, eachReqId == reqIdArray[reqIdArray.length - 1]);
                                const outgoingIds = requestIds[eachReqId] || [];
                                const outgoingEvents = [];
                                outgoingIds.map(childReqId => {
                                    const outgoingReqResPair = result.objects.filter(eachReq => eachReq.reqId === childReqId);
                                    outgoingEvents.push(...outgoingReqResPair);
                                })
                                this.showOutgoingRequests(savedTabId, reqObject.traceIdAddedFromClient, reqObject.collectionIdAddedFromClient, reqObject.recordingIdAddedFromClient, outgoingEvents);
                            }
                        }
                    }
                });
            }
            dispatch(apiCatalogActions.setHttpClientRequestIds([]));
        } else {
            let app = selectedApp;
            if(!app) {
                const parsedUrlObj = urlParser(window.location.href, true);
                app = parsedUrlObj.query.app;
            }
            if(tabs.length === 0)this.addTab(null, null, app, true);
        }

        if(PLATFORM_ELECTRON) {
            ipcRenderer.send('get_config');
            setDefaultMockContext()
        } else {
            dispatch(httpClientActions.fetchEnvironments());
        }
        // dispatch(httpClientActions.fetchEnvironments())
        //dispatch(httpClientActions.fetchMockConfigs())
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        const {httpClient: {tabs}} = this.props;
        window.removeEventListener('resize', this.updateDimensions);
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
    }
    
    componentWillReceiveProps(nextProps) {
        const {httpClient:{userHistoryCollection}} = this.props;
        const {httpClient:{userHistoryCollection: userHistoryCollectionNew}} = nextProps;

        if(userHistoryCollectionNew && !userHistoryCollection) {
            setDefaultMockContext()
        } else if (userHistoryCollectionNew && userHistoryCollection && userHistoryCollectionNew.collec != userHistoryCollection.collec) {
            setDefaultMockContext()
        }
    }


    toggleShowTrace = (tabId) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.toggleShowTrace(tabId))
    }

    getSelectedTabKey(givenTabs, type) {
        const {httpClient: {selectedTabKey}} = this.props;
        if(type && type === "outgoingRequests") {
            const currentTab = givenTabs.find((eachTab) => eachTab.id === selectedTabKey);
            const tabsToRender = currentTab[type];
            return tabsToRender.length > 0 ? tabsToRender[0].id : ""
        }
        return "";
    }

    getTabs(givenTabs) {
        let tabsToRender = givenTabs;
        const {httpClient: {selectedTabKey, cubeRunHistory, appGrpcSchema}, cube: { selectedApp }} = this.props;
        return tabsToRender.map((eachTab, index) => ({
            title: (
                <div className="tab-container">
                    <div className="tab-name">{eachTab.tabName ? eachTab.tabName : "New"}</div>
                </div>
            ),
            getContent: () => {
                return (
                    <div className="tab-container">
                        <HttpClient 
                            currentSelectedTab={eachTab}
                            selectedApp={selectedApp}
                            /* tabId={eachTab.id}
                            requestId={eachTab.requestId}
                            httpMethod={eachTab.httpMethod}
                            httpURL={eachTab.httpURL}
                            headers={eachTab.headers} 
                            queryStringParams={eachTab.queryStringParams}
                            bodyType={eachTab.bodyType}
                            formData={eachTab.formData} 
                            rawData={eachTab.rawData}
                            rawDataType={eachTab.rawDataType}
                            responseStatus={eachTab.responseStatus}
                            responseStatusText={eachTab.responseStatusText}
                            responseHeaders={eachTab.responseHeaders}
                            responseBody={eachTab.responseBody}
                            recordedResponseHeaders={eachTab.recordedResponseHeaders}
                            recordedResponseBody={eachTab.recordedResponseBody}
                            responseBodyType={eachTab.responseBodyType}
                            outgoingRequests={eachTab.outgoingRequests}
                            showSaveBtn={eachTab.showSaveBtn}
                            showCompleteDiff={eachTab.showCompleteDiff}
                            service={eachTab.service}
                            diffLayoutData={eachTab.diffLayoutData} */
                            appGrpcSchema={appGrpcSchema}
                            updateGrpcConnectData={this.updateGrpcConnectData}
                            updateRequestTypeOfTab={this.updateRequestTypeOfTab}
                            addOrRemoveParam={this.addOrRemoveParam} 
                            updateParam={this.updateParam}
                            updateAllParams={this.updateAllParams}
                            updateBodyOrRawDataType={this.updateBodyOrRawDataType}
                            replaceAllParams={this.replaceAllParams}
                            driveRequest={this.driveRequest}
                            handleRowClick={this.handleRowClick}
                            handleTestRowClick={this.handleTestRowClick}
                            setAsReference={this.setAsReference}
                            cubeRunHistory={cubeRunHistory}
                            showAddMockReqModal={this.showAddMockReqModal}
                            handleDuplicateTab={this.handleDuplicateTab}
                            toggleShowTrace={this.toggleShowTrace}
                            updateAbortRequest={this.updateAbortRequest}
                            initiateAbortRequest={this.initiateAbortRequest}
                            handleDeleteOutgoingReq={this.handleDeleteOutgoingReq}
                        />
                    </div>
                )
            },
            /* Optional parameters */
            key: eachTab.id,
            tabClassName: 'md-hc-tab',
            panelClassName: 'md-hc-tab-panel',
            hasTabChanged: hasTabDataChanged(eachTab),
            isHighlighted: eachTab.isHighlighted,
        }));
    }

    handleDuplicateTab = (tabId) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.createDuplicateTab(tabId))
    }

    showErrorAlert = (message) => {
        this.setState({ showErrorModal: true, errorMsg: message});
    };

    onCloseErrorModal = () => {
        this.setState({ showErrorModal: false});
    };

    render() {
        const { showErrorModal, errorMsg, importedToCollectionId, serializedCollection, modalErrorImportCollectionMessage, showImportModal, curlCommand, modalErrorImportFromCurlMessage } = this.state;
        const { cube: { selectedApp: app } } = this.props;
        const {httpClient: { userCollections, tabs, selectedTabKey, showAddMockReqModal, mockRequestServiceName, mockRequestApiPath, modalErrorAddMockReqMessage}} = this.props;

        return (

            <div className="http-client" style={{ display: "flex", height: "100%" }}>
                <aside className="" ref={e=> (this.sliderRef = e)}
                style={{ "width": "250px", "height": "100%", "background": "#EAEAEA", "padding": "10px", "display": "flex", "flexDirection": "column", overflow: "auto" }}>
                    
 {/* <div style={{ marginTop: "10px", marginBottom: "10px" }}>
                        <div className="label-n">APPLICATION</div>
                        <div className="application-name">{app}</div>
                    </div> */}
                    <AppManager />
                    <SideBarTabs onAddTab={this.addTab} showOutgoingRequests={this.showOutgoingRequests}/>
                </aside>
               <SplitSlider slidingElement={this.sliderRef} persistKey="VerticalSplitter_httpClientTabsSidebar"/>
                <main className="content-wrapper" style={{ flex: "1", overflow: "auto", padding: "25px", margin: "0" }}>
                    {/* <div>
                        <div className="vertical-middle inline-block">
                            <svg height="21"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                            </svg>
                        </div>
                        <div className="inline-block vertical-middle" style={{fontWeight: "bold", position: "relative", bottom: "3px", opacity: "0.5", paddingLeft: "10px"}}>API CATALOG - VIEW REQUEST DETAILS</div>
                    </div>

                    {/* <div>
                        <FormGroup>
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                type="text"
                                placeholder="Search"
                            />
                        </FormGroup>
                    </div> */}
                    <div style={{marginRight: "7px", marginTop: "-30px"}}>
                        <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}></div>
                        <div style={{display: "flex", justifyContent: "flex-end", alignItems: "flex-end" }}>
                            <div className="btn btn-sm cube-btn text-center" style={{ padding: "2px 10px", display: "inline-block", height: "25px", width: "85px" }} onClick={this.handleImportModalShow}>
                                <Glyphicon glyph="import" /> Import
                            </div>
                            <EnvironmentConfig />
                        </div>
                    </div>
                    <div style={{marginTop: "10px", display: ""}}>
                        <ResponsiveTabs
                            allowRemove={true} 
                            onAddClick={this.addTab}
                            removeActiveOnly={false} 
                            onChange={this.handleTabChange} 
                            onRemove={this.handleRemoveTab}
                            onPositionChange={this.onPositionChange}
                            items={this.getTabs(tabs)} 
                            tabsWrapperClass={"md-hc-tabs-wrapper"} 
                            selectedTabKey={selectedTabKey} 
                        />
                    </div>
                    <div>
                        <Modal show={showAddMockReqModal} onHide={this.handleAddMockReqModalClose}>
                            <Modal.Header closeButton>
                                <Modal.Title>Add mock request</Modal.Title>
                            </Modal.Header>
                            <Modal.Body>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Service Name</ControlLabel>
                                        <FormControl componentClass="input" placeholder="Service Name" name="mockReqServiceName" value={mockRequestServiceName} onChange={this.handleAddMockReqInputChange} />
                                    </FormGroup>
                                </div>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Api Path</ControlLabel>
                                        <FormControl componentClass="input" placeholder="API Path" name="mockReqApiPath" value={mockRequestApiPath} onChange={this.handleAddMockReqInputChange} />
                                    </FormGroup>
                                </div>
                                <p style={{ marginTop: "10px", fontWeight: 500 }}>{modalErrorAddMockReqMessage}</p>
                            </Modal.Body>
                            <Modal.Footer>
                                <Button onClick={this.handleAddMockReq}>Save</Button>
                                <Button onClick={this.handleAddMockReqModalClose}>Close</Button>
                            </Modal.Footer>
                        </Modal>
                    </div>
                    <div>
                    <Modal show={showImportModal} onHide={this.handleImportModalClose} bsSize="large">
                            <Modal.Header closeButton>
                                <Modal.Title>Import</Modal.Title>
                            </Modal.Header>
                            <Modal.Body>
                                <Tabs defaultActiveKey={1}>
                                    <Tab eventKey={1} title="Import from curl">
                                        <div>
                                            <FormGroup>
                                                <ControlLabel>Curl Command</ControlLabel>
                                                <FormControl componentClass="textarea" rows="15" placeholder="Curl Command" name="curlCommand" value={curlCommand} onChange={this.handleImportFromCurlInputChange} />
                                            </FormGroup>
                                        </div>
                                        <p style={{ marginTop: "10px", fontWeight: 500 }}>{modalErrorImportFromCurlMessage}</p>
                                        <Button onClick={this.handleImportFromCurl}>Import</Button>
                                    </Tab>
                                    <Tab eventKey={2} title="Import Collection">
                                        <div>
                                            <FormGroup style={{ marginBottom: "0px" }}>
                                                <ControlLabel>Collection</ControlLabel>
                                                <FormControl componentClass="select" placeholder="Select" name="importedToCollectionId" value={importedToCollectionId} onChange={this.handleImportedToCollectionIdChange}>
                                                    <option value=""></option>
                                                    {userCollections && userCollections.map((eachUserCollection) => {
                                                        return (
                                                            <option key={eachUserCollection.id} value={eachUserCollection.id}>{eachUserCollection.name}</option>
                                                        );
                                                    })}
                                                </FormControl>
                                            </FormGroup>
                                        </div>
                                        <hr />
                                        <div>
                                            <FormGroup>
                                                <ControlLabel>Collection</ControlLabel>
                                                <FormControl componentClass="textarea" rows="15" placeholder="Collection" name="serializedCollection" value={serializedCollection} onChange={this.handleImportCollectionInputChange} />
                                            </FormGroup>
                                        </div>
                                        <p style={{ marginTop: "10px", fontWeight: 500 }}>{modalErrorImportCollectionMessage}</p>
                                        <Button onClick={this.handleImportCollection}>Import</Button>
                                    </Tab>
                                </Tabs>
                            </Modal.Body>
                            <Modal.Footer>
                                <Button onClick={this.handleImportModalClose}>Close</Button>
                            </Modal.Footer>
                        </Modal>
                        
                        <Modal show={showErrorModal} onHide={this.onCloseErrorModal}>
                            <Modal.Header closeButton>
                                <Modal.Title>Error</Modal.Title>
                            </Modal.Header>
                            <Modal.Body>
                                    <p>{errorMsg}</p>
                            </Modal.Body>
                            <Modal.Footer>
                                <div onClick={this.onCloseErrorModal} className="btn btn-sm cube-btn text-center">Close</div>
                            </Modal.Footer>
                        </Modal>
                    </div>
                </main>
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    httpClient: state.httpClient,
    apiCatalog: state.apiCatalog,
    user: state.authentication.user
});

const connectedHttpClientTabs = connect(mapStateToProps)(HttpClientTabs);

export default connectedHttpClientTabs
