import _ from 'lodash';
import { v4 as uuidv4 } from "uuid";

const generateRunId = () => {
    return new Date(Date.now()).toISOString()
}

const getStatusColor = (status) => {
    if(status >=100 && status <= 399) {
        if(status >=200 && status <= 299) {
            return '#008000';
        }
        return '#FFFF00';
    } else if ( status == 'NA' || status == '' || status == undefined) {
        return 'none';
    } else {
        return '#FF0000';
    }
}

const getRecordedResponseOfParent = (currentSelectedTab) => {
    return {
        responseStatus: currentSelectedTab.responseStatus,
        responseStatusText: currentSelectedTab.responseStatusText,
        responseHeaders: currentSelectedTab.responseHeaders,
        responseBody: currentSelectedTab.responseBody
    };
}

const getRecordedResponseOfOutgoingRequests = (recordedHistory, selectedTraceTableTestReqTabId) => {
    const requestData = recordedHistory
                            .outgoingRequests
                            .find(request => request.requestId === selectedTraceTableTestReqTabId);
    return {
        responseStatus: requestData.recordedResponseStatus,
        responseStatusText: requestData.recordedResponseStatusText || "",
        responseHeaders: requestData.recordedResponseHeaders,
        responseBody: requestData.recordedResponseBody    
    }
};

const getTraceTableTestReqData = (currentSelectedTab, selectedTraceTableTestReqTabId) => {
    // Check if the request has be run. 
    if(selectedTraceTableTestReqTabId && currentSelectedTab.recordedHistory) {
        // If Yes, check is the selected request Id is parent and return response of parent
        // else return response of recorded outgoing requests
        return (
            selectedTraceTableTestReqTabId === currentSelectedTab.recordedHistory.requestId 
            ? getRecordedResponseOfParent(currentSelectedTab)
            : getRecordedResponseOfOutgoingRequests(currentSelectedTab.recordedHistory, selectedTraceTableTestReqTabId)
        ) 
    } 
        // If not return empty values
    return {
        responseStatus: "",
        responseStatusText: "",
        responseHeaders: "",
        responseBody: ""
    }
};

const getCurrentMockConfig = (mockConfigList, selectedMockConfig) => {
    const foundMockConfig = _.find(mockConfigList, { key: selectedMockConfig });
    return foundMockConfig ? JSON.parse(foundMockConfig.value) : {};
};

const generateApiPath = (parsedUrl) => {
    // Handle if 'file' protocol is detected
    if(parsedUrl.protocol.includes('file')) {
        return parsedUrl.pathname.split('/').filter(Boolean).slice(2).join('/');
    }

    // Handle if no protocol is detected
    if(!parsedUrl.protocol) {
        return parsedUrl.pathname.split('/').filter(Boolean).slice(1).join('/');
    }

    return parsedUrl.pathname ? parsedUrl.pathname : parsedUrl.host;
};

const getApiPathFromRequestEvent = (requestEvent) => {

    const { payload, apiPath } = requestEvent;
    const EMPTY_STRING = "";

    if(apiPath) {
        return apiPath;
    }

    if(payload[1].path) {
        return payload[1].path;
    }

    return EMPTY_STRING;
};

const hasTabDataChanged = (tab) => {
    if (tab.hasChanged) {
      return true;
    }

    if (_.find(tab.outgoingRequests, {hasChanged: true})) {
      return true;
    }

    return false;
}

const formatHttpEventToTabObject = (reqId, requestIdsObj, httpEventReqResPair) => {
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

const preRequestToFetchableConfig = (preRequestResult) => {
    const payload = preRequestResult.payload[1];
    // URL
    const httpRequestURLRendered = preRequestResult.metaData.href;
  
    //Headers
    const headers = new Headers();
    const preRequestheaders = payload.hdrs;
    Object.entries(preRequestheaders).forEach(([headerName, headerValues]) => {
        headerValues.forEach((headerValue) => {
        if (headerName && headerValue && headerName.indexOf(":") < 0)
          headers.append(headerName, headerValue);
        if (headerName === "x-b3-spanid" && headerValue)
          headers.append("baggage-parent-span-id", headerValue);
      });
    });
  
    //Query String
    const httpRequestQueryStringParamsRendered = {};
    const preRequestqueryString = payload.queryParams;
    Object.entries(preRequestqueryString).forEach(([key, queryStringValueArray]) => {
        queryStringValueArray.forEach((value) => {
        httpRequestQueryStringParamsRendered[key] = value;
      });
    });
  
    //Form params
    const formParams = payload.formParams;
    const bodyFormParams = new URLSearchParams();
    let containsFormParam = false;
    Object.entries(formParams).forEach(([key, paramValues]) => {
      containsFormParam = true;
      paramValues.forEach((value) => {
        bodyFormParams.append(key, value);
      });
    });
  
    const fetchConfigRendered = {
      method: payload.method,
      headers,
      body: containsFormParam ? bodyFormParams : payload.body,
    };
  
    return [
      httpRequestURLRendered,
      httpRequestQueryStringParamsRendered,
      fetchConfigRendered,
    ];
  };

export { 
    generateRunId,
    getStatusColor,
    generateApiPath,
    getCurrentMockConfig,
    getTraceTableTestReqData,
    getApiPathFromRequestEvent,
    hasTabDataChanged,
    formatHttpEventToTabObject,
    preRequestToFetchableConfig,
};