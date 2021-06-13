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

import _ from 'lodash';
import { v4 as uuidv4 } from "uuid";
import {Base64Binary} from '../../../shared/utils'
import {applyEnvVarsToUrl, getRenderEnvVars } from './envvar';
import cryptoRandomString from 'crypto-random-string';
import { store } from '../../helpers';
import URLParse from "url-parse";
import TabDataFactory from './TabDataFactory';
import MockConfigUtils from './mockConfigs.utils';

const generateRunId = () => {
    return new Date(Date.now()).toISOString()
}

const getHttpStatusColor = (status) => {
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

const getGrpcStatusColor = (status) => {
    if(status == 0) {
        return '#008000'
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
    return foundMockConfig ? JSON.parse(foundMockConfig.value) : { name: "", serviceConfigs: []};
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

const getMultipartField = (key, field) => {
    if(field.type == "file"){
        return {
            id: uuidv4(),
            name: key,
            value: field,
            description: "",
            selected: true,
            isFile: true
        }
    }else{
        return {
            id: uuidv4(),
            name: key,
            value: field.value,
            description: "",
            selected: true,
            isFile: false
        }
    }
}

const extractParamsFromRequestEvent = (httpRequestEvent) =>{
    let headers = [], queryParams = [], formData = [], multipartData = [], rawData = "", rawDataType = "", grpcRawData = "";
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
        httpRequestEvent.payload[1].queryParams[eachQueryParam].forEach(value => {
            queryParams.push({
                id: uuidv4(),
                name: eachQueryParam,
                value: value,
                description: "",
                selected: true,
            });
        })
    }

    let {httpURL, queryParamsFromUrl} = extractURLQueryParams(httpRequestEvent.apiPath)
    queryParamsFromUrl.forEach(param => {
        const existingQueryParam = _.find(queryParams, {name: param.name})
        if((existingQueryParam?.value != param.value) || (!existingQueryParam)) {
            queryParams.push(param)
        }
    })

    httpURL = httpRequestEvent.metaData.httpURL || httpURL; 

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
    const body = httpRequestEvent.payload[1].body;
    if (body) {
        if (!_.isString(body)) {
            try {
                const bodyType = httpRequestEvent.metaData?.bodyType;
                if(bodyType == "formData"){
                    Object.entries(body).forEach(([key, paramValues]) => {
                        formData.push({
                            id: uuidv4(),
                            name: key,
                            value: paramValues.join(","),
                            description: "",
                            selected: true,
                        });
                    });
                }else if(bodyType == "multipartData"){
                    Object.entries(body).forEach(([key, paramValues]) => {
                        if(_.isString(paramValues)){
                            multipartData.push(getMultipartField(key, paramValues));
                        }else{
                            paramValues.forEach((value) => {
                                multipartData.push(getMultipartField(key, value));
                                                            
                            });
                        }
                        
                    });
                }else{
                    const data = JSON.stringify(body, undefined, 4)
                    const dataType = "json";
                    if(isGrpc){
                        grpcRawData = data;
                    }else{
                        rawData = data;
                        rawDataType = dataType;
                    }
                }
            } catch (err) {
                console.error(err);
            }
        } else {
            if(isGrpc){
                grpcRawData = body;
            }else{

                rawData = body;
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
        headers, queryParams, formData, rawData, rawDataType, grpcRawData, multipartData, httpURL
    }
}

const formatHttpEventToTabObject = (reqId, requestIdsObj, httpEventReqResPair) => {
    const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
    const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
    const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
    const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];

    const serviceUrl = (new MockConfigUtils().getCurrentService(httpRequestEvent.service) || {}).url;

    const tabDataFactory = new TabDataFactory(httpRequestEvent, httpResponseEvent);
    return tabDataFactory.getReqObjectForAPICatalog(reqId, requestIdsObj, serviceUrl);
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
    const httpRequestQueryStringParamsRendered = new URLSearchParams();
    const preRequestqueryString = payload.queryParams;
    Object.entries(preRequestqueryString).forEach(([key, queryStringValueArray]) => {
        queryStringValueArray.forEach((value) => {
        httpRequestQueryStringParamsRendered.append(key, value);
      });
    });
  
    //Form params: Backward compatibility. Can be removed later.
    const formParams = payload.formParams;
    let bodyFormParams = new URLSearchParams();
    let containsFormParam = false;
    Object.entries(formParams).forEach(([key, paramValues]) => {
      containsFormParam = true;
      paramValues.forEach((value) => {
        bodyFormParams.append(key, value);
      });
    });

    let rawData = "";
    const bodyType = preRequestResult.metaData?.bodyType;
    if (payload.body) {
        if (!_.isString(payload.body)) {
          try {
              //In case of electron, Form data is created in listeners.js part
            if(bodyType == "formData" && !PLATFORM_ELECTRON){
                bodyFormParams = new URLSearchParams();
                Object.entries(payload.body).forEach(([key, paramValues]) => {
                    containsFormParam = true;
                    paramValues.forEach((value) => {
                        bodyFormParams.append(key, value);
                    });
                });
            }else if(bodyType == "multipartData" && !PLATFORM_ELECTRON){
                bodyFormParams = new FormData();
                Object.entries(payload.body).forEach(([key, paramValues]) => {
                    containsFormParam = true;
                    if(_.isArray(paramValues.value)){
                        paramValues.value.forEach((value) => {
                            formatMultipartData(key, value, bodyFormParams);
                        });
                    }else{  
                        formatMultipartData(key, paramValues, bodyFormParams);            
                    }
                });
            }else{
                rawData = JSON.stringify(
                    payload.body,
                    undefined,
                    4
                );
            }
          } catch (err) {
            console.error(err);
          }
        } else {
            if(isGrpc){
                const byteArray = Base64Binary.decode(payload.body);
                rawData = byteArray.buffer;

            }else{
                rawData = atob(payload.body);
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

const DataURIToBlob = (dataURI)=> {
	const splitDataURI = dataURI.split(',')
	const byteString = splitDataURI[0].indexOf('base64') >= 0 ? atob(splitDataURI[1]) : decodeURI(splitDataURI[1])
	const mimeString = splitDataURI[0].split(':')[1].split(';')[0]

	const ia = new Uint8Array(byteString.length)
	for (let i = 0; i < byteString.length; i++)
		ia[i] = byteString.charCodeAt(i)

	return new Blob([ia], { type: mimeString })
}

const formatMultipartData = (key, param, bodyFormParams)=>{
    switch(param.type){
        case "file":
            bodyFormParams.append(key, DataURIToBlob(param.value), param.filename);
            break;
        case "field":
        default:
            bodyFormParams.append(key, param.value);    
    }
}

const convertFileToString = async (file)=>{
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.readAsDataURL(file)
        reader.onload = () => resolve(reader.result)
        reader.onerror = (e) => reject(e)
      });
};

const tryJsonParse = (jsonString)=>{
    try{
        return JSON.parse(jsonString);
    }catch(e){}
    return jsonString;
}

const multipartDataToCubeFormat = (multipartData, type) =>{
    let formData = {};
    for(var each of multipartData) {
        if (each.name && each.value) {
            const nameRendered = getValueBySaveType(each.name, type)
            let valueRendered = getValueBySaveType(each.value, type);
            let value = undefined;
            
            if(each.isFile){
                const fileJSON = tryJsonParse(each.value); //this is type of IMultipartFileJSON
                value = { 
                    "value": fileJSON.value,
                    "type":"file",
                    "content-type": fileJSON.type,
                    "filename": fileJSON.filename,
                };
            }else{
                value = { 
                    "value": valueRendered,
                    "type":"field"
                };
            }
            if(formData[nameRendered]){
                formData[nameRendered] = [...formData[nameRendered], value];
            }else{
                formData[nameRendered] = [value];
            }
            
        }
    }
    return formData;
} 

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

const generateTraceIdDetails = (tracer, spanId) => {
    let traceId = cryptoRandomString({length:16})
    if (tracer==="meshd" || tracer==="jaeger" || !tracer) {
        if (!spanId)
            throw new Error("Error generating traceId: spanId not present")
        
        return {traceId:`${traceId}:${spanId}:0:1`, traceIdForEvent: traceId}; // full and only traceId part for event
    } else if (tracer==="datadog") {
        traceId = cryptoRandomString({length:19, type: "numeric"})
        return {traceId, traceIdForEvent: traceId}
    } else {
        return {traceId, traceIdForEvent: traceId}; // both same
    }
}

const generateSpanId = (tracer) => {
    if(tracer==="datadog") {
        return cryptoRandomString({length:19, type: "numeric"})
    } else {
        return cryptoRandomString({length:16})
    }
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
                    const nameRendered = getValueBySaveType(each.name, type)
                    const valueRendered = getValueBySaveType(each.value, type)
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
                    const nameRendered = getValueBySaveType(eachHeader, type)
                    const valueRendered = getValueBySaveType(headersReceived[eachHeader], type);
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
    const traceIdDetails = generateTraceIdDetails(tracer, spanId)
    return {
        traceIdDetails,
        spanId,
        parentSpanId,
        traceKeys,
    }
}

const generateUrlWithQueryParams = (httpURL, queryStringParams) => {
    const urlSearch = queryStringParams.filter(queryParam => queryParam.selected)
        .map(({name, value}) => (value == undefined ? `${name}`: `${name}=${value}`))        
        .join("&");
    return urlSearch ? `${httpURL}?${urlSearch}`: httpURL;
}

const extractURLQueryParams = (url) => {
    let httpURL = url
    let queryParamsFromUrl = []

    const parsedURLParts = url.split("?")
    if(parsedURLParts[1]) {
        httpURL = parsedURLParts[0]
        queryParamsFromUrl = parsedURLParts[1].split("&").map((part) => {
            let indexOfEqual = part.indexOf("=");
            let value = undefined;
            let key = part;
            if(indexOfEqual !== -1){
                key = part.substr(0, indexOfEqual);
                value = part.substr(indexOfEqual + 1);
            }
            return {
                name: key,
                value: value, 
                selected: true,
                id: uuidv4(),
                description: "",
            }
        })
    }
        
    return {httpURL, queryParamsFromUrl}
}

const generateContentTypeHeaderValue = (type, value, currentTab) => {
    if(value === 'formData') {
        return 'application/x-www-form-urlencoded';
    }

    if(value === 'multipartData') {
        return 'multipart/form-data';
    }

    if(value === 'rawData') {
        type = 'rawDataType';
        value = currentTab.rawDataType;
    }

    if(type === 'rawDataType') {
        switch(value) {
            case 'js':
                return 'application/js';
            case 'json':
                return 'application/json';
            case 'text':
                return 'application/text';
            case 'html':
                return 'application/html';
            case 'xml':
                return 'application/xml';
            default:
                return 'application/text';
        }
    }
}

const updateHeaderBasedOnContentType = (existingHeaders, type, value, currentTab) => {
    const contentTypeHeaderObject = existingHeaders.find(headerObject => headerObject.name.toLowerCase() === 'content-type');

    if(contentTypeHeaderObject) {
        // if content type exists, then update this and return the object

        // update the value
        contentTypeHeaderObject['value'] = generateContentTypeHeaderValue(type, value, currentTab);
        
        // filter out the old value
        const filteredHeaders = existingHeaders.filter(headerObject => headerObject.name.toLowerCase() !== 'content-type');

        return [...filteredHeaders, contentTypeHeaderObject];
    } 
    // add content type and return the object
    const newContentTypeHeaderObject = {
        description: "",
        id: uuidv4(),
        name: "content-type",
        selected: true,
        value: generateContentTypeHeaderValue(type, value, currentTab)
    };
    
    return [...existingHeaders, newContentTypeHeaderObject];
}

export function getInternalHeaders(){
    return ["x-datadog-trace-id", "x-datadog-parent-id", "ot-baggage-parent-span-id",
    "x-b3-traceid", "x-b3-spanid", "baggage-parent-span-id", "x-b3-parentspanid",
    "x-b3-sampled","x-istio-attributes", "x-request-id","x-forwarded-proto", ":method", ":path", ":authority", "user-agent",
    "sec-ch-ua", "sec-ch-ua-mobile", "sec-fetch-site", "sec-fetch-dest", "sec-fetch-mode", "md-trace-id",
    "uber-trace-id", "uberctx-parent-span-id", "mdctxmd-parent-span" ];
}
export function filterInternalHeaders(headers, isFilter){
    if(isFilter){
        return headers.filter( header => getInternalHeaders().indexOf(header.name.toLowerCase()) == -1 )
    }
    return headers;
}


export function isLocalhostUrl(url){
    const urlFetched = applyEnvVarsToUrl(url);
    if(urlFetched){
        return URLParse(urlFetched).hostname == "localhost";
    }
    return false;
}


export function getHostName(url) {
    try{
        const parsedUrl = URLParse(applyEnvVarsToUrl(url));
        return parsedUrl.hostname;
    }
    catch{
        //Silent error: this could be due to parsing while user is still is typing in URL entry
        return "";
    }
}

export { 
    generateRunId,
    getHttpStatusColor,
    getGrpcStatusColor,
    getCurrentMockConfig,
    getTraceTableTestReqData,
    getApiPathFromRequestEvent,
    hasTabDataChanged,
    formatHttpEventToTabObject,
    preRequestToFetchableConfig,
    selectedRequestParamData,
    unSelectedRequestParamData,
    extractParamsFromRequestEvent,
    Base64Binary,
    generateTraceIdDetails,
    generateSpanId,
    generateSpecialParentSpanId,
    extractQueryStringParamsToCubeFormat,
    extractBodyToCubeFormat,
    extractHeadersToCubeFormat,
    generateTraceKeys,
    getTracerForCurrentApp,
    getTraceDetailsForCurrentApp,
    generateUrlWithQueryParams,
    extractURLQueryParams,
    multipartDataToCubeFormat,
    convertFileToString,
    tryJsonParse,
    updateHeaderBasedOnContentType
};