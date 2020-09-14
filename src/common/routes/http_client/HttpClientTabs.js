import React, { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Tabs, Tab, Panel, Label, Modal, Button, ControlLabel, Glyphicon } from 'react-bootstrap';
import { Treebeard, decorators } from 'react-treebeard';

import _, { head } from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { stringify, parse } from 'query-string';
import arrayToTree from 'array-to-tree';
import * as moment from 'moment';
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
import TreeNodeContainer from "./TreeNodeContainer";
import TreeNodeToggle from "./TreeNodeToggle";
import ResponsiveTabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";
import CollectionTreeCSS from "./CollectionTreeCSS";

import EnvVar from "./EnvVar";
import Mustache from "mustache";
import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { httpClientActions } from "../../actions/httpClientActions";
import { generateRunId } from "../../utils/http_client/utils"; 
import { parseCurlCommand } from '../../utils/http_client/curlparser';

import SplitSlider  from '../../components/SplitSlider.js';

import commonConstants from '../../utils/commonConstants';


class HttpClientTabs extends Component {

    constructor(props, context) {
        super(props, context);

        this.state = { 
            showEnvVarModal: false,
            showSelectedEnvModal: false,
            showErrorModal: false,
            errorMsg: "",
            showDeleteGoldenConfirmation:false,
            itemToDelete: {},
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
        this.saveToCollection = this.saveToCollection.bind(this);

        this.onToggle = this.onToggle.bind(this);
        this.handlePanelClick = this.handlePanelClick.bind(this);


        this.handleCloseModal = this.handleCloseModal.bind(this);
        this.showSaveModal = this.showSaveModal.bind(this);
        this.handleSave = this.handleSave.bind(this);

        this.handleChange = this.handleChange.bind(this);
        this.handleCreateCollection = this.handleCreateCollection.bind(this);
        this.handleTreeNodeClick = this.handleTreeNodeClick.bind(this);
        this.renderTreeNodeHeader = this.renderTreeNodeHeader.bind(this);

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
        this.persistPanelState = [];
    }

    createRecordedDataForEachRequest(toBeUpdatedData, toBeCopiedFromData) {
        let referenceEventData = toBeCopiedFromData ? toBeCopiedFromData.eventData : null,
            eventData = toBeUpdatedData.eventData;
        if(referenceEventData && referenceEventData.length > 0) {
            let refHttpRequestEventTypeIndex = referenceEventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let refHttpResponseEventTypeIndex = refHttpRequestEventTypeIndex === 0 ? 1 : 0;
            let refHttpResponseEvent = referenceEventData[refHttpResponseEventTypeIndex];
            let refHttpRequestEvent = referenceEventData[refHttpRequestEventTypeIndex];

            let httpRequestEventTypeIndex = eventData[0].eventType === "HTTPRequest" ? 0 : 1;
            let httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
            let gatewayHttpResponseEvent = eventData[httpResponseEventTypeIndex];
            let gatewayHttpRequestEvent = eventData[httpRequestEventTypeIndex];

            let httpResponseEvent = {
                customerId: eventData.customerId,
                app: eventData.app,
                service: refHttpRequestEvent.service,
                instanceId: eventData.custominstanceIderId,
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
                recordingType: eventData.recordingType,
                metaData: {

                }
            };
            
            let httpRequestEvent = {
                customerId: eventData.customerId,
                app: eventData.app,
                service: refHttpRequestEvent.service,
                instanceId: eventData.custominstanceIderId,
                collection: toBeUpdatedData.collectionIdAddedFromClient,
                traceId: toBeUpdatedData.traceIdAddedFromClient,
                spanId: cryptoRandomString({length: 16}),
                parentSpanId: eventData[httpRequestEventTypeIndex].parentSpanId,
                runType: refHttpRequestEvent.runType,
                runId: null,
                timestamp: refHttpRequestEvent.timestamp,
                reqId: "NA",
                apiPath: refHttpRequestEvent.apiPath,
                eventType: "HTTPRequest",
                payload: refHttpRequestEvent.payload,
                recordingType: eventData.recordingType,
                metaData: {

                }
            };

            let tabData = {
                tabId: uuidv4(),
                requestId: toBeCopiedFromData.reqId,
                httpMethod: toBeCopiedFromData.httpMethod,
                httpURL: toBeCopiedFromData.httpURL,
                headers: toBeCopiedFromData.headers,
                queryStringParams: toBeCopiedFromData.queryStringParams,
                bodyType: toBeCopiedFromData.bodyType,
                formData: toBeCopiedFromData.formData,
                rawData: toBeCopiedFromData.rawData,
                rawDataType: toBeCopiedFromData.rawDataType,
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
                clearIntervalHandle: null,
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
                requestId: toBeUpdatedData.requestId,
                httpMethod: toBeCopiedFromData.httpMethod,
                httpURL: toBeCopiedFromData.httpURL,
                httpURLShowOnly: toBeUpdatedData.httpURLShowOnly,
                headers: toBeCopiedFromData.headers,
                queryStringParams: toBeCopiedFromData.queryStringParams,
                bodyType: toBeCopiedFromData.bodyType,
                formData: toBeCopiedFromData.formData,
                rawData: toBeCopiedFromData.rawData,
                rawDataType: toBeCopiedFromData.rawDataType,
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
                clearIntervalHandle: toBeUpdatedData.clearIntervalHandle,
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
                tabToBeProcessed.outgoingRequests.splice(matchedReqIndex, 1);
            } else {
                const copiedOutgoingData = this.createRecordedDataForEachRequest(tabToBeProcessed, eachReq);
                outgoingRequests.push(copiedOutgoingData);
            }
        })
        copiedTab.outgoingRequests = [...tabToBeProcessed.outgoingRequests, ...outgoingRequests];
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

    getParameterCaseInsensitive (object, key) {
        return object[
            Object.keys(object)
            .find(k => k.toLowerCase() === key.toLowerCase())
        ];
    }

    importFromCurl(curlCommand) {
        const { dispatch } = this.props;
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
            const user = JSON.parse(localStorage.getItem('user'));
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
            let contentTypeHeader = _.isObject(parsedCurl.headers) ? this.getParameterCaseInsensitive(parsedCurl.headers, "content-type") : "";
            if(contentTypeHeader && contentTypeHeader.indexOf("application/json") > -1) {
                rawData = JSON.stringify(JSON.parse(parsedCurl.data), undefined, 4);
                rawDataType = "json";
                bodyType = "rawData";
            } else if(contentTypeHeader && contentTypeHeader.indexOf("application/x-www-form-urlencoded") > -1) {
                const formParams = new URLSearchParams(parsedCurl.data);
                for (let eachFormParam of formParams) {
                    formData.push({
                        id: uuidv4(),
                        name: eachFormParam,
                        value: formParams.get(eachFormParam),
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
                clearIntervalHandle: null,
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
        const {httpClient: {tabs, mockReqServiceName, mockReqApiPath}} = this.props;
        const { dispatch } = this.props;
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

            const user = JSON.parse(localStorage.getItem('user'));
            const customerId = user.customer_name;
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
                runType: "Manual",
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
                runType: "Manual",
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
                recordedHistory: null,
                clearIntervalHandle: null
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
            runType: "Manual",
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
            runType: "Manual",
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
        const { dispatch } = this.props;
        dispatch(httpClientActions.setSelectedTraceTableReqTabId(selectedTraceTableReqTabId, tabId));
    }

    handleTestRowClick(selectedTraceTableTestReqTabId, tabId) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.setSelectedTraceTableTestReqId(selectedTraceTableTestReqTabId, tabId));
    }

    handleCloseModal() {
        const { dispatch } = this.props;
        dispatch(httpClientActions.closeSaveModal(false));
    }

    showSaveModal(isOutgoingRequest, tabId) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.showSaveModal(tabId, true, "", "", "",false, ""));
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
                const user = JSON.parse(localStorage.getItem('user'));
                const customerId = user.customer_name;
                
                let headers = {}, queryParams = {}, formData = {}, rawData = "";
                for (let eachHeader in requestHeaders) {
                    headers[eachHeader] = [requestHeaders[eachHeader]];
                }
                for (let eachQueryParam in parsedQueryParams) {
                    queryParams[eachQueryParam] = _.isArray(parsedQueryParams[eachQueryParam]) ? parsedQueryParams[eachQueryParam] : [parsedQueryParams[eachQueryParam]] ;
                }
                if(requestBody && requestBody.mode) {
                    let contentTypeHeader = _.isObject(requestHeaders) ? this.getParameterCaseInsensitive(requestHeaders, "content-type") : "";
                    if(contentTypeHeader && contentTypeHeader.indexOf("application/json") > -1 && requestBody.mode === "raw" && requestBody.raw) {
                        rawData = JSON.parse(requestBody.raw);
                    } else if(requestBody.mode === "urlencoded") {
                        for (let eachFormParam of requestBodyUrlEncodedParams) {
                            formData[eachFormParam.key] = [eachFormParam.value];
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
        const urlToPost = `${config.apiBaseUrl}/cs/storeUserReqResp/${importedToCollectionId}`;
        const apiConfig = {};
        api.post(urlToPost, httpEventPairs, apiConfig)
            .then((serverRes) => {
                this.setState({
                    showImportModal: false,
                    serializedCollection: "",
                    importedToCollectionId: "",
                    modalErrorImportCollectionMessage: "Saved."
                });
            }, (error) => {
                console.error("error: ", error);
                this.setState({
                    showImportModal: true,
                    modalErrorImportCollectionMessage: error
                });
            })
        return httpEventPairs;
    }

    onToggle(node, toggled){
        const {httpClient: {historyCursor}} = this.props;
        const { dispatch } = this.props;
        if (historyCursor) {
            dispatch(httpClientActions.setInactiveHistoryCursor(historyCursor, false));
            /* if (!_.includes(cursor.children, node)) {
                cursor.toggled = false;
                cursor.active = false;
            } */
        }
        node.active = true;
        if (node.children) {
            node.toggled = toggled;
            if(node.isCubeRunHistory){
                node.children.forEach(u=> u.isCubeRunHistory = true);
            }
        }
        if(node.requestEventId){
            this.persistPanelState[node.requestEventId] = toggled;
        }
        dispatch(httpClientActions.setActiveHistoryCursor(node));
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
        const {httpClient: {tabs}} = this.props, tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const { dispatch } = this.props;
        const reqIdArray = tabs[tabIndex]["outgoingRequestIds"];
        if(tabs[tabIndex]["outgoingRequests"] && tabs[tabIndex]["outgoingRequests"].length > 0) {
            return;
        };
        if (reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(app, reqIdArray, eventTypes).then((result) => {
                if (result && result.numResults > 0) {
                    let outgoingRequests = [];
                    for (let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);                    
                        if (reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            
                            let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
                            
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: "",
                                    selected: true,
                                });
                            }
                            for (let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                                    description: "",
                                    selected: true,
                                });
                            }
                            for (let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: "",
                                    selected: true,
                                });
                                rawDataType = "";
                            }
                            if (httpRequestEvent.payload[1].body) {
                                if (!_.isString(httpRequestEvent.payload[1].body)) {
                                    try {
                                        rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                                        rawDataType = "json";
                                    } catch (err) {
                                        console.error(err);
                                    }
                                } else {
                                    rawData = httpRequestEvent.payload[1].body;
                                    rawDataType = "text";
                                }
                            }
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

    applyEnvVars = (httpRequestURL, httpRequestQueryStringParams, fetchConfig) => {
        const headers = fetchConfig.headers;
        const body = fetchConfig.body;
        let headersRendered = new Headers(), bodyRendered = "";

        const currentEnvironment = this.getCurrentEnvirnoment();

        // convert list of envvar objects to a map
        const currentEnvVars = currentEnvironment ? Object.fromEntries(
            currentEnvironment.vars.map((v) => ([v.key, v.value]))
        ) : {};

        // define method to check and render Mustache template
        const renderEnvVars = (input) => {
            if (!input) return input;

            // get variables in the input string
            // let pInput = Mustache.parse(input);
            // console.log(pInput);
            let inputVariables = Mustache.parse(input)
                    .filter(v => (v[0]==='name') || v[0]==='&' || v[0]==='#')
                    .map(v => v[1]);
        
            // check for the presence of variables in the environment
            inputVariables.forEach((inputVariable) => {
                if (!currentEnvVars.hasOwnProperty(inputVariable)) {
                    throw new Error("The variable '" + inputVariable + "' is not defined in the current environment. \nPlease check your environment and the variables being used.")
                }
            })

            return Mustache.render(input, currentEnvVars);
        }

        
        const httpRequestURLRendered = renderEnvVars(httpRequestURL);

        const httpRequestQueryStringParamsRendered = Object.fromEntries(
            Object.entries(httpRequestQueryStringParams)
                .map(
                    ([key, value]) => [renderEnvVars(key), renderEnvVars(value)]
                )
        );

        if (headers) {
            for (let pair of headers.entries()) {
                headersRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
            }
        }

        if (body instanceof URLSearchParams) {
            bodyRendered = new URLSearchParams();
            for (let pair of body.entries()) {
                bodyRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
            }
        } else {
            // string
            bodyRendered = renderEnvVars(body)
        }

        const fetchConfigRendered = {
            method: fetchConfig.method,
            headers: headersRendered,
            body: bodyRendered,
        }

        return [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered]
    }

    driveRequest(isOutgoingRequest, tabId) {
        const {httpClient: {tabs, selectedTabKey, userHistoryCollection}} = this.props;
        const { cube: { selectedApp } } = this.props;
        const { dispatch } = this.props;
        const user = JSON.parse(localStorage.getItem('user'));
        const userId = user.username,
            customerId = user.customer_name;
        let tabsToProcess = tabs;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        if(tabIndex < 0) return;
        dispatch(httpClientActions.resetRunState(tabId))
        // generate a new run id every time a request is run
        const runId = generateRunId();
        const spanId = tabToProcess.eventData[0].spanId;

        if(PLATFORM_ELECTRON) {
            const mockContext = {
                collectionId: userHistoryCollection.collec,
                // recordingId: this.state.tabs[tabIndex].recordingIdAddedFromClient,
                recordingCollectionId: tabs[tabIndex].collectionIdAddedFromClient,
                traceId: tabs[tabIndex].traceIdAddedFromClient,
                selectedApp,
                customerName: customerId,
                runId: runId,
                spanId: spanId
            }

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
        const httpMethod = tabToProcess.httpMethod;
        const httpRequestURL = tabToProcess.httpURL;

        let fetchConfig = {
            method: httpMethod,
            headers: httpReqestHeaders
        }
        if (httpMethod !== "GET".toLowerCase() && httpMethod !== "HEAD".toLowerCase()) {
            fetchConfig["body"] = httpRequestBody;
        }
        
        // render environment variables
        let httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered;
        try {
            [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] 
                        = this.applyEnvVars(httpRequestURL, httpRequestQueryStringParams, fetchConfig);
        } catch (e) {
            this.showErrorAlert(`${e}`); // prompt user for error in env vars
            return
        }
        const fetchUrlRendered = httpRequestURLRendered + (httpRequestQueryStringParamsRendered ? "?" + stringify(httpRequestQueryStringParamsRendered) : "");
        dispatch(httpClientActions.preDriveRequest(tabId, "WAITING...", false));
        dispatch(httpClientActions.setReqRunning(tabId))
        tabs.map(eachTab => {
            if (eachTab.id === tabId) {
                if(eachTab["clearIntervalHandle"]) clearInterval(eachTab["clearIntervalHandle"]);
            }
            return eachTab; 
        });
        // Make request
        // https://www.mocky.io/v2/5185415ba171ea3a00704eed
        let fetchedResponseHeaders = {}, responseStatus = "", responseStatusText = "";
        const startDate = new Date(Date.now() - 2 * 1000).toISOString();
        fetchConfigRendered.signal = tabToProcess.abortRequest.signal;
        return fetch(fetchUrlRendered, fetchConfigRendered).then((response) => {
            responseStatus = response.status;
            responseStatusText = response.statusText;
            for (const header of response.headers) {
                fetchedResponseHeaders[header[0]] = header[1];
            }
            if(response.headers.get("content-type").indexOf("text/html") !== -1) {
                return response.text();
            } else if (response.headers.get("content-type").indexOf("application/json") !== -1 ) {// checking response header
                return response.json();
            } else {
                return response.text();
                //throw new TypeError('Response from has unexpected "content-type"');
            }
        })
        .then((data) => {
            // handle success
            dispatch(httpClientActions.postSuccessDriveRequest(tabId, responseStatus, responseStatusText, JSON.stringify(fetchedResponseHeaders, undefined, 4), JSON.stringify(data, undefined, 4)));
            this.saveToCollection(isOutgoingRequest, tabId, userHistoryCollection.id, "History", runId);
            //dispatch(httpClientActions.unsetReqRunning(tabId))
        })
        .catch((error) => {
            console.error(error);
            dispatch(httpClientActions.postErrorDriveRequest(tabId, error.message));
            dispatch(httpClientActions.unsetReqRunning(tabId));
            if(error.message !== commonConstants.USER_ABORT_MESSAGE){                
                this.showErrorAlert(`Could not get any response. There was an error connecting: ${error}`);
            }
        });
    }

    handleTabChange(tabKey) {
        const { dispatch } = this.props;
        dispatch(httpClientActions.setSelectedTabKey(tabKey));
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
        if (tabToProcess && tabToProcess.clearIntervalHandle) clearInterval(tabToProcess.clearIntervalHandle);

        // create a new array without [indexToRemove] item
        const newTabs = [...currentTabs.slice(0, indexToRemove), ...currentTabs.slice(indexToRemove + 1)];

        const nextSelectedIndex = newTabs[indexToRemove] ? indexToRemove : indexToRemove - 1;
        if (!newTabs[nextSelectedIndex]) {
            alert('You can not delete the last tab!');
            return;
        }
        
        dispatch(httpClientActions.removeTab(newTabs, newTabs[nextSelectedIndex].id));
    }

    extractHeadersToCubeFormat(headersReceived) {
        let headers = {};
        if (_.isArray(headersReceived)) {
            headersReceived.forEach(each => {
                if (each.name && each.value) headers[each.name] = each.value.split(",");
            });
        } else if (_.isObject(headersReceived)) {
            Object.keys(headersReceived).map((eachHeader) => {
                if (eachHeader && headersReceived[eachHeader]) headers[eachHeader] = headersReceived[eachHeader];
            })
        }

        return headers;
    }

    extractQueryStringParamsToCubeFormat(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams.forEach(each => {
            if (each.name && each.value) qsParams[each.name] = [each.value];
        })
        return qsParams;
    }

    extractBodyToCubeFormat(httpRequestBody) {
        let formData = {};
        if (_.isArray(httpRequestBody)) {
            httpRequestBody.forEach(each => {
                if (each.name && each.value) formData[each.name] = each.value.split(",");
            })
            return formData;
        } else {
            return httpRequestBody;
        }
    }

    getReqResFromTabData(eachPair, tabToSave, runId, type) {
        const httpRequestEventTypeIndex = eachPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        let httpRequestEvent = eachPair[httpRequestEventTypeIndex];
        let httpResponseEvent = eachPair[httpResponseEventTypeIndex];

        let apiPath = httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : "";

        if(httpRequestEvent.reqId === "NA") {
            const parsedUrl = urlParser(tabToSave.httpURL, true);
            apiPath = parsedUrl.pathname ? parsedUrl.pathname : parsedUrl.host;
            let service = parsedUrl.host ? parsedUrl.host : "NA";
            httpRequestEvent = this.updateHttpEvent(apiPath, service, httpRequestEvent);
            httpResponseEvent = this.updateHttpEvent(apiPath, service, httpResponseEvent);
        }

        if(httpRequestEvent.parentSpanId === null) {
            httpRequestEvent.parentSpanId = "NA"
        }

        if(httpRequestEvent.spanId === null) {
            httpRequestEvent.spanId = "NA"
        }

        const { headers, queryStringParams, bodyType, rawDataType, responseHeaders, responseBody, recordedResponseHeaders, recordedResponseBody, responseStatus } = tabToSave;
        const httpReqestHeaders = this.extractHeadersToCubeFormat(headers);
        const httpRequestQueryStringParams = this.extractQueryStringParamsToCubeFormat(queryStringParams);
        let httpRequestFormParams = {}, httpRequestBody = "";
        if (bodyType === "formData") {
            const { formData } = tabToSave;
            httpRequestFormParams = this.extractBodyToCubeFormat(formData);
        }
        if (bodyType === "rawData") {
            const { rawData } = tabToSave;
            httpRequestBody = this.extractBodyToCubeFormat(rawData);
        }
        const httpMethod = tabToSave.httpMethod;
        let httpResponseHeaders, httpResponseBody, httpResponseStatus;
        if (type !== "History") {
            httpResponseHeaders = recordedResponseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : null;
            httpResponseBody = recordedResponseBody ? JSON.parse(recordedResponseBody) : responseBody ? JSON.parse(responseBody) : null;
            httpResponseStatus = httpResponseEvent.payload[1].status
        } else {
            httpResponseHeaders = responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : recordedResponseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : null;
            httpResponseBody = responseBody ? JSON.parse(responseBody) : recordedResponseBody ? JSON.parse(recordedResponseBody) : null;
            httpResponseStatus = responseStatus
        }
        const reqResCubeFormattedData = {   
            request: {
                ...httpRequestEvent,
                runId: runId,
                payload: [
                    "HTTPRequestPayload",
                    {
                        hdrs: httpReqestHeaders,
                        queryParams: httpRequestQueryStringParams,
                        formParams: httpRequestFormParams,
                        ...(httpRequestBody && { body: httpRequestBody }),
                        method: httpMethod.toUpperCase(),
                        path: apiPath,
                        pathSegments: apiPath.split("/")
                    }
                ]
            },
            response: {
                ...httpResponseEvent,
                runId: runId,
                payload: [
                    "HTTPResponsePayload",
                    {
                        hdrs: httpResponseHeaders,
                        body: httpResponseBody,
                        status: httpResponseStatus,
                        statusCode: String(httpResponseStatus),
                    }
                ]
            }
        }
        return reqResCubeFormattedData;
    }

    saveToCollection(isOutgoingRequest, tabId, recordingId, type, runId="") {
        const {httpClient: {tabs, selectedTabKey, showSaveModal}} = this.props;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const {dispatch} = this.props;
        let tabsToProcess = tabs;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        if (!tabToProcess.eventData) return;
        const reqResPair = tabToProcess.eventData;
        try {
            if (reqResPair.length > 0) {
                const data = [];
                data.push(this.getReqResFromTabData(reqResPair, tabToProcess, runId, type));
                if (type !== "History") {
                    tabToProcess.outgoingRequests.forEach((eachOutgoingTab) => {
                        if (eachOutgoingTab.eventData && eachOutgoingTab.eventData.length > 0) {
                            data.push(this.getReqResFromTabData(eachOutgoingTab.eventData, eachOutgoingTab, runId, type));
                        }
                    });
                }
                const urlToPost = `${config.apiBaseUrl}/cs/storeUserReqResp/${recordingId}`;
                const apiConfig = type == "History" ? {cancelToken: tabToProcess.abortRequest.cancelToken}:{};
                const abortRequest = tabToProcess.abortRequest; //After first successful data from getApiTrace, tabToProcess.abortRequest is set to null without cancelling
                api.post(urlToPost, data, apiConfig)
                    .then((serverRes) => {
                        let clearIntervalHandle;
                        if (type === "History") {
                            const jsonTraceReqData = serverRes.data.response && serverRes.data.response.length > 0 ? serverRes.data.response[0] : "";
                            try {
                                const parsedTraceReqData = JSON.parse(jsonTraceReqData);
                                const apiPath = _.trimStart(data[0].request.apiPath, '/');
                                this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig);
                                clearIntervalHandle = setInterval(() => {
                                    if(abortRequest){
                                        if(!abortRequest.isCancelled){
                                            this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig);
                                        }                                        
                                        else if (clearIntervalHandle) {
                                            clearInterval(clearIntervalHandle);
                                        }
                                    }else{
                                        this.loadSavedTrace(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath, apiConfig);
                                    }
                                }, 5000);
                                setTimeout(() => {
                                    if (clearIntervalHandle) clearInterval(clearIntervalHandle);
                                }, 120000);
                            } catch (error) {
                                console.error("Error ", error);
                                throw new Error("Error");
                            }
                            dispatch(httpClientActions.postSuccessSaveToCollection(tabId, false, "Saved Successfully!", clearIntervalHandle));
                        } else {
                            this.updateTabWithNewData(tabId, serverRes, recordingId);
                            dispatch(httpClientActions.postSuccessSaveToCollection(tabId, showSaveModal ? true : false, "Saved Successfully!"));
                        }
                        setTimeout(() => {
                            this.loadFromHistory();
                            this.loadUserCollections();
                            // update api catalog golden and collection lists
                            dispatch(apiCatalogActions.fetchGoldenCollectionList(app, "Golden"))
                            dispatch(apiCatalogActions.fetchGoldenCollectionList(app, "UserGolden"))

                        }, 2000);
                    }, (error) => {
                        dispatch(httpClientActions.unsetReqRunning(tabId));
                        dispatch(httpClientActions.postErrorSaveToCollection(type === "History" ? false : showSaveModal ? true : false, "Error saving: " + error));
                        console.error("error: ", error);
                    })
            } 
        } catch (error) {
            console.error("Error ", error);
            dispatch(httpClientActions.unsetReqRunning(tabId));
            dispatch(httpClientActions.catchErrorSaveToCollection(type === "History" ? false : showSaveModal ? true : false, "Error: Invalid JSON body"));
            throw new Error("Error");
        }        
    }
    
    updateEachRequest(req, data, collectionId, recordingId) {
        req.requestId = data.newReqId;
        req.collectionIdAddedFromClient = collectionId;
        req.traceIdAddedFromClient = data.newTraceId;
        req.recordingIdAddedFromClient = recordingId;
        req.eventData[0].reqId = data.newReqId;
        req.eventData[0].traceId = data.newTraceId;
        req.eventData[0].collection = collectionId;
        req.eventData[1].reqId = data.newReqId;
        req.eventData[1].traceId = data.newTraceId;
        req.eventData[1].collection = data.collec;
    }

    updateTabWithNewData(tabId, response, recordingId) {
        const {httpClient: {tabs, selectedTabKey, showSaveModal, userCollections}} = this.props;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const tabToProcess = tabs[tabIndex];
        if(response.status === "success") {
            try {
                const parsedData = response.data.response && response.data.response.length > 0 ?  response.data.response.map((eachOne) => {
                    return JSON.parse(eachOne);
                }) : [];
                const collection = userCollections.find(eachCollection => eachCollection.id === recordingId);
                for(let eachReq of parsedData) {
                    if(eachReq.oldReqId === tabToProcess.requestId) {
                        this.updateEachRequest(tabToProcess, eachReq, collection.collec, collection.id);
                    } else {
                        tabToProcess.outgoingRequests.map((eachOutgoingReq) => {
                            if(eachReq.oldReqId === eachOutgoingReq.requestId) {
                                this.updateEachRequest(eachOutgoingReq, eachReq, collection.collec, collection.id);
                            }
                            return eachOutgoingReq;
                        })
                    }
                }
            } catch(err) {
                console.error("Error ", error);
            }
        }
    }

    handleChange(evt) {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setUpdatedModalUserCollectionDetails(evt.target.name, evt.target.value));
    }

    handleCreateCollection() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {httpClient: {collectionName, collectionLabel}} = this.props;
        const {dispatch} = this.props;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const userId = user.username,
            customerId = user.customer_name;
        const searchParams = new URLSearchParams();

        searchParams.set('name', collectionName);
        searchParams.set('userId', userId);
        searchParams.set('label', collectionLabel);
        searchParams.set('recordingType', "UserGolden");

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        };

        try {
            api.post(`${config.apiBaseUrl}/cs/start/${user.customer_name}/${app}/dev/Default${app}`, searchParams, configForHTTP)
                .then((serverRes) => {
                    this.loadUserCollections();
                    dispatch(httpClientActions.postSuccessCreateCollection(true, "Created Successfully! Please select this newly created collection from below dropdown and click save."));
                }, (error) => {
                    dispatch(httpClientActions.postErrorCreateCollection(true, "Error saving: " + error));
                    console.error("error: ", error);
                })
        } catch(error) {
            dispatch(httpClientActions.catchErrorCreateCollection(true, "Error saving: " + error));
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    handleSave() {
        const {httpClient: {userCollectionId, userCollections, selectedSaveableTabId, tabs, selectedTabKey}} = this.props;
        let isOutgoingRequest = false;
        let tabsToProcess = tabs;
        let tabIndex = this.getTabIndexGivenTabId(selectedSaveableTabId, tabsToProcess);
        if (tabIndex < 0) {
            const indexToFind = tabs.findIndex(tab => tab.id === selectedTabKey);
            tabsToProcess = tabs[indexToFind]["outgoingRequests"];
            tabIndex = this.getTabIndexGivenTabId(selectedSaveableTabId, tabsToProcess);
            if (tabIndex > -1) {
                isOutgoingRequest = true;
            }
        }
        const selectedCollection = userCollections.find((eachCollection) => {
            return eachCollection.id === userCollectionId;
        });
        tabs[tabIndex].abortRequest = null;
        this.saveToCollection(isOutgoingRequest, selectedSaveableTabId, selectedCollection.id, "UserGolden");
    }

    loadUserCollections() {
        const user = JSON.parse(localStorage.getItem('user'));
        const { cube: {selectedApp} } = this.props;
        let app = selectedApp;
        if(!app) {
            const parsedUrlObj = urlParser(window.location.href, true);
            app = parsedUrlObj.query.app;
        }
        const { dispatch } = this.props;
        const userId = encodeURIComponent(user.username),
            customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=UserGolden&archived=false`)
                .then((serverRes) => {
                    const userCollections = serverRes.filter((eachCollection) => {
                        return eachCollection.recordingType !== "History"
                    });
                    dispatch(httpClientActions.addUserCollections(userCollections));
                }, (error) => {
                    console.error("error: ", error);
                })
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    loadFromHistory() {
        const user = JSON.parse(localStorage.getItem('user'));
        const { dispatch } = this.props;
        const { cube: {selectedApp} } = this.props;
        let app = selectedApp;
        if(!app) {
            const parsedUrlObj = urlParser(window.location.href, true);
            app = parsedUrlObj.query.app;
        }
        const userId = encodeURIComponent(user.username),
            customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=History&archived=false`)
                .then((serverRes) => {
                    const {httpClient: {userHistoryCollection}} = this.props;
                    const fetchedUserHistoryCollection = serverRes.find((eachCollection) => {
                        return eachCollection.recordingType === "History";
                    });
                    if(!userHistoryCollection && fetchedUserHistoryCollection) {
                        dispatch(httpClientActions.addUserHistoryCollection(fetchedUserHistoryCollection));
                    }
                    const startTime = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
                    api.get(`${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${fetchedUserHistoryCollection.collec}&startDate=${startTime}`)
                        .then((res) => {
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
                        }, (err) => {
                            console.error("err: ", err);
                        })
                }, (error) => {
                    console.error("error: ", error);
                })
        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    formatHttpEventToReqResObject(reqId, httpEventReqResPair) {
        const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
        const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
        let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
        for (let eachHeader in httpRequestEvent.payload[1].hdrs) {
            headers.push({
                id: uuidv4(),
                name: eachHeader,
                value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                description: "",
                selected: true,
            });
        }
        for (let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
            queryParams.push({
                id: uuidv4(),
                name: eachQueryParam,
                value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                description: "",
                selected: true,
            });
        }
        for (let eachFormParam in httpRequestEvent.payload[1].formParams) {
            formData.push({
                id: uuidv4(),
                name: eachFormParam,
                value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                description: "",
                selected: true,
            });
            rawDataType = "";
        }
        if (httpRequestEvent.payload[1].body) {
            if (!_.isString(httpRequestEvent.payload[1].body)) {
                try {
                    rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                    rawDataType = "json";
                } catch (err) {
                    console.error(err);
                }
            } else {
                rawData = httpRequestEvent.payload[1].body;
                rawDataType = "text";
            }
        }
        let reqObject = {
            id: uuidv4(),
            httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
            httpURL: "{{{url}}}/" + httpRequestEvent.apiPath,
            httpURLShowOnly: httpRequestEvent.apiPath,
            headers: headers,
            queryStringParams: queryParams,
            bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
            formData: formData,
            rawData: rawData,
            rawDataType: rawDataType,
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
            isOutgoingRequest: false,
            service: httpRequestEvent.service,
            recordingIdAddedFromClient: "",
            collectionIdAddedFromClient: httpRequestEvent.collection,
            traceIdAddedFromClient: httpRequestEvent.traceId,
            apiPath: httpRequestEvent.apiPath,
            requestRunning: false,
            showTrace: null,
        };
        return reqObject;
    }

    loadSavedTrace(tabId, traceId, reqId, runId, apiPath, apiConfig) {
        const {httpClient: {tabs, userHistoryCollection}} = this.props;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        if(!app) {
            const parsedUrlObj = urlParser(window.location.href, true);
            app = parsedUrlObj.query.app;
        }
        const { dispatch } = this.props;
        const user = JSON.parse(localStorage.getItem('user'));
        const historyCollectionId = userHistoryCollection.collec;
        const apiTraceUrl = `${config.apiBaseUrl}/as/getApiTrace/${user.customer_name}/${app}?depth=100&collection=${historyCollectionId}&apiPath=${apiPath}&runId=${runId}`;
        api.get(apiTraceUrl, apiConfig)
            .then((res) => {
                res.response.sort((a, b) => {
                    return b.res.length - a.res.length;
                });
                const apiTrace = res.response[0];
                const reqIdArray = [];
                apiTrace && apiTrace.res.map((eachApiTraceEvent) => {
                    reqIdArray.push(eachApiTraceEvent.requestEventId);
                });

                if (reqIdArray && reqIdArray.length > 0) {
                    const eventTypes = [];
                    cubeService.fetchAPIEventData(app, reqIdArray, eventTypes, apiConfig).then((result) => {
                        if(result && result.numResults > 0) {
                            const ingressReqResPair = result.objects.filter(eachReq => eachReq.apiPath === apiPath);
                            let ingressReqObj;
                            if (ingressReqResPair.length > 0) {
                                ingressReqObj = this.formatHttpEventToReqResObject(reqId, ingressReqResPair);
                            }
                            for (let eachReqId of reqIdArray) {
                                const reqResPair = result.objects.filter(eachReq => {
                                    return (eachReq.reqId === eachReqId && eachReq.apiPath !== apiPath);
                                });
                                if (reqResPair.length > 0 && eachReqId !== reqId) {
                                    let reqObject = this.formatHttpEventToReqResObject(eachReqId, reqResPair);
                                    ingressReqObj.outgoingRequests.push(reqObject);
                                }
                            }
                            dispatch(httpClientActions.postSuccessLoadRecordedHistory(tabId, ingressReqObj));
                            if(reqIdArray.length > 1) {
                                tabs.map((eachTab) => {
                                    if(eachTab.clearIntervalHandle) {
                                        clearInterval(eachTab.clearIntervalHandle);
                                    }
                                });
                            }
                            dispatch(httpClientActions.unsetReqRunning(tabId))
                        }
                    });
                }
            }, (err) => {
                console.error("err: ", err);
                dispatch(httpClientActions.unsetReqRunning(tabId));
            })
    }

    handlePanelClick(selectedCollectionId, forceLoad) {
        if (!selectedCollectionId) return;
        const user = JSON.parse(localStorage.getItem('user'));
        const {httpClient: {userCollections}} = this.props;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const {dispatch} = this.props;
        const customerId = encodeURIComponent(user.customer_name);
        const selectedCollection = userCollections.find(eachCollection => eachCollection.collec === selectedCollectionId);
        const apiTracesForACollection = selectedCollection.apiTraces;
        try {
            if (!apiTracesForACollection || forceLoad) {
                api.get(`${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${selectedCollectionId}`)
                    .then((res) => {
                        const apiTraces = [];
                        res.response.sort((a, b) => {
                            return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
                        });
                        res.response.map(eachApiTrace => {
                            eachApiTrace.res.map((eachApiTraceEvent) => {
                                eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                                eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                                eachApiTraceEvent["toggled"] = false;
                                eachApiTraceEvent["recordingIdAddedFromClient"] = selectedCollection.id;
                                eachApiTraceEvent["traceIdAddedFromClient"] = eachApiTrace.traceId;
                                eachApiTraceEvent["collectionIdAddedFromClient"] = eachApiTrace.collection;
                            });
                            const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                                customID: "spanId", parentProperty: "parentSpanId"
                            });
                            apiTraces.push({
                                ...apiFlatArrayToTree[0]
                            })
                        });

                    selectedCollection.apiTraces = apiTraces;
                    dispatch(httpClientActions.addUserCollections(userCollections));
                }, (err) => {
                    console.error("err: ", err);
                });
            }

        } catch (error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    addTab(evt, reqObject, givenApp) {
        const { dispatch } = this.props;
        const tabId = uuidv4();
        const requestId = uuidv4();
        const { app } = this.state;
        const appAvailable = givenApp ? givenApp : app ? app : "";
        if (!reqObject) {
            const traceId = cryptoRandomString({length: 32});
            const { cube: {selectedApp} } = this.props;
            const user = JSON.parse(localStorage.getItem('user'));
            const customerId = user.customer_name;
            const eventData = this.generateEventdata(selectedApp, customerId, traceId);
            reqObject = {
                httpMethod: "get",
                httpURL: "",
                httpURLShowOnly: "",
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
                recordedHistory: null,
                clearIntervalHandle: null
            };
        }
        dispatch(httpClientActions.addTab(tabId, reqObject, appAvailable, tabId, reqObject.httpURL ? reqObject.httpURL : "New"));
        return tabId;
    }

    getRequestIds() {
        const { apiCatalog: { httpClientRequestIds } } = this.props;
        return httpClientRequestIds;
    }

    formatHttpEventToTabObject(reqId, requestIdsObj, httpEventReqResPair) {
        const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
        const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
        let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
        for (let eachHeader in httpRequestEvent.payload[1].hdrs) {
            headers.push({
                id: uuidv4(),
                name: eachHeader,
                value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                description: "",
                selected: true,
            });
        }
        for (let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
            queryParams.push({
                id: uuidv4(),
                name: eachQueryParam,
                value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                description: "",
                selected: true,
            });
        }
        for (let eachFormParam in httpRequestEvent.payload[1].formParams) {
            formData.push({
                id: uuidv4(),
                name: eachFormParam,
                value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                description: "",
                selected: true,
            });
            rawDataType = "";
        }
        if (httpRequestEvent.payload[1].body) {
            if (!_.isString(httpRequestEvent.payload[1].body)) {
                try {
                    rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                    rawDataType = "json";
                } catch (err) {
                    console.error(err);
                }
            } else {
                rawData = httpRequestEvent.payload[1].body;
                rawDataType = "text";
            }
        }
        let reqObject = {
            httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
            httpURL: "{{{url}}}/" + httpRequestEvent.apiPath,
            httpURLShowOnly: httpRequestEvent.apiPath,
            headers: headers,
            queryStringParams: queryParams,
            bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
            formData: formData,
            rawData: rawData,
            rawDataType: rawDataType,
            responseStatus: "NA",
            responseStatusText: "",
            responseHeaders: "",
            responseBody: "",
            recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
            recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
            recordedResponseStatus: httpResponseEvent ? httpResponseEvent.payload[1].status : "",
            responseBodyType: "json",
            requestId: reqId,
            outgoingRequestIds: requestIdsObj[reqId] ? requestIdsObj[reqId] : [],
            eventData: httpEventReqResPair,
            showOutgoingRequestsBtn: requestIdsObj[reqId] && requestIdsObj[reqId].length > 0,
            showSaveBtn: true,
            outgoingRequests: [],
            showCompleteDiff: false,
            isOutgoingRequest: false,
            service: httpRequestEvent.service,
            recordingIdAddedFromClient: "",
            collectionIdAddedFromClient: httpRequestEvent.collection,
            traceIdAddedFromClient: httpRequestEvent.traceId,
            requestRunning: false,
            showTrace: null,
        };
        return reqObject;
    }

    updateDimensions = () => {
        this.setState({ showEnvPopoverOverlay: false});
    };

    componentDidMount() {
        const { dispatch } = this.props;
        const { cube: {selectedApp} } = this.props;
        const {httpClient: {tabs}} = this.props;
        window.addEventListener('resize', this.updateDimensions);
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
        this.loadFromHistory();
        this.loadUserCollections();
        
        const requestIds = this.getRequestIds(), reqIdArray = Object.keys(requestIds);
        tabs.map(eachTab => {
            const indx = reqIdArray.findIndex((eachReq) => eachReq === eachTab.requestId);
            if(indx > -1) reqIdArray.splice(indx, 1);
            return eachTab; 
        });
        if (reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(selectedApp, reqIdArray, eventTypes).then((result) => {
                if (result && result.numResults > 0) {
                    for (let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if (reqResPair.length > 0) {
                            let reqObject = this.formatHttpEventToTabObject(eachReqId, requestIds, reqResPair);
                            const savedTabId = this.addTab(null, reqObject, selectedApp);
                            this.showOutgoingRequests(savedTabId, reqObject.traceIdAddedFromClient, reqObject.collectionIdAddedFromClient, reqObject.recordingIdAddedFromClient);
                        }
                    }
                }
            });
        } else {
            let app = selectedApp;
            if(!app) {
                const parsedUrlObj = urlParser(window.location.href, true);
                app = parsedUrlObj.query.app;
            }
            if(tabs.length === 0)this.addTab(null, null, app);
        }

        dispatch(httpClientActions.fetchEnvironments())
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        const {httpClient: {tabs}} = this.props;
        window.removeEventListener('resize', this.updateDimensions);
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
        tabs.map((eachTab) => {
            if (eachTab.clearIntervalHandle) {
                clearInterval(eachTab.clearIntervalHandle);
            }
        });
    }

    handleTreeNodeClick(node) {
        this.openTab(node);
    }

    openTab(node) {
        const { cube: {selectedApp} } = this.props;
        const reqIdArray = [node.requestEventId];
        if(reqIdArray && reqIdArray.length > 0) {
            const user = JSON.parse(localStorage.getItem('user'));
            const apiEventURL = `${config.recordBaseUrl}/getEvents`;
            let body = {
                "customerId": user.customer_name,
                "app": selectedApp,
                "eventTypes": [],
                "services": [node.service],
                "traceIds": [node.traceIdAddedFromClient],
                "reqIds": reqIdArray,
                "paths": [node.apiPath],
                "collection": node.collectionIdAddedFromClient
            }
            api.post(apiEventURL, body).then((result) => {
                if (result && result.numResults > 0) {
                    for (let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if (reqResPair.length === 1) {
                            reqResPair.push(result.objects.find(eachReq => eachReq.eventType === "HTTPResponse"));
                        }
                        if (reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
                            for (let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: "",
                                    selected: true,
                                });
                            }
                            for (let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                                    description: "",
                                    selected: true,
                                });
                            }
                            for (let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: "",
                                    selected: true,
                                });
                                rawDataType = "";
                            }
                            if (httpRequestEvent.payload[1].body) {
                                if (!_.isString(httpRequestEvent.payload[1].body)) {
                                    try {
                                        rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                                        rawDataType = "json";
                                    } catch (err) {
                                        console.error(err);
                                    }
                                } else {
                                    rawData = httpRequestEvent.payload[1].body;
                                    rawDataType = "text";
                                }
                            }

                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: "{{{url}}}/" + httpRequestEvent.apiPath,
                                httpURLShowOnly: httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
                                formData: formData,
                                rawData: rawData,
                                rawDataType: rawDataType,
                                responseStatus: "NA",
                                responseStatusText: "",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
                                recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                recordedResponseStatus: httpResponseEvent ? httpResponseEvent.payload[1].status : "",
                                responseBodyType: "json",
                                requestId: httpRequestEvent.reqId,
                                outgoingRequestIds: node.children ? node.children.map(eachChild => eachChild.requestEventId) : [],
                                eventData: reqResPair,
                                showOutgoingRequestsBtn: node.children && node.children.length > 0,
                                showSaveBtn: true,
                                recordingIdAddedFromClient: node.recordingIdAddedFromClient,
                                collectionIdAddedFromClient: node.collectionIdAddedFromClient,
                                traceIdAddedFromClient: node.traceIdAddedFromClient,
                                outgoingRequests: [],
                                showCompleteDiff: false,
                                isOutgoingRequest: false,
                                service: httpRequestEvent.service,
                                requestRunning: false,
                                showTrace: null,
                            };
                            const savedTabId = this.addTab(null, reqObject, selectedApp);
                            this.showOutgoingRequests(savedTabId, node.traceIdAddedFromClient, node.collectionIdAddedFromClient, node.recordingIdAddedFromClient);
                        }
                    }
                }
            });
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
                        <HttpClient currentSelectedTab={eachTab}
                        
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
                        showSaveModal={this.showSaveModal}
                        handleRowClick={this.handleRowClick}
                        handleTestRowClick={this.handleTestRowClick}
                        setAsReference={this.setAsReference}
                        cubeRunHistory={cubeRunHistory}
                        showAddMockReqModal={this.showAddMockReqModal}
                        handleDuplicateTab={this.handleDuplicateTab}
                        toggleShowTrace={this.toggleShowTrace}
                        >
                        </HttpClient>
                    </div>
                )
            },
            /* Optional parameters */
            key: eachTab.id,
            tabClassName: 'md-hc-tab',
            panelClassName: 'md-hc-tab-panel'
        }));
    }

    renderTreeNodeHeader(props) {
        const isParent = props.node.isCubeRunHistory ? !!(props.node.children && props.node.children.length> 0) : (props.node.parentSpanId == "NA") ;
        return (
            <div style={props.style.base} className="treeNodeItem">
                <div style={props.style.title}>
                    <div style={{ paddingLeft: "9px", backgroundColor: "", display: "flex" }}>
                        <div style={{ flexDirection: "column", width: "36px", verticalAlign: "top", }}>
                            <Label bsStyle="default" style={{ fontWeight: "600", fontSize: "9px" }}>{props.node.method}</Label>
                        </div>
                        <div style={{ flex: "1", wordBreak: "break-word", verticalAlign: "top", fontSize: "12px" }} onClick={() => this.handleTreeNodeClick(props.node)}>
                            <span style={{ paddingLeft: "5px", marginLeft: "5px", borderLeft: "2px solid #fc6c0a", whiteSpace : 'nowrap',
    textOverflow: 'ellipsis',
    overflow: 'hidden' }} >{props.node.name + " " + moment(props.node.reqTimestamp * 1000).format("hh:mm:ss")}</span>
                        </div>
                        <div className="collection-options"><i className="fas fa-trash pointer" 
                            data-id={isParent? props.node.traceIdAddedFromClient : props.node.requestEventId} 
                            data-isparent = {isParent}
                            data-name={props.node.name}  title="Delete"
                            data-collection-id={props.node.collectionIdAddedFromClient} 
                            data-cubehistory={props.node.isCubeRunHistory === true}
                            data-type="request" onClick={this.onDeleteBtnClick}/>
                            </div>
                    </div>
                </div>
            </div>
        );
    }

    renderTreeNodeContainer(props) {
        return (
            <TreeNodeContainer {...props} />
        );
    }

    renderTreeNodeToggle(props) {
        return (
            <TreeNodeToggle {...props} />
        );
    }

    getCurrentEnvirnoment = () => {
        const { httpClient: { environmentList, selectedEnvironment } } = this.props;
        return _.find(environmentList, { name: selectedEnvironment })
    }

    renderEnvListDD = () => {
        const { httpClient: { environmentList, selectedEnvironment } } = this.props;
        return (
            <FormGroup bsSize="small" style={{ marginBottom: "0px" }}>
                <FormControl componentClass="select" placeholder="Environment" style={{ fontSize: "12px" }} value={selectedEnvironment} onChange={this.handleEnvChange} className="btn-sm">
                    <option value="">No Environment</option>
                    {environmentList.map((env) => (<option key={env.name} value={env.name}>{env.name}</option>))}
                </FormControl>
            </FormGroup>)
    }

    handleEnvChange = (e) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.setSelectedEnvironment(e.target.value))
    }

    renderSelectedEnvModal = () => {
        const currentEnvironment = this.getCurrentEnvirnoment();
        const { httpClient: { selectedEnvironment } } = this.props;
        return (
            <Fragment>
                <span title="Environment quick look" className="btn btn-sm cube-btn text-center" onClick={this.openSelectedEnvModal}>
                    <i className="fas fa-eye" />
                </span>
                <Modal show={this.state.showSelectedEnvModal} onHide={this.closeSelectedEnvModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>{selectedEnvironment || "No Environment Selected"}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div>
                            {currentEnvironment && !_.isEmpty(currentEnvironment.vars) && <table className="table table-bordered table-hover">
                                <thead>
                                    <tr>
                                        <th style={{ width: "20%" }}>Variable</th>
                                        <th>Value</th>
                                    </tr>
                                </thead>
                                <tbody>
                                {currentEnvironment.vars.map((varEntry) => (
                                    <tr>
                                        <td>{varEntry.key}</td>
                                        <td style={{wordBreak: "break-all"}}>{varEntry.value}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>}
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <div onClick={this.closeSelectedEnvModal} className="btn btn-sm cube-btn text-center">Close</div>
                    </Modal.Footer>
                </Modal>
            </Fragment>
        )
    }

    openSelectedEnvModal = () => {
        this.setState({showSelectedEnvModal: true});
    }

    closeSelectedEnvModal = () => {
        this.setState({showSelectedEnvModal: false})
    }

    hideEnvModal = () => {
        this.setState({ showEnvVarModal: false })
    }
     
    handleDuplicateTab = (tabId) => {
        const {dispatch} = this.props;
        dispatch(httpClientActions.createDuplicateTab(tabId))
    }

    deleteItem = async ()  => {
        const {  dispatch, httpClient:{userCollections}} = this.props;
        const {itemToDelete} = this.state;
        try {
            if(itemToDelete.requestType == "collection"){
                await cubeService.deleteGolden(itemToDelete.id);
                dispatch(httpClientActions.deleteUserCollection(itemToDelete.id));
            }else if(itemToDelete.requestType ==  "request"){
                if(itemToDelete.isParent){
                    await cubeService.deleteEventByTraceId(itemToDelete.id, itemToDelete.collectionId);
                }else{
                    await cubeService.deleteEventByRequestId(itemToDelete.id);
                }
                if(itemToDelete.isCubeHistory){
                    dispatch(httpClientActions.deleteCubeRunHistory(itemToDelete.id))
                }else{
                    this.handlePanelClick(itemToDelete.collectionId, true);                
                }
            }
        } catch (error) {
            console.error("Error caught in softDelete Golden: " + error);
        }
        this.setState({
            showDeleteGoldenConfirmation: false
        });
    }

    onDeleteBtnClick = (event) => {
        event.stopPropagation();
        const requestType = event.target.getAttribute('data-type');
        const id = event.target.getAttribute('data-id');
        const name = event.target.getAttribute('data-name');       
        const collectionId = event.target.getAttribute('data-collection-id');
        const isParent = event.target.getAttribute('data-isparent') == 'true';
        const isCubeHistory = event.target.getAttribute('data-cubehistory') == 'true';
        
        this.setState({showDeleteGoldenConfirmation: true, 
            itemToDelete: {
                requestType, id, name, collectionId, isParent, isCubeHistory
            }});
    };

    showErrorAlert = (message) => {
        this.setState({ showErrorModal: true, errorMsg: message});
    };

    onCloseErrorModal = () => {
        this.setState({ showErrorModal: false});
    };

    getExpendedState = (uniqueid)=>{
        const isExpanded = this.persistPanelState[uniqueid];
        if(isExpanded){
            this.handlePanelClick(uniqueid);
        }
        return isExpanded;
    }

    onPanelToggle = (isToggled, event)=> {
        this.persistPanelState[event.target.parentElement.getAttribute('data-unique-id')] = isToggled;
    }

    render() {
        const { cube } = this.props;
        const { showEnvVarModal, showDeleteGoldenConfirmation, showErrorModal, errorMsg, importedToCollectionId, serializedCollection, modalErrorImportCollectionMessage, showImportModal, curlCommand, modalErrorImportFromCurlMessage} = this.state;
        const { cube: {selectedApp} } = this.props;
        const app = selectedApp;
        const {httpClient: {cubeRunHistory, userCollections, collectionName, collectionLabel, modalErroSaveMessage,modalErroSaveMessageIsError, modalErroCreateCollectionMessage, tabs, selectedTabKey, showSaveModal, showAddMockReqModal, mockRequestServiceName, mockRequestApiPath, modalErrorAddMockReqMessage}} = this.props;

        return (

            <div className="http-client" style={{ display: "flex", height: "100%" }}>
                <aside className="" ref={e=> (this.sliderRef = e)}
                style={{ "width": "250px", "height": "100%", "background": "#EAEAEA", "padding": "10px", "display": "flex", "flexDirection": "column", overflow: "auto" }}>
                    <div style={{ marginTop: "10px", marginBottom: "10px" }}>
                        <div className="label-n">APPLICATION</div>
                        <div className="application-name">{app}</div>
                    </div>
                    <Tabs defaultActiveKey={1} id="uncontrolled-tab-example">
                        <Tab eventKey={1} title="History">
                            <div className="margin-top-10">
                                <div className="value-n"></div>
                            </div>
                            <div className="margin-top-10">
                                {Object.keys(cubeRunHistory).map((k, i) => {
                                    return (
                                        <Panel key={k + "_" + i} id="collapsible-panel-example-2" defaultExpanded>
                                            <Panel.Heading style={{ paddingLeft: "9px" }}>
                                                <Panel.Title toggle style={{ fontSize: "13px" }}>
                                                    {k}
                                                </Panel.Title>
                                            </Panel.Heading>
                                            <Panel.Collapse>
                                                <Panel.Body style={{ padding: "3px" }}>
                                                    {cubeRunHistory[k].map(eachTabRun => {
                                                        eachTabRun.isCubeRunHistory = true;
                                                        /* return (
                                                            <div key={eachTabRun.reqTimestamp} style={{padding: "5px", backgroundColor: ""}}>
                                                                <div style={{display: "inline-block", width: "21%"}}>
                                                                    <Label bsStyle="default" style={{fontWeight: "600"}}>{eachTabRun.method.toUpperCase()}</Label>
                                                                </div>
                                                                <div style={{paddingLeft: "5px", display: "inline-block", wordBreak: "break-word", width: "78%", verticalAlign: "middle", fontSize: "12px", color: "#9CA5AB" , cursor: "pointer"}}>
                                                                    {eachTabRun.apiPath}
                                                                </div>
                                                            </div>
                                                        ); */
                                                        return (
                                                            <Treebeard key={eachTabRun.id}
                                                                data={eachTabRun}
                                                                style={CollectionTreeCSS}
                                                                onToggle={this.onToggle}
                                                                decorators={{ ...decorators, Header: this.renderTreeNodeHeader, Container: this.renderTreeNodeContainer, Toggle: this.renderTreeNodeToggle }}
                                                            />
                                                        );
                                                    })}
                                                </Panel.Body>
                                            </Panel.Collapse>
                                        </Panel>
                                    )
                                })}
                            </div>
                        </Tab>
                        <Tab eventKey={2} title="Collections">
                            <div className="margin-top-10">
                                <div className="value-n"></div>
                            </div>
                            <div className="margin-top-10">
                                {userCollections && userCollections.map(eachCollec => {
                                    return (
                                        <Panel id="collapsible-panel-example-2" className="collection-panel-div" key={eachCollec.collec} value={eachCollec.collec} onClick={() => this.handlePanelClick(eachCollec.collec)} 
                                            defaultExpanded = {this.getExpendedState(eachCollec.collec)}
                                            onToggle={this.onPanelToggle}>
                                            <Panel.Heading style={{ paddingLeft: "9px", position: 'relative' }}>
                                                <Panel.Title toggle style={{ fontSize: "13px" }} data-unique-id={eachCollec.collec}>
                                                    {eachCollec.name}
                                                </Panel.Title>
                                                <div className="collection-options"><i className="fas fa-trash pointer" data-id={eachCollec.rootRcrdngId} 
                                                data-name={eachCollec.name} title="Delete"
                                                data-type="collection" onClick={this.onDeleteBtnClick}/></div>
                                            </Panel.Heading>
                                            <Panel.Collapse>
                                                <Panel.Body style={{ padding: "3px", width: "100%", overflow: "auto" }}>
                                                    {eachCollec.apiTraces && eachCollec.apiTraces.map((eachApiTrace) => {
                                                        if(this.persistPanelState[eachApiTrace.requestEventId]){
                                                            eachApiTrace.toggled = true;
                                                        }
                                                        return (
                                                            <Treebeard key={eachApiTrace.id}
                                                                data={eachApiTrace}
                                                                style={CollectionTreeCSS}
                                                                onToggle={this.onToggle}
                                                                decorators={{ ...decorators, Header: this.renderTreeNodeHeader, Container: this.renderTreeNodeContainer, Toggle: this.renderTreeNodeToggle }}
                                                            />
                                                        );
                                                    })}

                                                </Panel.Body>
                                            </Panel.Collapse>
                                        </Panel>
                                    );
                                })}
                            </div>
                        </Tab>
                    </Tabs>
                </aside>
               <SplitSlider slidingElement={this.sliderRef}/>
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
                                <div style={{display: "inline-block", padding: 0}} className="btn">{this.renderEnvListDD()}</div>
                                <div style={{display: "inline-block"}}>{this.renderSelectedEnvModal()}</div>
                                <span className="btn btn-sm cube-btn text-center" onClick={() => {this.setState({showEnvVarModal: true})}} title="Configure environments"><i className="fas fa-cog"/> </span>
                            {/* <div style={{display: "inline-block", margin: "10px" }}>
                            </div> */}
                        </div>
                    </div>
                    <div style={{marginTop: "10px", display: ""}}>
                        <ResponsiveTabs 
                            showMore={true} 
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
                        <Modal show={showSaveModal} onHide={this.handleCloseModal}>
                            <Modal.Header closeButton>
                                <Modal.Title>Save to collection</Modal.Title>
                            </Modal.Header>
                            <Modal.Body>
                                <h5 style={{ textAlign: 'center' }}>
                                    Create a new collection
                                </h5>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Name</ControlLabel>
                                        <FormControl componentClass="input" placeholder="Name" name="collectionName" value={collectionName} onChange={this.handleChange} />
                                    </FormGroup>
                                </div>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Label</ControlLabel>
                                        <FormControl componentClass="input" placeholder="Label" name="collectionLabel" value={collectionLabel} onChange={this.handleChange} />
                                    </FormGroup>
                                </div>
                                <p style={{ fontWeight: 500 }}>{modalErroCreateCollectionMessage}</p>
                                <div>
                                    <div onClick={this.handleCreateCollection} className="btn btn-sm cube-btn text-center">Create</div>
                                </div>
                                <hr />
                                <h5 style={{ textAlign: 'center' }}>
                                    Select an exisiting collection
                                </h5>
                                <div>
                                    <FormGroup style={{ marginBottom: "0px" }}>
                                        <FormControl componentClass="select" placeholder="Select" name="userCollectionId" value={this.userCollectionId} onChange={this.handleChange}>
                                            <option value=""></option>
                                            {userCollections && userCollections.map((eachUserCollection) => {
                                                return (
                                                    <option key={eachUserCollection.id} value={eachUserCollection.id}>{eachUserCollection.name}</option>
                                                );
                                            })}
                                        </FormControl>
                                    </FormGroup>
                                </div>
                                <p style={{ marginTop: "10px", fontWeight: 500,color: modalErroSaveMessageIsError ? "red" : "" }} >
                                    {modalErroSaveMessage}
                                </p>
                            </Modal.Body>
                            <Modal.Footer>
                                <div onClick={this.handleSave} className="btn btn-sm cube-btn text-center">Save</div>
                                <div onClick={this.handleCloseModal} className="btn btn-sm cube-btn text-center">Close</div>
                            </Modal.Footer>
                        </Modal>
                        <Modal bsSize="large" show={showEnvVarModal} onHide={this.hideEnvModal} className="envModal">
                            <EnvVar hideModal={this.hideEnvModal} />
                        </Modal>
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
                        <Modal show={showDeleteGoldenConfirmation}>
                            <Modal.Body>
                                <div style={{ display: "flex", flex: 1, justifyContent: "center"}}>
                                    <div className="margin-right-10" style={{ display: "flex", flexDirection: "column", fontSize:20 }}>
                                        This will delete the {this.state.itemToDelete.name}. Please confirm.
                                    </div>
                                    <div style={{ display: "flex", alignItems: "flex-start" }}>
                                            <span className="cube-btn margin-right-10" onClick={() => this.deleteItem()}>Confirm</span>
                                            <span className="cube-btn" onClick={() => (this.setState({
                                                showDeleteGoldenConfirmation: false,
                                                itemToDelete:{}
                                            }))}>No</span>
                                    </div>
                                </div>
                            </Modal.Body>
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

function mapStateToProps(state) {
    const {cube, apiCatalog, httpClient} = state;
    return {
        cube,
        apiCatalog,
        httpClient
    }
}

const connectedHttpClientTabs = connect(mapStateToProps)(HttpClientTabs);

export default connectedHttpClientTabs
