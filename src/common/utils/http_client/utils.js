import {parseExpressionAt} from 'acorn';
import _, { head } from 'lodash';

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

const getCurrentEnvirnoment = (environmentList, selectedEnvironment) => {
    return _.find(environmentList, { name: selectedEnvironment });
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

export { 
    generateRunId,
    getStatusColor,
    generateApiPath,
    getCurrentMockConfig,
    getCurrentEnvirnoment, 
    getTraceTableTestReqData,
    getApiPathFromRequestEvent,
    getTraceTableTestReqData,
    getCurrentMockConfig
};