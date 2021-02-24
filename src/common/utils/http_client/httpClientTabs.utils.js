/*
    - This file contains functions moved from httpClientTabs Component
    - to de-clutter existing file
    - only standalone functions are moved
    - More functions can be decoupled from existing code and move out here
*/

import { 
    generateRunId,
    generateApiPath, 
    extractParamsFromRequestEvent, 
    selectedRequestParamData, 
    unSelectedRequestParamData, 
    generateTraceKeys, 
    getTracerForCurrentApp, 
    extractQueryStringParamsToCubeFormat,
    extractBodyToCubeFormat, 
    extractHeadersToCubeFormat, 
    multipartDataToCubeFormat,
    generateSpanId,
    tryJsonParse
} from "../../utils/http_client/utils.js";
import urlParser from 'url-parse';
import { v4 as uuidv4 } from 'uuid';
import _ from 'lodash';

import { getRenderEnvVars,applyEnvVarsToUrl } from "../../utils/http_client/envvar";

import { applyGrpcDataToRequestObject, extractGrpcBody,
    getRequestUrlFromSchema, getConnectionSchemaFromMetadataOrApiPath } from "../../utils/http_client/grpc-utils"; 

export function createRecordedDataForEachRequest(toBeUpdatedData, toBeCopiedFromData) {
    let referenceEventData = toBeCopiedFromData ? toBeCopiedFromData.eventData : null;
    let eventData = toBeUpdatedData.eventData;
    const tracer = getTracerForCurrentApp()
    const spanId = generateSpanId(tracer)
    if(referenceEventData && referenceEventData.length > 0) {
        let refHttpRequestEventTypeIndex = referenceEventData[0].eventType === "HTTPRequest" ? 0 : 1;
        let refHttpResponseEventTypeIndex = refHttpRequestEventTypeIndex === 0 ? 1 : 0;
        let refHttpResponseEvent = referenceEventData[refHttpResponseEventTypeIndex];
        let refHttpRequestEvent = referenceEventData[refHttpRequestEventTypeIndex];
        let refRequestEventData = eventData[refHttpRequestEventTypeIndex];
        let refResponseEventData = eventData[refHttpResponseEventTypeIndex];

        let httpRequestEventTypeIndex = eventData[0].eventType === "HTTPRequest" ? 0 : 1;

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
            spanId: spanId,
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
            requestId: toBeCopiedFromData.requestId,
            httpMethod: toBeCopiedFromData.httpMethod,
            httpURL: toBeCopiedFromData.httpURL,
            httpURLShowOnly: toBeCopiedFromData.httpURLShowOnly,
            headers: toBeCopiedFromData.headers,
            queryStringParams: toBeCopiedFromData.queryStringParams,
            bodyType: toBeCopiedFromData.bodyType,
            formData: toBeCopiedFromData.formData,
            multipartData: toBeCopiedFromData.multipartData,
            rawData: toBeCopiedFromData.rawData,
            rawDataType: toBeCopiedFromData.rawDataType,
            paramsType: toBeCopiedFromData.grpcData ? "showBody" : "showQueryParams",
            responseStatus: toBeCopiedFromData.responseStatus,
            responseStatusText: toBeCopiedFromData.responseStatusText,
            responseHeaders: toBeCopiedFromData.responseHeaders,
            responseBody: toBeCopiedFromData.responseBody,
            responsePayloadState: toBeCopiedFromData.responsePayloadState,
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
            grpcData: toBeCopiedFromData.grpcData,
            grpcConnectionSchema: toBeCopiedFromData.grpcConnectionSchema,
            recordedHistory: [],
            hasChanged: true,
        }
        return tabData;
    }
}

export function copyRecordedDataForEachRequest(toBeUpdatedData, toBeCopiedFromData) {
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
        if(httpResponseEvent && refHttpResponseEvent)
        {
            httpResponseEvent.payload = refHttpResponseEvent.payload;
        }
        httpRequestEvent.payload = refHttpRequestEvent.payload;

        let tabData = {
            id: toBeUpdatedData.id,
            tabName: toBeUpdatedData.tabName,
            requestId: toBeCopiedFromData.requestId,
            httpMethod: toBeCopiedFromData.httpMethod,
            httpURL: toBeCopiedFromData.httpURL,
            httpURLShowOnly: toBeCopiedFromData.httpURLShowOnly,
            headers: toBeCopiedFromData.headers,
            queryStringParams: toBeCopiedFromData.queryStringParams,
            bodyType: toBeCopiedFromData.bodyType,
            formData: toBeCopiedFromData.formData,
            multipartData: toBeCopiedFromData.multipartData,
            rawDataType: toBeCopiedFromData.rawDataType,
            rawData: toBeCopiedFromData.rawData,
            grpcData: toBeCopiedFromData.grpcData,
            paramsType: toBeCopiedFromData.paramsType,
            responseStatus: toBeCopiedFromData.responseStatus,
            responseStatusText: toBeCopiedFromData.responseStatusText,
            recordedResponseStatus: toBeCopiedFromData.recordedResponseStatus,
            responseHeaders: toBeCopiedFromData.responseHeaders,
            responseBody: toBeCopiedFromData.responseBody,
            responsePayloadState: toBeCopiedFromData.responsePayloadState,
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
            grpcConnectionSchema: toBeCopiedFromData.grpcConnectionSchema,
        }
        return tabData;
    }
}

export function findOutgoingRequestIndexGivenApiPath(refReq, tab) {
    let outgoingRequests = tab.outgoingRequests;
    const indexToFind = outgoingRequests.findIndex(eachReq => eachReq.eventData[0].apiPath === refReq.eventData[0].apiPath);
    return indexToFind;
}


export function setAsReferenceForEachRequest(tabToBeProcessed) {
    const recordedHistory = tabToBeProcessed.recordedHistory;
    const copiedTabData = copyRecordedDataForEachRequest(tabToBeProcessed, recordedHistory);
    const copiedTab = {
        ...copiedTabData
    };
    const outgoingRequests = [];
    recordedHistory.outgoingRequests.forEach((eachReq) => {
        const matchedReqIndex = findOutgoingRequestIndexGivenApiPath(eachReq, tabToBeProcessed);
        if(matchedReqIndex > -1) {
            const copiedOutgoingData = copyRecordedDataForEachRequest(tabToBeProcessed.outgoingRequests[matchedReqIndex], eachReq);
            outgoingRequests.push(copiedOutgoingData);
            tabToBeProcessed.outgoingRequests.splice(matchedReqIndex, 1); // Please please please no. Mutation of passed parameters leads to untraceable and confusing bugs
        } else {
            const copiedOutgoingData = createRecordedDataForEachRequest(tabToBeProcessed, eachReq);
            outgoingRequests.push(copiedOutgoingData);
        }
    })
    copiedTab.outgoingRequests = [...tabToBeProcessed.outgoingRequests, ...outgoingRequests];
    copiedTab.selectedTraceTableReqTabId = copiedTab.id;
    copiedTab.selectedTraceTableTestReqTabId = recordedHistory.id;
    copiedTab.requestId = recordedHistory.requestId;
    return copiedTab;
}


export function generateEventdata(app, customerId, traceDetails, service, apiPath, method, requestHeaders, requestQueryParams, requestFormParams, rawData) {
    const timestamp = Date.now() / 1000;
    let path = apiPath ? apiPath.replace(/^\/|\/$/g, '') : "";
    let {traceIdDetails: {traceIdForEvent}, spanId, parentSpanId} = traceDetails;
    let httpResponseEvent = {
        customerId: customerId,
        app: app,
        service: service ? service : "NA",
        instanceId: "devtool",
        collection: "NA",
        traceId: traceIdForEvent,
        spanId: spanId,
        parentSpanId: parentSpanId,
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
        traceId: traceIdForEvent,
        spanId: spanId,
        parentSpanId: parentSpanId,
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


export function flattenCollection(collectionToImport) {
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

export function extractBody(httpRequestBody) {
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

export function extractQueryStringParams(httpRequestQueryStringParams) {
    let qsParams = {};
    httpRequestQueryStringParams
        .filter((param) => param.selected)
        .forEach(each => {
            if(each.name && each.value) qsParams[each.name] = each.value;
        })
    return qsParams;
}

export function extractHeaders(httpReqestHeaders) {
    let headers = {};
    httpReqestHeaders
        .filter((header) => header.selected)
        .forEach(each => {
            if(each.name && each.value && each.name.indexOf(":") < 0) headers[each.name] = each.value;
        })
    return headers;
}


export function isgRPCRequest(tabToProcess){
    return tabToProcess.bodyType === "grpcData" && tabToProcess.eventData[0].payload[0] === "GRPCRequestPayload"
    // tabToProcess.grpcData && tabToProcess.grpcData.trim()
}


export function formatHttpEventToReqResObject(reqId, httpEventReqResPair, isOutgoingRequest, existingId) {
    const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
    const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
    const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
    const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
    const httpResponseEventPayload  = httpResponseEvent && httpResponseEvent.payload ?  httpResponseEvent.payload[1] : {};
    
    const { headers, queryParams, formData, rawData, rawDataType, grpcRawData, multipartData, httpURL }  = extractParamsFromRequestEvent(httpRequestEvent);
    
    let reqObject = {
        id: existingId || uuidv4(),
        httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
        httpURL: httpURL,
        httpURLShowOnly: httpURL,
        headers: headers,
        queryStringParams: queryParams,
        bodyType: multipartData && multipartData.length > 0 ? "multipartData" : formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : grpcRawData && grpcRawData.length > 0 ? "grpcData" : "formData",
        formData: formData,
        multipartData,
        rawData: rawData,
        rawDataType: rawDataType,
        paramsType: grpcRawData ? "showBody" : "showQueryParams",
        responseStatus: "NA",
        responseStatusText: "",
        responseHeaders: "",
        responseBody: "",
        responsePayloadState: httpResponseEventPayload?.payloadState,
        recordedResponseHeaders: httpResponseEventPayload.hdrs ? JSON.stringify(httpResponseEventPayload.hdrs, undefined, 4) : "",
        recordedResponseBody: httpResponseEvent ? httpResponseEventPayload.body ? JSON.stringify(httpResponseEventPayload.body, undefined, 4) : "" : "",
        recordedResponseStatus: httpResponseEvent ? httpResponseEventPayload.status : "",
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
        metaData: httpResponseEvent ? httpResponseEvent.metaData : {},
        grpcData: applyGrpcDataToRequestObject(grpcRawData, httpRequestEvent.metaData.grpcConnectionSchema, httpRequestEvent.apiPath),
        grpcConnectionSchema: getConnectionSchemaFromMetadataOrApiPath(httpRequestEvent.metaData.grpcConnectionSchema, httpRequestEvent.apiPath)
    };
    return reqObject;
}


function updateHttpEvent(selectedApp, apiPath, service, httpEvent) {
    
    return {
        ...httpEvent,
        app: selectedApp,
        ...(apiPath && {apiPath: apiPath}),
        ...(service && {service: service})
    };
}

export function getHttpMethod(tabToSave){
    return isgRPCRequest(tabToSave) ? "POST" : tabToSave.httpMethod;
}

function getPathName(url){
    const urlData = urlParser(url);
    return _.trim(urlData.pathname, '/');
}


export function getReqResFromTabData(selectedApp, eachPair, tabToSave, runId, type, reqTimestamp, resTimestamp, urlEnvVal, currentEnvironment, tracer, traceDetails, parentSpanId, spanId) {
    const { headers, queryStringParams, bodyType, responseHeaders, responseBody, recordedResponseHeaders, recordedResponseBody, responseStatus, recordedResponseStatus, responsePayloadState } = tabToSave;

    const httpRequestEventTypeIndex = eachPair[0].eventType === "HTTPRequest" ? 0 : 1;
    const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
    let httpRequestEvent = eachPair[httpRequestEventTypeIndex];
    let httpResponseEvent = eachPair[httpResponseEventTypeIndex];
    let httpURL = "";

    // Set the URL
    if(bodyType === "grpcData") {
        httpURL = getRequestUrlFromSchema(tabToSave.grpcConnectionSchema);
    } else {    
        httpURL = tabToSave.httpURL;
    }
    
    // let apiPath = getApiPathFromRequestEvent(httpRequestEvent); // httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : "";
    // let apiPath = this.getPathName(applyEnvVarsToUrl(tabToSave.httpURL));
    const parsedUrl = urlParser(applyEnvVarsToUrl(httpURL), PLATFORM_ELECTRON ? {} : true);

    let apiPath = generateApiPath(parsedUrl);

    if(bodyType === "grpcData") {
        // Trim the all slashes in case of gRPC
        apiPath = _.trim(apiPath, '/');
    }
    

    if(httpRequestEvent.reqId === "NA") {
        let service = httpRequestEvent.service != "NA" ? httpRequestEvent.service : (parsedUrl.host || "NA");
        httpRequestEvent = updateHttpEvent(selectedApp, apiPath, service, httpRequestEvent);
        httpResponseEvent = updateHttpEvent(selectedApp, apiPath, service, httpResponseEvent);
        httpRequestEvent.metaData.typeOfRequest = "devtool";
    } else {
        if(!httpRequestEvent.metaData.typeOfRequest) httpRequestEvent.metaData.typeOfRequest = "apiCatalog";
        httpRequestEvent = updateHttpEvent(selectedApp, apiPath, "", httpRequestEvent);
        httpResponseEvent = updateHttpEvent(selectedApp, apiPath, "", httpResponseEvent);
    }

    if(!httpResponseEvent.metaData){
        httpResponseEvent.metaData = {};
    }
    httpRequestEvent.metaData.httpURL = httpURL;
    httpResponseEvent.metaData.httpURL = httpURL;

    if(httpRequestEvent.parentSpanId === null) {
        httpRequestEvent.parentSpanId = "NA"
    }

    if(httpRequestEvent.spanId === null) {
        httpRequestEvent.spanId = "NA"
    }

    if(traceDetails?.traceIdForEvent) {
        httpRequestEvent.traceId = traceDetails.traceIdForEvent
        httpResponseEvent.traceId = traceDetails.traceIdForEvent
    }

    if (parentSpanId) {
        httpRequestEvent.parentSpanId = parentSpanId
        httpResponseEvent.parentSpanId = parentSpanId
    }

    if(spanId) {
        httpRequestEvent.spanId = spanId
        httpResponseEvent.spanId = spanId
    }

    if(type === "History") {
        httpRequestEvent.metaData.collectionId = tabToSave.collectionIdAddedFromClient;
        httpRequestEvent.metaData.requestId = tabToSave.requestId;
        if(urlEnvVal) {
            const path = getPathName(urlEnvVal);
            httpRequestEvent.apiPath = path;
            httpResponseEvent.apiPath = path;                
        }else{
            httpRequestEvent.metaData.href = "";
        }
        if(currentEnvironment) {
            httpRequestEvent.metaData.currentEnvironment = currentEnvironment;
        }
        const renderEnvVars = getRenderEnvVars();
        apiPath = renderEnvVars(apiPath);
    }

    httpRequestEvent.metaData.hdrs = JSON.stringify(unSelectedRequestParamData(headers));
    httpRequestEvent.metaData.queryParams = JSON.stringify(unSelectedRequestParamData(queryStringParams));
    httpRequestEvent.metaData.bodyType = bodyType;
    httpRequestEvent.metaData.grpcConnectionSchema = JSON.stringify(tabToSave.grpcConnectionSchema);
    
    const httpReqestHeaders = extractHeadersToCubeFormat(selectedRequestParamData(headers), type);
    const httpRequestQueryStringParams = extractQueryStringParamsToCubeFormat(selectedRequestParamData(queryStringParams), type);
    let httpRequestBody = "";
    if (bodyType === "formData") {
        const { formData } = tabToSave;
        httpRequestEvent.metaData.formParams = JSON.stringify(unSelectedRequestParamData(formData));
        httpRequestBody = extractBodyToCubeFormat(selectedRequestParamData(formData), type);
    }
    if (bodyType === "multipartData") {
        const { multipartData } = tabToSave;
        httpRequestEvent.metaData.multipartData = JSON.stringify(unSelectedRequestParamData(multipartData));
        httpRequestBody = multipartDataToCubeFormat(selectedRequestParamData(multipartData), type);
    }
    if (bodyType === "rawData") {
        const { rawData } = tabToSave;
        httpRequestBody = extractBodyToCubeFormat(rawData, type);
    }
    if (isgRPCRequest(tabToSave)) {
        const { grpcData, grpcConnectionSchema } = tabToSave;
        httpRequestBody = extractGrpcBody(grpcData, grpcConnectionSchema);
        httpReqestHeaders["content-type"] = ["application/grpc"];          
    }

    const {traceIdKey, spanIdKey, parentSpanIdKeys} = generateTraceKeys(tracer)

    if(traceDetails?.traceId) {
        httpReqestHeaders[traceIdKey] = [traceDetails.traceId]
    }

    if (parentSpanId) {
        parentSpanIdKeys.forEach((key) => {
            httpReqestHeaders[key] = [parentSpanId]
        })
    }
    
    if (spanId && spanIdKey) {
        httpReqestHeaders[spanIdKey] = [spanId]
    }

    const httpMethod = getHttpMethod(tabToSave);
    let httpResponseHeaders, httpResponseBody, httpResponseStatus;
    if (type !== "History") {
        httpResponseHeaders = recordedResponseHeaders ? extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : responseHeaders ? extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : null;
        httpResponseBody = tryJsonParse(recordedResponseBody);
        httpResponseStatus = recordedResponseStatus;
    } else {
        httpResponseHeaders = responseHeaders ? extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : recordedResponseHeaders ? extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : null;
        httpResponseBody = tryJsonParse(responseBody);
        httpResponseStatus = responseStatus;
    }

    const httpResponseEventPayload = httpResponseEvent && httpResponseEvent.payload ?  httpResponseEvent.payload[1] : {};
    const httpResponseTrailers = httpResponseEventPayload.trls;
    const reqResCubeFormattedData = {   
        request: {
            ...httpRequestEvent,
            ...(reqTimestamp && { timestamp: reqTimestamp }),
            runId: runId,
            runType: "DevTool",
            payload: [
                isgRPCRequest(tabToSave) ? "GRPCRequestPayload": "HTTPRequestPayload",
                {
                    hdrs: httpReqestHeaders,
                    queryParams: httpRequestQueryStringParams,
                     formParams: [], //This can be removed after few releases. Backward compatibility.
                    ...(httpRequestBody && { body: httpRequestBody }),
                    method: httpMethod.toUpperCase(),
                    path: apiPath,
                    pathSegments: apiPath.split("/"),
                    payloadState :  isgRPCRequest(tabToSave) ? "UnwrappedDecoded": "WrappedEncoded",
                }
            ]
        },
        response: {
            ...httpResponseEvent,
            ...(resTimestamp && { timestamp: resTimestamp }),
            runId: runId,
            runType: "DevTool",
            payload: [
                isgRPCRequest(tabToSave) ? "GRPCResponsePayload": "HTTPResponsePayload",
                {
                    hdrs: httpResponseHeaders,
                    body: httpResponseBody,
                    status: httpResponseStatus,
                    payloadState: responsePayloadState || "WrappedEncoded", // pick from event if already present, or use WrappedEncoded
                    ...(isgRPCRequest(tabToSave) && {path: apiPath}), // path not needed in non-grpc case
                    trls: httpResponseTrailers,
                }
            ]
        }
    }
    return reqResCubeFormattedData;
}
