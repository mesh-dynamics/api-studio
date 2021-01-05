import _ from 'lodash';
import { v4 as uuidv4 } from "uuid";
import {Base64Binary} from '../../../shared/utils'
import {applyEnvVarsToUrl, getRenderEnvVars } from './envvar';
import cryptoRandomString from 'crypto-random-string';
import { store } from '../../helpers';

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
        return parsedUrl.pathname.split('/').filter(Boolean).join('/');
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

//Following could be used globally, can be moved to a common utility file.
const isValidJSON = (jsonString) =>{
    try{
         JSON.parse(jsonString);
    }catch(e){
        return false;
    }
    return true;
}

const hasTabDataChanged = (tab) => {
    if (tab.hasChanged) {
      return true;
    }

    if (_.find(tab.outgoingRequests, {hasChanged: true})) {
      return true;
    }

    return false;
}

const extractParamsFromRequestEvent = (httpRequestEvent) =>{
    let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "", grpcData = "", grpcDataType = "";
    const isGrpc = httpRequestEvent.payload[0] =="GRPCRequestPayload";
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
                const data = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                const dataType = "json";
                if(isGrpc){
                    grpcData = data;
                    grpcDataType = dataType;
                }else{
                    rawData = data;
                    rawDataType = dataType;
                }
            } catch (err) {
                console.error(err);
            }
        } else {
            if(isGrpc){
                grpcData = httpRequestEvent.payload[1].body;
                grpcDataType = "json";
            }else{

                rawData = httpRequestEvent.payload[1].body;
                rawDataType = "text";
            }
        }
    }

    // Add unselected params (queryString, formData and headers) from metadata
    if(httpRequestEvent.metaData.hdrs){
        const hdrs = JSON.parse(httpRequestEvent.metaData.hdrs);
        for (let eachHeader in hdrs) {
          headers.push({
            id: uuidv4(),
            name: hdrs[eachHeader].name,
            value: hdrs[eachHeader].value, //TODO check for multiple headers
            description: "",
            selected: false,
          });
        }
      }
      if(httpRequestEvent.metaData.queryParams){
        const queryParamStored = JSON.parse(httpRequestEvent.metaData.queryParams);
        for (let eachQueryParam in queryParamStored) {
          queryParams.push({
            id: uuidv4(),
            name: queryParamStored[eachQueryParam].name,
            value:
            queryParamStored[eachQueryParam].value,
            description: "",
            selected: false,
          });
        }
      }
      if(httpRequestEvent.metaData.formParams){
        const formParams = JSON.parse(httpRequestEvent.metaData.formParams);
        for (let eachFormParam in formParams) {
          formData.push({
            id: uuidv4(),
            name: formParams[eachFormParam].name,
            value: formParams[eachFormParam].value, //Check if join is required
            description: "",
            selected: false,
          });
        }
      }

    return{
        headers, queryParams, formData, rawData, rawDataType, grpcData, grpcDataType
    }
}

const formatHttpEventToTabObject = (reqId, requestIdsObj, httpEventReqResPair) => {
    const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
    const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
    const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
    const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
    const { headers, queryParams, formData, rawData, rawDataType, grpcData, grpcDataType }  = extractParamsFromRequestEvent(httpRequestEvent);
    let reqObject = {
        httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
        httpURL: "{{{url}}}/" + httpRequestEvent.apiPath,
        httpURLShowOnly: httpRequestEvent.apiPath,
        headers: headers,
        queryStringParams: queryParams,
        bodyType:   formData?.length > 0
            ? "formData"
            : rawData?.length > 0
            ? "rawData"
            : grpcData?.length ? "grpcData" : "formData",
        formData: formData,
        rawData: rawData,
        grpcData: grpcData,
        rawDataType: rawDataType,
        paramsType: grpcData && grpcData.length ? "showBody": "showQueryParams",
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

const selectedRequestParamData = (paramsData)=>{
    return paramsData.filter(param => param.selected);
}
const unSelectedRequestParamData = (paramsData)=>{
    return paramsData.filter(param => !param.selected);
}



const preRequestToFetchableConfig = (preRequestResult, httpURL) => {
    const payload = preRequestResult.payload[1];
    const isGrpc = preRequestResult.payload[0] == "GRPCRequestPayload";
    // URL
    const httpRequestURLRendered = applyEnvVarsToUrl(httpURL);
  
    //Headers
    const headers = {};
    const preRequestheaders = payload.hdrs;
    Object.entries(preRequestheaders).forEach(([headerName, headerValues]) => {
        headerValues.forEach((headerValue) => {
        if (headerName && headerValue && headerName.indexOf(":") < 0)
          headers[headerName] = headerValue;
        if (headerName === "x-b3-spanid" && headerValue)
          headers["baggage-parent-span-id"] = headerValue;
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

    let rawData = "";

    if (payload.body) {
        if (!_.isString(payload.body)) {
          try {
            rawData = JSON.stringify(
                payload.body,
              undefined,
              4
            );
          } catch (err) {
            console.error(err);
          }
        } else {
            if(isGrpc){
                const byteArray = Base64Binary.decode(payload.body);
                rawData = byteArray.buffer;

            }else{
                rawData = payload.body;
            }
        }
    }
  
    const fetchConfigRendered = {
      method: payload.method,
      headers,      
     ...( !(payload.method == "GET" || payload.method == "HEAD") && {body: containsFormParam ? bodyFormParams : rawData}),
    };
  
    return [
      httpRequestURLRendered,
      httpRequestQueryStringParamsRendered,
      fetchConfigRendered,
    ];
};

const generateTraceKeys = (tracer) => {
    let traceIdKey, spanIdKey, parentSpanIdKeys = [];
    switch (tracer) {
        case "jaeger":
            traceIdKey = "uber-trace-id"
            parentSpanIdKeys = ["uberctx-parent-span-id"]
            // no span id key
            break
            
        case "zipkin":
            traceIdKey = "x-b3-traceid"
            parentSpanIdKeys = ["baggage-parent-span-id", "x-b3-parentspanid"]
            spanIdKey = "x-b3-spanid"
            break;

        case "datadog":
            traceIdKey = "x-datadog-trace-id"
            parentSpanIdKeys = ["ot-baggage-parent-span-id"]
            spanIdKey = "x-datadog-parent-id"
            break

        case "meshd": // default to meshd
        default:
            traceIdKey = "md-trace-id";
            parentSpanIdKeys = ["mdctxmd-parent-span"];
            // no span id key    
    }
    return {traceIdKey, spanIdKey, parentSpanIdKeys}
}

const generateTraceId = (tracer, spanId) => {
    const traceId = cryptoRandomString({length:16})
    if (tracer==="meshd" || tracer==="jaeger") {
        if (!spanId)
            throw new Error("Error generating traceId: spanId not present")
        
        return encodeURIComponent(`${traceId}:${spanId}:0:1`);
    } else {
        return traceId;
    }
}

const generateSpanId = (tracer) => {
    return cryptoRandomString({length:16})
}

const generateSpecialParentSpanId = (tracer) => {
    return "ffffffffffffffff"    
}

const extractQueryStringParamsToCubeFormat = (httpRequestQueryStringParams, type)=> {
    let qsParams = {};
    httpRequestQueryStringParams.forEach(each => {
        if (each.name && each.value) {
            const nameRendered = getValueBySaveType(each.name, type)
            const valueRendered = getValueBySaveType(each.value, type)
            if (qsParams[nameRendered]) {
                qsParams[nameRendered] = [...qsParams[nameRendered], valueRendered];
            } else {
                qsParams[nameRendered] = [valueRendered];
            }
        }
    })
    return qsParams;
}

const extractBodyToCubeFormat = (httpRequestBody, type) => {
    let formData = {};
    if (_.isArray(httpRequestBody)) {
        httpRequestBody.forEach(each => {
            if (each.name && each.value) {
                const nameRendered = getValueBySaveType(each.name, type)
                const valueRendered = getValueBySaveType(each.value, type)
                if(formData[nameRendered]){
                    formData[nameRendered] = [...formData[nameRendered], valueRendered];
                }else{
                    formData[nameRendered] = [valueRendered];
                }
            }
        })
        return formData;
    } else {
        return getValueBySaveType(httpRequestBody, type);
    }
};
const getValueBySaveType = (value, type) => {
        if(!_.isString(value)){
            return value;
        }
        const renderEnvVars = getRenderEnvVars();
        return type !== "History" ? value : renderEnvVars(value);
}

const extractHeadersToCubeFormat = (headersReceived, type="")=> {
        let headers = {};
        if (_.isArray(headersReceived)) {
            headersReceived.forEach(each => {
                if (each.name && each.value) {
                    const nameRendered = this.getValueBySaveType(each.name, type)
                    const valueRendered = this.getValueBySaveType(each.value, type)
                    if(headers[nameRendered]){
                        headers[nameRendered] = [...headers[nameRendered], valueRendered];
                    }else{
                        headers[nameRendered] = [valueRendered];
                    }
                }
            });
        } else if (_.isObject(headersReceived)) {
            Object.keys(headersReceived).map((eachHeader) => {
                if (eachHeader && headersReceived[eachHeader]) {
                    const nameRendered = this.getValueBySaveType(eachHeader, type)
                    const valueRendered = this.getValueBySaveType(headersReceived[eachHeader], type);
                    if(_.isArray(headersReceived[eachHeader])) headers[nameRendered] = valueRendered;
                    if(_.isString(headersReceived[eachHeader])) headers[nameRendered] = [valueRendered];
                }
            })
        }

    return headers;
}

const getTracerForCurrentApp = () => {
    const {cube: {selectedApp, appsList}} = store.getState()
    if (!selectedApp || !appsList?.length) {
        return ""
    }
    const {tracer} = _.find(appsList, {name: selectedApp})
    return tracer
}

const getTraceDetailsForCurrentApp = () => {
    const tracer = getTracerForCurrentApp()
    const traceKeys = generateTraceKeys(tracer)
    const spanId = generateSpanId(tracer)
    const parentSpanId = generateSpecialParentSpanId(tracer)
    const traceId = generateTraceId(tracer, spanId)
    return {
        traceId,
        spanId,
        parentSpanId,
        traceKeys,
    }
}

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
    selectedRequestParamData,
    unSelectedRequestParamData,
    extractParamsFromRequestEvent,
    isValidJSON,
    Base64Binary,
    generateTraceId,
    generateSpanId,
    generateSpecialParentSpanId,
    extractQueryStringParamsToCubeFormat,
    extractBodyToCubeFormat,
    extractHeadersToCubeFormat,
    generateTraceKeys,
    getTracerForCurrentApp,
    getTraceDetailsForCurrentApp,
};