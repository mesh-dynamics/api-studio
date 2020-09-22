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


const getCurrentMockConfig = (mockConfigList, selectedMockConfig) => {
    const foundMockConfig = _.find(mockConfigList, { key: selectedMockConfig });
    return foundMockConfig ? JSON.parse(foundMockConfig.value) : {};
};

export { 
    generateRunId,
    getStatusColor,
    getTraceTableTestReqData,
    getCurrentMockConfig
};