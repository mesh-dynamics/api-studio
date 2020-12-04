import React, { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Tabs, Tab, Panel, Label, Modal, Button, ControlLabel, Glyphicon } from 'react-bootstrap';

import { preRequestToFetchableConfig, getCurrentMockConfig } from "../../utils/http_client/utils";
import { applyEnvVars, getCurrentEnvironment, getRenderEnvVars, getCurrentEnvVars } from "../../utils/http_client/envvar";
import EnvironmentSection from './EnvironmentSection';
import MockConfigSection from './MockConfigSection';
import _, { head } from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { stringify, parse } from 'query-string';
import cryptoRandomString from 'crypto-random-string';
import urlParser from 'url-parse';
import * as URL from "url";
import { Collection } from 'postman-collection';

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

import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { httpClientActions } from "../../actions/httpClientActions";
import { generateRunId, generateApiPath, getApiPathFromRequestEvent, extractParamsFromRequestEvent, selectedRequestParamData, unSelectedRequestParamData, isValidJSON, Base64Binary  } from "../../utils/http_client/utils"; 
import { parseCurlCommand } from '../../utils/http_client/curlparser';
import { getParameterCaseInsensitive } from '../../../shared/utils';

import SplitSlider  from '../../components/SplitSlider.tsx';

import commonConstants from '../../utils/commonConstants';
import MockConfigs from "./MockConfigs";
import {setDefaultMockContext} from '../../helpers/httpClientHelpers'
import SideBarTabs from "./SideBarTabs";
import {hasTabDataChanged, formatHttpEventToTabObject} from "../../utils/http_client/utils"
import {applyEnvVarsToUrl} from "../../utils/http_client/envvar";

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
        this.updateBodyOrRawDataType = this.updateBodyOrRawDataType.bind(this);

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
        this.handleDeleteOutgoingReq = this.handleDeleteOutgoingReq.bind(this);
        
    }

    createRecordedDataForEachRequest(toBeUpdatedData, toBeCopiedFromData) {
        let referenceEventData = toBeCopiedFromData ? toBeCopiedFromData.eventData : null;
        let eventData = toBeUpdatedData.eventData;
        if(referenceEventData && referenceEventData.length > 0) {
            let refHttpRequestEventTypeIndex = referenceEventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let refHttpResponseEventTypeIndex = refHttpRequestEventTypeIndex === 0 ? 1 : 0;
            let refHttpResponseEvent = referenceEventData[refHttpResponseEventTypeIndex];
            let refHttpRequestEvent = referenceEventData[refHttpRequestEventTypeIndex];
            let refRequestEventData = eventData[refHttpRequestEventTypeIndex];
            let refResponseEventData = eventData[refHttpResponseEventTypeIndex];

            let httpRequestEventTypeIndex = eventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
            let gatewayHttpResponseEvent = eventData[httpResponseEventTypeIndex];
            let gatewayHttpRequestEvent = eventData[httpRequestEventTypeIndex];

            // TODO: should have been more careful while copying event data.
            // This has to be simpler.
            let httpResponseEvent = {
                customerId: refResponseEventData.customerId,
                app: refResponseEventData.app,
                service: refHttpRequestEvent.service,
                instanceId: refResponseEventData.instanceId,
                collection: toBeUpdatedData.collectionIdAddedFromClient,
                traceId: toBeUpdatedData.traceIdAddedFromClient,
                spanId: null,
                parentSpanId: null,
                runType: refHttpRequestEvent.runType,
                runId: null,
                timestamp: refHttpRequestEvent.timestamp,
                reqId: "NA",
                apiPath: refHttpRequestEvent.apiPath,
                eventType: "HTTPResponse",
                payload: refHttpResponseEvent.payload,
                recordingType: refResponseEventData.recordingType,
                metaData: {}
            };
            
            let httpRequestEvent = {
                customerId: refRequestEventData.customerId,
                app: refRequestEventData.app,
                service: refHttpRequestEvent.service,
                instanceId: refRequestEventData.instanceId,
                collection: toBeUpdatedData.collectionIdAddedFromClient,
                traceId: toBeUpdatedData.traceIdAddedFromClient,
                spanId: cryptoRandomString({length: 16}),
                parentSpanId: eventData[httpRequestEventTypeIndex].spanId,
                runType: refHttpRequestEvent.runType,
                runId: null,
                timestamp: refHttpRequestEvent.timestamp,
                reqId: refHttpRequestEvent.reqId || "NA", // TODO: Revisit this for new request
                apiPath: refHttpRequestEvent.apiPath,
                eventType: "HTTPRequest",
                payload: refHttpRequestEvent.payload,
                recordingType: refRequestEventData.recordingType,
                metaData: {}
            };

            let tabData = {
                id: uuidv4(),
                requestId: toBeCopiedFromData.reqId,
                httpMethod: toBeCopiedFromData.httpMethod,
                httpURL: toBeCopiedFromData.httpURL,
                httpURLShowOnly: toBeCopiedFromData.httpURLShowOnly,
                headers: toBeCopiedFromData.headers,
                queryStringParams: toBeCopiedFromData.queryStringParams,
                bodyType: toBeCopiedFromData.bodyType,
                formData: toBeCopiedFromData.formData,
                rawData: toBeCopiedFromData.rawData,
                rawDataType: toBeCopiedFromData.rawDataType,
                paramsType: "showQueryParams",
                responseStatus: toBeCopiedFromData.responseStatus,
                responseStatusText: toBeCopiedFromData.responseStatusText,
                responseHeaders: toBeCopiedFromData.responseHeaders,
                responseBody: toBeCopiedFromData.responseBody,
                recordedResponseHeaders: toBeCopiedFromData.recordedResponseHeaders,
                recordedResponseBody: toBeCopiedFromData.recordedResponseBody,
                responseBodyType: toBeCopiedFromData.responseBodyType,
                outgoingRequestIds: toBeCopiedFromData.outgoingRequestIds,
                eventData: [httpRequestEvent, httpResponseEvent],
                showOutgoingRequestsBtn: toBeCopiedFromData.showOutgoingRequestsBtn,
                showSaveBtn: toBeCopiedFromData.showSaveBtn,
                outgoingRequests: toBeCopiedFromData.outgoingRequests,
                diffLayoutData: toBeCopiedFromData.diffLayoutData,
                showCompleteDiff: toBeCopiedFromData.showCompleteDiff,
                isOutgoingRequest: toBeCopiedFromData.isOutgoingRequest,
                service: toBeCopiedFromData.service,
                recordingIdAddedFromClient: toBeUpdatedData.recordingIdAddedFromClient,
                collectionIdAddedFromClient: toBeUpdatedData.collectionIdAddedFromClient,
                traceIdAddedFromClient: toBeUpdatedData.traceIdAddedFromClient,
                recordedHistory: [],
                hasChanged: true,
            }
            return tabData;
        }
    }

    copyRecordedDataForEachRequest(toBeUpdatedData, toBeCopiedFromData) {
        let referenceEventData = toBeCopiedFromData ? toBeCopiedFromData.eventData : null,
            eventData = toBeUpdatedData.eventData; 
        if(referenceEventData && eventData && referenceEventData.length > 0 && eventData.length > 0 ) {
            let refHttpRequestEventTypeIndex = referenceEventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let refHttpResponseEventTypeIndex = refHttpRequestEventTypeIndex === 0 ? 1 : 0;
            let refHttpResponseEvent = referenceEventData[refHttpResponseEventTypeIndex];
            let refHttpRequestEvent = referenceEventData[refHttpRequestEventTypeIndex];

            let httpRequestEventTypeIndex = eventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
            let httpResponseEvent = eventData[httpResponseEventTypeIndex];
            let httpRequestEvent = eventData[httpRequestEventTypeIndex];

            httpResponseEvent.payload = refHttpResponseEvent.payload;
            httpRequestEvent.payload = refHttpRequestEvent.payload;

            let tabData = {
                id: toBeUpdatedData.id,
                tabName: toBeUpdatedData.tabName,
                requestId: toBeUpdatedData.requestId, // from the original request. diff fails in case of setAsReference
                httpMethod: toBeCopiedFromData.httpMethod,
                httpURL: toBeCopiedFromData.httpURL,
                httpURLShowOnly: toBeCopiedFromData.httpURLShowOnly,
                headers: toBeCopiedFromData.headers,
                queryStringParams: toBeCopiedFromData.queryStringParams,
                bodyType: toBeCopiedFromData.bodyType,
                formData: toBeCopiedFromData.formData,
                rawDataType: toBeCopiedFromData.rawDataType,
                rawData: toBeCopiedFromData.rawData,
                grpcData: toBeCopiedFromData.grpcData,
                paramsType: toBeUpdatedData.paramsType,
                responseStatus: toBeCopiedFromData.responseStatus,
                responseStatusText: toBeCopiedFromData.responseStatusText,
                responseHeaders: toBeCopiedFromData.responseHeaders,
                responseBody: toBeCopiedFromData.responseBody,
                recordedResponseHeaders: toBeCopiedFromData.recordedResponseHeaders,
                recordedResponseBody: toBeCopiedFromData.recordedResponseBody,
                responseBodyType: toBeCopiedFromData.responseBodyType,
                outgoingRequestIds: toBeCopiedFromData.outgoingRequestIds,
                eventData: toBeCopiedFromData.eventData,
                showOutgoingRequestsBtn: toBeCopiedFromData.showOutgoingRequestsBtn,
                showSaveBtn: toBeCopiedFromData.showSaveBtn,
                outgoingRequests: toBeCopiedFromData.outgoingRequests,
                diffLayoutData: toBeCopiedFromData.diffLayoutData,
                showCompleteDiff: toBeCopiedFromData.showCompleteDiff,
                isOutgoingRequest: toBeCopiedFromData.isOutgoingRequest,
                service: toBeCopiedFromData.service,
                recordingIdAddedFromClient: toBeUpdatedData.recordingIdAddedFromClient,
                collectionIdAddedFromClient: toBeUpdatedData.collectionIdAddedFromClient,
                traceIdAddedFromClient: toBeUpdatedData.traceIdAddedFromClient,
                recordedHistory: toBeUpdatedData.recordedHistory,
                hasChanged: true,
            }
            return tabData;
        }
    }

    findOutgoingRequestIndexGivenApiPath(refReq, tab) {
        let outgoingRequests = tab.outgoingRequests;
        const indexToFind = outgoingRequests.findIndex(eachReq => eachReq.eventData[0].apiPath === refReq.eventData[0].apiPath);
        return indexToFind;
    }

    setAsReferenceForEachRequest(tabToBeProcessed) {
        const recordedHistory = tabToBeProcessed.recordedHistory;
        const copiedTabData = this.copyRecordedDataForEachRequest(tabToBeProcessed, recordedHistory);
        const copiedTab = {
            ...copiedTabData
        };
        const outgoingRequests = [];
        recordedHistory.outgoingRequests.forEach((eachReq) => {
            const matchedReqIndex = this.findOutgoingRequestIndexGivenApiPath(eachReq, tabToBeProcessed);
            if(matchedReqIndex > -1) {
                const copiedOutgoingData = this.copyRecordedDataForEachRequest(tabToBeProcessed.outgoingRequests[matchedReqIndex], eachReq);
                outgoingRequests.push(copiedOutgoingData);
                tabToBeProcessed.outgoingRequests.splice(matchedReqIndex, 1); // Please please please no. Mutation of passed parameters leads to untraceable and confusing bugs
            } else {
                const copiedOutgoingData = this.createRecordedDataForEachRequest(tabToBeProcessed, eachReq);
                outgoingRequests.push(copiedOutgoingData);
            }
        })
        copiedTab.outgoingRequests = [...tabToBeProcessed.outgoingRequests, ...outgoingRequests];
        copiedTab.selectedTraceTableReqTabId = copiedTab.id
        copiedTab.selectedTraceTableTestReqTabId = recordedHistory.id
        return copiedTab;
    }

    setAsReference(tabId) {
        const {httpClient: {tabs}} = this.props;
        const { dispatch } = this.props;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const tabToBeUpdated = tabs[tabIndex];

        let updatedTab = this.setAsReferenceForEachRequest(tabToBeUpdated);
        updatedTab = JSON.parse(JSON.stringify(updatedTab));
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
            let service = parsedUrl.host ? parsedUrl.host : "NA";
            const traceId = cryptoRandomString({length: 32});
            const customerId = user.customer_name;
            const eventData = this.generateEventdata(app, customerId, traceId, service, apiPath);
            let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "", bodyType = "";
            for (let eachHeader in parsedCurl.headers) {
                headers.push({
                    id: uuidv4(),
                    name: eachHeader,
                    value: parsedCurl.headers[eachHeader],
                    description: "",
                    selected: true,
                });
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
            }
            let contentTypeHeader = _.isObject(parsedCurl.headers) ? getParameterCaseInsensitive(parsedCurl.headers, "content-type") : "";
            if(contentTypeHeader && contentTypeHeader.indexOf("json") > -1) {
                rawData = parsedCurl.data;
                rawDataType = "json";
                bodyType = "rawData";
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
                bodyType = "formData";
            } else {
                rawData = parsedCurl.data;
                rawDataType = "text";
                bodyType = "rawData";
            }
            let reqObj = {
                requestId: "",
                tabName: urlWithoutQuery,
                httpMethod: parsedCurl.method,
                httpURL: urlWithoutQuery,
                httpURLShowOnly: url,
                headers: headers,
                queryStringParams: queryParams,
                bodyType: bodyType,
                formData: formData,
                rawData: rawData,
                rawDataType: rawDataType,
                paramsType: "showQueryParams",
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                recordedResponseStatus: "",
                responseBodyType: "",
                requestId: "",
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
                isoDate = new Date().toISOString(),
                timestamp = new Date(isoDate).getTime();

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
                        statusCode: ""
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
                requestId: "",
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
                recordedHistory: null
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

    generateEventdata(app, customerId, traceId, service, apiPath, method, requestHeaders, requestQueryParams, requestFormParams, rawData) {
        const isoDate = new Date().toISOString();
        const timestamp = new Date(isoDate).getTime();
        let path = apiPath ? apiPath.replace(/^\/|\/$/g, '') : "";
        
        let httpResponseEvent = {
            customerId: customerId,
            app: app,
            service: service ? service : "NA",
            instanceId: "devtool",
            collection: "NA",
            traceId: traceId,
            spanId: "NA",
            parentSpanId: "NA",
            runType: "DevTool",
            runId: generateRunId(),
            timestamp: timestamp,
            reqId: "NA",
            apiPath: path ? path : "NA",
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
        };
        
        let httpRequestEvent = {
            customerId: customerId,
            app: app,
            service: service ? service : "NA",
            instanceId: "devtool",
            collection: "NA",
            traceId: traceId,
            spanId: cryptoRandomString({length: 16}),
            parentSpanId: "NA",
            runType: "DevTool",
            runId: generateRunId(),
            timestamp: timestamp,
            reqId: "NA",
            apiPath: path ? path : "NA",
            eventType: "HTTPRequest",
            payload: [
                "HTTPRequestPayload",
                {
                    hdrs: requestHeaders ? requestHeaders : {},
                    queryParams: requestQueryParams ? requestQueryParams : {},
                    formParams: requestFormParams ? requestFormParams : {},
                    method: method ? method : "",
                    path: path ? path : "",
                    pathSegments: path ? path.split(/\//) : [],
                    ...(rawData && {body: rawData})
                }
            ],
            recordingType: "UserGolden",
            metaData: {

            }
        };

        return [...httpRequestEvent, ...httpResponseEvent];
    }

    updateHttpEvent(apiPath, service, httpEvent) {
        const { cube: {selectedApp} } = this.props;

        return {
            ...httpEvent,
            app: selectedApp,
            ...(apiPath && {apiPath: apiPath}),
            ...(service && {service: service})
        };
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

    flattenCollection(collectionToImport) {
        let result = [], queue = [];
        if(!collectionToImport || !collectionToImport.items || !collectionToImport.items.count() || !collectionToImport.items.members || !collectionToImport.items.members.length) {
            return result;
        }
        for(let eachItem of collectionToImport.items.members) {
            queue.push({
                ...eachItem
            });
        }
        while (queue.length > 0) {
            let current = queue.shift();
            if(current.items && current.items.members && current.items.members.length) {
                for(let eachItemNode of current.items.members) {
                    queue.unshift({
                        ...eachItemNode
                    });
                }
            } else if(current.items && current.items.count() === 0) {
                
            } else {
                result.push({
                    ...current
                });
            }
        }
        return result;
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
            const flattenedCollection = this.flattenCollection(collectionToImport);
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
                const traceId = cryptoRandomString({length: 32});
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
                const eventData = this.generateEventdata(app, customerId, traceId, service, unescape(apiPath), method, headers, queryParams, formData, rawData);
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

    updateParam(isOutgoingRequest, tabId, type, key, value, id) {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateParamInSelectedOutgoingTab(tabId, type, key, value, id));
        } else {
            dispatch(httpClientActions.updateParamInSelectedTab(tabId, type, key, value, id));

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

    updateBodyOrRawDataType(isOutgoingRequest, tabId, type, value) {
        const {dispatch} = this.props;
        if(isOutgoingRequest) {
            dispatch(httpClientActions.updateBodyOrRawDataTypeInOutgoingTab(tabId, type, value));
        } else {
            dispatch(httpClientActions.updateBodyOrRawDataTypeInTab(tabId, type, value));
        } 
    }

    showOutgoingRequests(tabId, traceId, collectionId, recordingId) {    
        const { 
            dispatch,
            httpClient: { tabs },
            cube: { selectedApp: app },
            user: { customer_name: customerId },
        } = this.props; 
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);

        const reqIdArray = tabs[tabIndex]["outgoingRequestIds"];
        if(tabs[tabIndex]["outgoingRequests"] && tabs[tabIndex]["outgoingRequests"].length > 0) {
            return;
        };
        if (reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(customerId, app, reqIdArray, eventTypes).then((result) => {
                if (result && result.numResults > 0) {
                    let outgoingRequests = [];
                    for (let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);                    
                        if (reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            
                            const { headers, queryParams, formData, rawData, rawDataType }  = extractParamsFromRequestEvent(httpRequestEvent);
                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: httpRequestEvent.apiPath,
                                httpURLShowOnly: httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
                                formData: formData,
                                rawData: rawData,
                                rawDataType: rawDataType,
                                paramsType: "showQueryParams",
                                responseStatus: "NA",
                                responseStatusText: "",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
                                recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                recordedResponseStatus: httpResponseEvent ? httpResponseEvent.payload[1].status : "",
                                responseBodyType: "json",
                                showOutgoingRequestsBtn: false,
                                isOutgoingRequest: true,
                                showSaveBtn: true,
                                outgoingRequests: [],
                                service: httpRequestEvent.service,
                                recordingIdAddedFromClient: recordingId,
                                collectionIdAddedFromClient: collectionId,
                                traceIdAddedFromClient: traceId,
                                requestRunning: false,
                                showTrace: null,
                            };
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
            });
        }
    }

    extractHeaders(httpReqestHeaders) {
        let headers = new Headers();
        headers.delete('Content-Type');
        httpReqestHeaders
            .filter((header) => header.selected)
            .forEach(each => {
                if(each.name && each.value && each.name.indexOf(":") < 0) headers.append(each.name, each.value);
                // ideally for ingress requests
                if(each.name === "x-b3-spanid" && each.value) headers.append("baggage-parent-span-id", each.value);
            })
        return headers;
    }

    extractBody(httpRequestBody) {
        let formData = new URLSearchParams();
        if(_.isArray(httpRequestBody)) {
            httpRequestBody
                .filter((fparam) => fparam.selected)
                .forEach(each => {
                    if(each.name && each.value) formData.append(each.name, each.value);
                })
            return formData;
        } else {
            return httpRequestBody;
        }
    }

    extractQueryStringParams(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams
            .filter((param) => param.selected)
            .forEach(each => {
                if(each.name && each.value) qsParams[each.name] = each.value;
            })
        return qsParams;
    }

    isgRPCRequest(tabToProcess){
       return tabToProcess.bodyType === "grpcData" && tabToProcess.grpcData && tabToProcess.grpcData.trim()
    }

    async driveRequest(isOutgoingRequest, tabId) {
        const {httpClient: {tabs, selectedTabKey, userHistoryCollection, mockConfigList, selectedMockConfig, mockContextLookupCollection, mockContextSaveToCollection, selectedEnvironment, contextMap  }} = this.props;
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
        const spanId = tabToProcess.eventData[0].spanId;

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
                spanId: spanId
            }

            console.log("Setting mock context for this request: ", mockContext)
            ipcRenderer.send('mock_context_change', mockContext);
        }
        const { headers, queryStringParams, bodyType, rawDataType } = tabToProcess;
        const httpReqestHeaders = this.extractHeaders(headers);

        const httpRequestQueryStringParams = this.extractQueryStringParams(queryStringParams);
        let httpRequestBody;
        if (bodyType === "formData") {
            const { formData } = tabToProcess;
            httpRequestBody = this.extractBody(formData);
        }
        if (bodyType === "rawData") {
            const { rawData } = tabToProcess;
            httpRequestBody = this.extractBody(rawData);
        }
        if (this.isgRPCRequest(tabToProcess)) {
            const { grpcData } = tabToProcess;
            if(!isValidJSON(grpcData)){
                const errorMessage = "Grpc data should be valid JSON object";
                alert(errorMessage);
                throw new Error(errorMessage);
            }
            httpRequestBody = this.extractBody(grpcData);
        }
        const httpMethod = this.getHttpMethod(tabToProcess);
        const httpRequestURL = tabToProcess.httpURL;

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

        const reqISODate = new Date().toISOString();
        const reqTimestamp = new Date(reqISODate).getTime();

        try {
            
            const reqResPair = tabToProcess.eventData;
            const formattedData = this.getReqResFromTabData(reqResPair, tabToProcess, runId, "History", reqTimestamp, null, httpRequestURLRendered, currentEnvironment, currentEnvironmentVars);
            const preRequestData = {
                requestEvent : formattedData.request,
                environmentName: selectedEnvironment,
                injectionConfigVersion: `Default${selectedApp}`,
                contextMap:  contextMap || {},
            }
            const preRequestResult = await cubeService.fetchPreRequest(userHistoryCollection.id, runId, preRequestData, selectedApp, tabToProcess.abortRequest.cancelToken);
        
            [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] = preRequestToFetchableConfig(preRequestResult, httpRequestURL);

        } catch (e) {
            console.error(e);
            //Fallback to old way of generating request
            try{
                [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] 
                = applyEnvVars(httpRequestURL, httpRequestQueryStringParams, fetchConfig);
            }
            catch(error){
                this.showErrorAlert(`${e}`); // prompt user for error in env vars
                dispatch(httpClientActions.postErrorDriveRequest(tabId, error.message));
                dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                return
            }
        }
        let fetchUrlRendered = httpRequestURLRendered + (Object.keys(httpRequestQueryStringParamsRendered).length ? "?" + stringify(httpRequestQueryStringParamsRendered) : "");
        let fetchedResponseHeaders = {}, responseStatus = "", responseStatusText = "";
        const startDate = new Date(Date.now() - 2 * 1000).toISOString();
        fetchConfigRendered.signal = tabToProcess.abortRequest.signal;
        // TODO: Update this to be visible from UI
        // fetchConfigRendered.headers.append('md-trace-id', encodeURIComponent(`${traceId}:${spanId}:0:1`) );
        let resTimestamp;
        return fetch(fetchUrlRendered, fetchConfigRendered).then(async(response) => {
            const resISODate = new Date().toISOString();
            resTimestamp = new Date(resISODate).getTime();
            responseStatus = response.status;
            responseStatusText = response.statusText;
            for (const header of response.headers) {
                fetchedResponseHeaders[header[0]] = header[1];
            }
            if(fetchedResponseHeaders["content-type"] == "application/grpc"){
                var reader = response.body.getReader();
                var result = await reader.read();
                const base64Data = Base64Binary.encode(result.value || new Uint8Array());
                return base64Data;
            }
            return response.text();
        })
        .then((data) => {
            // handle success
            dispatch(httpClientActions.postSuccessDriveRequest(tabId, responseStatus, responseStatusText, JSON.stringify(fetchedResponseHeaders, undefined, 4), data));
            this.saveToHistoryAndLoadTrace(tabId, userHistoryCollection.id, runId, reqTimestamp, resTimestamp, httpRequestURLRendered, currentEnvironment);
            //dispatch(httpClientActions.unsetReqRunning(tabId, runId))
        })
        .catch((error) => {
            console.error(error);
            dispatch(httpClientActions.postErrorDriveRequest(tabId, error.message));
            dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            if(error.message !== commonConstants.USER_ABORT_MESSAGE){                
                this.showErrorAlert(`Could not get any response. There was an error connecting: ${error}`);
            }
        });
    }

    updateAbortRequest(tabId, abortRequest) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.updateAbortRequest(tabId, abortRequest));
    }

    handleTabChange(tabKey) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.setSelectedTabKey(tabKey));
        dispatch(httpClientActions.setTabIsHighlighted(tabKey, false));
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

    getValueBySaveType(value, type) {
        const renderEnvVars = getRenderEnvVars();
        return type !== "History" ? value : renderEnvVars(value);
    }

    extractHeadersToCubeFormat(headersReceived, type="") {
        let headers = {};
        if (_.isArray(headersReceived)) {
            headersReceived.forEach(each => {
                if (each.name && each.value) {
                    if(headers[each.name]){
                        headers[each.name] = [...headers[each.name], this.getValueBySaveType(each.value, type)];
                    }else{
                        headers[each.name] = [this.getValueBySaveType(each.value, type)];
                    }
                }
            });
        } else if (_.isObject(headersReceived)) {
            Object.keys(headersReceived).map((eachHeader) => {
                if (eachHeader && headersReceived[eachHeader]) {
                    if(_.isArray(headersReceived[eachHeader])) headers[eachHeader] = this.getValueBySaveType(headersReceived[eachHeader], type);
                    if(_.isString(headersReceived[eachHeader])) headers[eachHeader] = [this.getValueBySaveType(headersReceived[eachHeader], type)];
                }
            })
        }

        return headers;
    }

    extractQueryStringParamsToCubeFormat(httpRequestQueryStringParams, type) {
        let qsParams = {};
        httpRequestQueryStringParams.forEach(each => {
            if (each.name && each.value) {
                if(qsParams[each.name]){
                    qsParams[each.name] = [...qsParams[each.name], this.getValueBySaveType(each.value, type)];
                }else{
                    qsParams[each.name] = [this.getValueBySaveType(each.value, type)];
                }
            }
        })
        return qsParams;
    }

    extractBodyToCubeFormat(httpRequestBody, type) {
        let formData = {};
        if (_.isArray(httpRequestBody)) {
            httpRequestBody.forEach(each => {
                if (each.name && each.value) {
                    if(formData[each.name]){
                        formData[each.name] = [...formData[each.name], this.getValueBySaveType(each.value, type)];
                    }else{
                        formData[each.name] = [this.getValueBySaveType(each.value, type)];
                    }
                }
            })
            return formData;
        } else {
            return this.getValueBySaveType(httpRequestBody, type);
        }
    }

    tryJsonParse(jsonString){
        try{
            return JSON.parse(jsonString);
        }catch(e){}
        return jsonString;
    }

    getHttpMethod(tabToSave){
        return this.isgRPCRequest(tabToSave) ? "POST" : tabToSave.httpMethod;
    }

    getPathName(url){
        const urlData = urlParser(url);
        return _.trim(urlData.pathname, '/');
    }


    getReqResFromTabData(eachPair, tabToSave, runId, type, reqTimestamp, resTimestamp, urlEnvVal, currentEnvironment) {
        const { headers, queryStringParams, bodyType, rawDataType, responseHeaders, responseBody, recordedResponseHeaders, recordedResponseBody, responseStatus } = tabToSave;

        const httpRequestEventTypeIndex = eachPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        let httpRequestEvent = eachPair[httpRequestEventTypeIndex];
        let httpResponseEvent = eachPair[httpResponseEventTypeIndex];
        
        // let apiPath = getApiPathFromRequestEvent(httpRequestEvent); // httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : "";
        // let apiPath = this.getPathName(applyEnvVarsToUrl(tabToSave.httpURL));
        const parsedUrl = urlParser(applyEnvVarsToUrl(tabToSave.httpURL), PLATFORM_ELECTRON ? {} : true);
        let apiPath = _.trim(generateApiPath(parsedUrl), '/');
        httpRequestEvent.metaData.httpURL = tabToSave.httpURL;
        httpResponseEvent.metaData.httpURL = tabToSave.httpURL;

        if(httpRequestEvent.reqId === "NA") {
            let service = httpRequestEvent.service != "NA" ? httpRequestEvent.service : (parsedUrl.host || "NA");
            httpRequestEvent = this.updateHttpEvent(apiPath, service, httpRequestEvent);
            httpResponseEvent = this.updateHttpEvent(apiPath, service, httpResponseEvent);
            httpRequestEvent.metaData.typeOfRequest = "devtool";
        } else {
            if(!httpRequestEvent.metaData.typeOfRequest) httpRequestEvent.metaData.typeOfRequest = "apiCatalog";
            httpRequestEvent = this.updateHttpEvent(apiPath, "", httpRequestEvent);
            httpResponseEvent = this.updateHttpEvent(apiPath, "", httpResponseEvent);
        }

        if(httpRequestEvent.parentSpanId === null) {
            httpRequestEvent.parentSpanId = "NA"
        }

        if(httpRequestEvent.spanId === null) {
            httpRequestEvent.spanId = "NA"
        }

        if(type === "History") {
            httpRequestEvent.metaData.collectionId = tabToSave.collectionIdAddedFromClient;
            httpRequestEvent.metaData.requestId = tabToSave.requestId;
            if(urlEnvVal) {
                const path = this.getPathName(urlEnvVal);
                httpRequestEvent.apiPath = path;
                httpResponseEvent.apiPath = path;
                httpRequestEvent.metaData.href = urlEnvVal;
            }
            if(currentEnvironment) {
                httpRequestEvent.metaData.currentEnvironment = currentEnvironment;
            }
            const renderEnvVars = getRenderEnvVars();
            apiPath = renderEnvVars(apiPath);
        }

        httpRequestEvent.metaData.hdrs = JSON.stringify(unSelectedRequestParamData(headers));
        httpRequestEvent.metaData.queryParams = JSON.stringify(unSelectedRequestParamData(queryStringParams));
        
        const httpReqestHeaders = this.extractHeadersToCubeFormat(selectedRequestParamData(headers), type);
        const httpRequestQueryStringParams = this.extractQueryStringParamsToCubeFormat(selectedRequestParamData(queryStringParams), type);
        let httpRequestFormParams = {}, httpRequestBody = "";
        if (bodyType === "formData") {
            const { formData } = tabToSave;
            httpRequestEvent.metaData.formParams = JSON.stringify(unSelectedRequestParamData(formData));
            httpRequestFormParams = this.extractBodyToCubeFormat(selectedRequestParamData(formData), type);
        }
        if (bodyType === "rawData") {
            const { rawData } = tabToSave;
            httpRequestBody = this.extractBodyToCubeFormat(rawData, type);
        }
        if (this.isgRPCRequest(tabToSave)) {
            const { grpcData } = tabToSave;
            httpRequestBody = this.tryJsonParse(grpcData);  
            httpReqestHeaders["content-type"] = ["application/grpc"];          
        }
        const httpMethod = this.getHttpMethod(tabToSave);
        let httpResponseHeaders, httpResponseBody, httpResponseStatus;
        if (type !== "History") {
            httpResponseHeaders = recordedResponseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : null;
            httpResponseBody = recordedResponseBody ? this.tryJsonParse(recordedResponseBody) : responseBody ? this.tryJsonParse(responseBody) : null;
            httpResponseStatus = httpResponseEvent.payload[1].status
        } else {
            httpResponseHeaders = responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : recordedResponseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : null;
            httpResponseBody = responseBody ? this.tryJsonParse(responseBody) : recordedResponseBody ? this.tryJsonParse(recordedResponseBody) : null;
            httpResponseStatus = responseStatus;
        }
        const reqResCubeFormattedData = {   
            request: {
                ...httpRequestEvent,
                ...(reqTimestamp && { timestamp: reqTimestamp }),
                runId: runId,
                runType: "DevTool",
                payload: [
                    this.isgRPCRequest(tabToSave) ? "GRPCRequestPayload": "HTTPRequestPayload",
                    {
                        hdrs: httpReqestHeaders,
                        queryParams: httpRequestQueryStringParams,
                        formParams: httpRequestFormParams,
                        ...(httpRequestBody && { body: httpRequestBody }),
                        method: httpMethod.toUpperCase(),
                        path: apiPath,
                        pathSegments: apiPath.split("/"),
                        payloadState :  this.isgRPCRequest(tabToSave) ? "UnwrappedDecoded": "WrappedDecoded",
                    }
                ]
            },
            response: {
                ...httpResponseEvent,
                ...(resTimestamp && { timestamp: resTimestamp }),
                runId: runId,
                runType: "DevTool",
                payload: [
                    this.isgRPCRequest(tabToSave) ? "GRPCResponsePayload": "HTTPResponsePayload",
                    {
                        hdrs: httpResponseHeaders,
                        body: httpResponseBody,
                        status: httpResponseStatus,
                        statusCode: String(httpResponseStatus),
                        path: apiPath,
                        payloadState : "WrappedDecoded",
                    }
                ]
            }
        }
        return reqResCubeFormattedData;
    }

    saveToHistoryAndLoadTrace = (tabId, recordingId, runId="", reqTimestamp="", resTimestamp="", urlEnvVal="", currentEnvironment="") => {
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
            return;
        }
        
        const reqResPair = tabToProcess.eventData;
        
        try {
            if (reqResPair.length > 0) {
                const data = [];
                data.push(this.getReqResFromTabData(reqResPair, tabToProcess, runId, "History", reqTimestamp, resTimestamp, urlEnvVal, currentEnvironment));
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
                            const apiPath = _.trimStart(data[0].request.apiPath, '/');
                            this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig);
                            setTimeout(() => {
                                this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig, true);
                            }, 5000);

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

    formatHttpEventToReqResObject(reqId, httpEventReqResPair, isOutgoingRequest=false) {
        const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
        const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];

        const { headers, queryParams, formData, rawData, rawDataType, grpcData, grpcDataType }  = extractParamsFromRequestEvent(httpRequestEvent);
        
        let reqObject = {
            id: uuidv4(),
            httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
            httpURL: httpRequestEvent.metaData.httpURL || httpRequestEvent.apiPath,
            httpURLShowOnly: httpRequestEvent.metaData.httpURL || httpRequestEvent.apiPath,
            headers: headers,
            queryStringParams: queryParams,
            bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : grpcData && grpcData.length > 0 ? "grpcData" : "formData",
            formData: formData,
            rawData: rawData,
            rawDataType: rawDataType,
            grpcData: grpcData,
            grpcDataType: grpcDataType,
            paramsType: "showQueryParams",
            responseStatus: "NA",
            responseStatusText: "",
            responseHeaders: "",
            responseBody: "",
            recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
            recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
            recordedResponseStatus: httpResponseEvent ? httpResponseEvent.payload[1].status : "",
            responseBodyType: "json",
            requestId: reqId,
            outgoingRequestIds: [],
            eventData: httpEventReqResPair,
            showOutgoingRequestsBtn: false,
            showSaveBtn: true,
            outgoingRequests: [],
            showCompleteDiff: false,
            isOutgoingRequest: isOutgoingRequest,
            service: httpRequestEvent.service,
            recordingIdAddedFromClient: "",
            collectionIdAddedFromClient: httpRequestEvent.collection,
            traceIdAddedFromClient: httpRequestEvent.traceId,
            apiPath: httpRequestEvent.apiPath,
            requestRunning: false,
            showTrace: null,
            metaData: httpResponseEvent ? httpResponseEvent.metaData : {}
        };
        return reqObject;
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
                    reqIdArray.push(apiTrace.res[0].requestEventId);
                    apiTrace.res.reverse().pop();
                    apiTrace.res.forEach((eachApiTraceEvent) => {
                        reqIdArray.push(eachApiTraceEvent.requestEventId);
                    });
                }

                if (reqIdArray && reqIdArray.length > 0) {
                    const eventTypes = [];
                    cubeService.fetchAPIEventData(customerId, app, reqIdArray, eventTypes, apiConfig).then((result) => {
                        if(result && result.numResults > 0) {
                            const ingressReqResPair = result.objects.filter(eachReq => eachReq.apiPath === apiPath);
                            let ingressReqObj;
                            if (ingressReqResPair.length > 0) {
                                ingressReqObj = this.formatHttpEventToReqResObject(reqId, ingressReqResPair, false);
                            }
                            for (let eachReqId of reqIdArray) {
                                const reqResPair = result.objects.filter(eachReq => {
                                    return (eachReq.reqId === eachReqId && eachReq.apiPath !== apiPath);
                                });
                                if (reqResPair.length > 0 && eachReqId !== reqId) {
                                    let reqObject = this.formatHttpEventToReqResObject(eachReqId, reqResPair, true);
                                    ingressReqObj.outgoingRequests.push(reqObject);
                                }
                            }
                            dispatch(httpClientActions.postSuccessLoadRecordedHistory(tabId, ingressReqObj, runId));
                            if(!isRefetchTrace) dispatch(httpClientActions.unsetReqRunning(tabId, runId));
                        }
                    });
                }
            }, (err) => {
                console.error("err: ", err);
                dispatch(httpClientActions.unsetReqRunning(tabId, runId));
            })
    }


    addTab(evt, reqObject, givenApp, isSelected = true) {
        let traceId;
        let spanId;
        const httpRequestEventIndex = 0;
        const { dispatch, user, httpClient: {selectedTabKey} } = this.props;
        const tabId = uuidv4();
        const requestId = uuidv4();
        const { app } = this.state;
        const appAvailable = givenApp ? givenApp : app ? app : "";
        if (!reqObject) {
            traceId = cryptoRandomString({length: 16});
            spanId = cryptoRandomString({length: 16});
            const { cube: { selectedApp } } = this.props;
            const customerId = user.customer_name;
            const eventData = this.generateEventdata(selectedApp, customerId, traceId);
            const mdTraceHeader = {
                description: "",
                id: uuidv4(),
                name: "md-trace-id",
                selected: true,
                value: encodeURIComponent(`${traceId}:${spanId}:0:1`)
            };
            reqObject = {
                httpMethod: "get",
                httpURL: "",
                httpURLShowOnly: "",
                headers: [mdTraceHeader],
                queryStringParams: [],
                bodyType: "formData",
                formData: [],
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
                requestId: "",
                outgoingRequestIds: [],
                eventData: eventData,
                showOutgoingRequestsBtn: false,
                showSaveBtn: true,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: "",
                recordingIdAddedFromClient: "",
                collectionIdAddedFromClient: "",
                traceIdAddedFromClient: traceId,
                recordedHistory: null
            };
        } else {
            traceId = reqObject.eventData[httpRequestEventIndex].traceId;
            spanId = reqObject.eventData[httpRequestEventIndex].spanId;
    
            const mdTraceHeader = { 
                description: "",
                id: uuidv4(),
                name: "md-trace-id",
                selected: true,
                value: encodeURIComponent(`${traceId}:${spanId}:0:1`) 
            }
            reqObject.headers.push(mdTraceHeader);
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
                    reqIdArray.splice(indx, 1);
                    tabsToHighlight.push(eachTab.id);
                }
            });

            tabsToHighlight.forEach( tabId => {
                if(selectedTabKey !== tabId || reqIdArray.length > 0) {
                    dispatch(httpClientActions.setTabIsHighlighted(tabId, true));
                }
            });

            const eventTypes = [];
            cubeService.fetchAPIEventData(customerId, selectedApp, reqIdArray, eventTypes).then((result) => {
                if (result && result.numResults > 0) {
                    for (let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if (reqResPair.length > 0) {
                            let reqObject = formatHttpEventToTabObject(eachReqId, requestIds, reqResPair);
                            const savedTabId = this.addTab(null, reqObject, selectedApp, eachReqId == reqIdArray[reqIdArray.length - 1]);
                            this.showOutgoingRequests(savedTabId, reqObject.traceIdAddedFromClient, reqObject.collectionIdAddedFromClient, reqObject.recordingIdAddedFromClient);
                        }
                    }
                }
            });
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
        const {httpClient: {selectedTabKey, cubeRunHistory}} = this.props;
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

                            addOrRemoveParam={this.addOrRemoveParam} 
                            updateParam={this.updateParam}
                            updateAllParams={this.updateAllParams}
                            updateBodyOrRawDataType={this.updateBodyOrRawDataType}
                            driveRequest={this.driveRequest}
                            getReqResFromTabData={this.getReqResFromTabData.bind(this)}
                            handleRowClick={this.handleRowClick}
                            handleTestRowClick={this.handleTestRowClick}
                            setAsReference={this.setAsReference}
                            cubeRunHistory={cubeRunHistory}
                            showAddMockReqModal={this.showAddMockReqModal}
                            handleDuplicateTab={this.handleDuplicateTab}
                            toggleShowTrace={this.toggleShowTrace}
                            updateAbortRequest={this.updateAbortRequest}
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
        const { cube } = this.props;
        const { showErrorModal, errorMsg, importedToCollectionId, serializedCollection, modalErrorImportCollectionMessage, showImportModal, curlCommand, modalErrorImportFromCurlMessage } = this.state;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const {httpClient: { userCollections, tabs, selectedTabKey, showAddMockReqModal, mockRequestServiceName, mockRequestApiPath, modalErrorAddMockReqMessage}} = this.props;

        return (

            <div className="http-client" style={{ display: "flex", height: "100%" }}>
                <aside className="" ref={e=> (this.sliderRef = e)}
                style={{ "width": "250px", "height": "100%", "background": "#EAEAEA", "padding": "10px", "display": "flex", "flexDirection": "column", overflow: "auto" }}>
                    <div style={{ marginTop: "10px", marginBottom: "10px" }}>
                        <div className="label-n">APPLICATION</div>
                        <div className="application-name">{app}</div>
                    </div>
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
                    <div style={{marginRight: "7px"}}>
                        <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}></div>
                        <div style={{display: "inline-block", width: "80%", textAlign: "right"}}>
                            <div className="btn btn-sm cube-btn text-center" style={{ padding: "2px 10px", display: "inline-block"}} onClick={this.handleImportModalShow}>
                                <Glyphicon glyph="import" /> Import
                            </div>
                            <MockConfigSection />
                            <EnvironmentSection />
                        </div>
                    </div>
                    <div style={{marginTop: "10px", display: ""}}>
                        <ResponsiveTabs
                            allowRemove={true} 
                            onAddClick={this.addTab}
                            removeActiveOnly={false} 
                            onChange={this.handleTabChange} 
                            onRemove={this.handleRemoveTab}
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
