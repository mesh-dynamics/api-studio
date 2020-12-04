import _ from 'lodash';
import { v4 as uuidv4 } from "uuid";

import {applyEnvVarsToUrl} from './envvar';

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
        bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
        formData: formData,
        rawData: rawData,
        grpcData: grpcData,
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

const selectedRequestParamData = (paramsData)=>{
    return paramsData.filter(param => param.selected);
}
const unSelectedRequestParamData = (paramsData)=>{
    return paramsData.filter(param => !param.selected);
}


const Base64Binary = {
	_keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
	
	/* will return a  Uint8Array type */
	decodeArrayBuffer: function(input) {
		var bytes = (input.length/4) * 3;
		var ab = new ArrayBuffer(bytes);
		this.decode(input, ab);
		
		return ab;
	},

	removePaddingChars: function(input){
		var lkey = this._keyStr.indexOf(input.charAt(input.length - 1));
		if(lkey == 64){
			return input.substring(0,input.length - 1);
		}
		return input;
	},

	decode: function (input, arrayBuffer) {
		//get last chars to see if are valid
		input = this.removePaddingChars(input);
		input = this.removePaddingChars(input);

		var bytes = parseInt((input.length / 4) * 3, 10);
		
		var uarray;
		var chr1, chr2, chr3;
		var enc1, enc2, enc3, enc4;
		var i = 0;
		var j = 0;
		
		if (arrayBuffer)
			uarray = new Uint8Array(arrayBuffer);
		else
			uarray = new Uint8Array(bytes);
		
		input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
		
		for (i=0; i<bytes; i+=3) {	
			//get the 3 octects in 4 ascii chars
			enc1 = this._keyStr.indexOf(input.charAt(j++));
			enc2 = this._keyStr.indexOf(input.charAt(j++));
			enc3 = this._keyStr.indexOf(input.charAt(j++));
			enc4 = this._keyStr.indexOf(input.charAt(j++));
	
			chr1 = (enc1 << 2) | (enc2 >> 4);
			chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
			chr3 = ((enc3 & 3) << 6) | enc4;
	
			uarray[i] = chr1;			
			if (enc3 != 64) uarray[i+1] = chr2;
			if (enc4 != 64) uarray[i+2] = chr3;
		}
	
		return uarray;	
    },
    encode: function(dataArr){
        var encoder = new TextEncoder("ascii");
        var decoder = new TextDecoder("ascii");
        var base64Table = encoder.encode(this._keyStr);

        var padding = dataArr.byteLength % 3;
        var len = dataArr.byteLength - padding;
        padding = padding > 0 ? (3 - padding) : 0;
        var outputLen = ((len/3) * 4) + (padding > 0 ? 4 : 0);
        var output = new Uint8Array(outputLen);
        var outputCtr = 0;
        for(var i=0; i<len; i+=3){              
            var buffer = ((dataArr[i] & 0xFF) << 16) | ((dataArr[i+1] & 0xFF) << 8) | (dataArr[i+2] & 0xFF);
            output[outputCtr++] = base64Table[buffer >> 18];
            output[outputCtr++] = base64Table[(buffer >> 12) & 0x3F];
            output[outputCtr++] = base64Table[(buffer >> 6) & 0x3F];
            output[outputCtr++] = base64Table[buffer & 0x3F];
        }
        if (padding == 1) {
            var buffer = ((dataArr[len] & 0xFF) << 8) | (dataArr[len+1] & 0xFF);
            output[outputCtr++] = base64Table[buffer >> 10];
            output[outputCtr++] = base64Table[(buffer >> 4) & 0x3F];
            output[outputCtr++] = base64Table[(buffer << 2) & 0x3F];
            output[outputCtr++] = base64Table[64];
        } else if (padding == 2) {
            var buffer = dataArr[len] & 0xFF;
            output[outputCtr++] = base64Table[buffer >> 2];
            output[outputCtr++] = base64Table[(buffer << 4) & 0x3F];
            output[outputCtr++] = base64Table[64];
            output[outputCtr++] = base64Table[64];
        }
        
        var ret = decoder.decode(output);
        output = null;
        dataArr = null;
        return ret;
    }
}

const preRequestToFetchableConfig = (preRequestResult, httpURL) => {
    const payload = preRequestResult.payload[1];
    const isGrpc = preRequestResult.payload[0] == "GRPCRequestPayload";
    // URL
    const httpRequestURLRendered = applyEnvVarsToUrl(httpURL);
  
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
    Base64Binary
};