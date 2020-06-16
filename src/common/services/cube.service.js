import config from '../config';
import api from '../api';
import _ from 'lodash';

// TODO: replace console log statements with logging
const fetchAppsList = async () => {
    const user = JSON.parse(localStorage.getItem('user'));
    try {
        return await api.get(`${config.apiBaseUrl}/app`);
    } catch(error) {
        console.log("Error Fetching Applist \n", error);
        throw new Error("Error Fetching Applist");
    }
}

const getGraphDataByAppId = async (appId) => {
    try {
        return await api.get(`${config.apiBaseUrl}/app/${appId}/service-graphs`);
    } catch (error) {
        console.log("Error fetching service graphs \n", error);
        throw new Error("Error fetching service graphs");
    }
};

const getInstanceList = async () => {
    try {
        return await api.get(`${config.apiBaseUrl}/instance`);
    } catch (error) {
        console.log("Error fetching instance list \n", error);
        throw new Error("Error fetching instance list"); 
    }
};

//TODO: Not sure if this call is actually being used
const getGraphData = async () => {
    try {
        return await api.get(`${config.apiBaseUrl}/service_graph`);
    } catch (error) {
        console.log("Error fetching graph data \n", error);
        throw new Error("Error fetching graph data");
    }
};

const getTestConfigByAppId = async (appId) => {
    try {
        return await api.get(`${config.apiBaseUrl}/app/${appId}/test-configs`);
    } catch(error) {
        console.log("Error fetching test config \n", error);
        throw new Error("Error fetching test config");
    }
};

const fetchCollectionList = async (app) => {
    const user = JSON.parse(localStorage.getItem('user')); // TODO: Change this to be passed from auth tree
    try {
        return await api.get(`${config.recordBaseUrl}/searchRecording?customerId=${user.customer_name}&app=${app}`);
    } catch(error) {
        console.log("Error fetching test config \n", error);
        throw new Error("Error fetching test config");
    }
};

const forceCompleteReplay = async (fcId) => {
    try {
        return await api.post(`${config.replayBaseUrl}/forcecomplete/${fcId}`, null);
    } catch (error) {
        throw (error.response);
    }
};

const checkStatusForReplay = async (replayId) => {
    const requestOptions = {
        headers: {
            "cache-control": "no-cache",
        }
    };

    try {
        return await api.get(`${config.replayBaseUrl}/status/${replayId}`, requestOptions);
    } catch (error) {
        console.log("Errors in replay status \n", error);
        throw error;
    }
};

const fetchTimelineData = (app, userId, endDate, startDate, numResults, testConfigName, goldenName) => {
    const user = JSON.parse(localStorage.getItem('user')); // TODO: Update to pass user from correct place
    const endDateString = endDate.toISOString();
    const params = new URLSearchParams();
    const requestOptions = {
        headers: {
            "cache-control": "no-cache",
        }
    };

    params.set("byPath", "y");
    params.set("endDate", endDateString);
    // TODO: Simplify these ifs
    if(startDate) {
        let sd = startDate.toISOString();
        params.set("startDate", sd);
    }

    if (userId !== 'ALL') {
        params.set("userId", user.username);
    }
    
    if (numResults || numResults == 0){
        params.set("numResults", numResults);
    }

    if(testConfigName) {
        params.set("testConfigName", testConfigName);
    }
    
    if(goldenName) {
        params.set("golden_name", goldenName);
    }

    try {
        return api.get(`${config.analyzeBaseUrl}/timelineres/${user.customer_name}/${app}?${params.toString()}`, requestOptions);
    } catch (error) {
        console.log("Error fetching timeline data \n", error);
        throw error;
    }
};

const getCollectionUpdateOperationSet = async (app) => {
    const user = JSON.parse(localStorage.getItem('user')); // TODO: Update to pass user from correct place
    const requestOptions = {
        headers: {
            "Access-Control-Allow-Origin": "*",
        }
    }
    const url = `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/create?customer=${user.customer_name}&app=${app}`;
    try {
        return await api.post(url, null, requestOptions);
    } catch (error) {
        console.log("Error updating operation set \n", error);
        throw error;
    }
};

const fetchJiraBugData = async (replayId, apiPath) => {
    try{
        return await api.get(`${config.apiBaseUrl}/jira/issue/details?replayId=${replayId}&apiPath=${apiPath}`);
    } catch(error) {
        console.log("Error fetching Jira Bugs\n", error);
        throw error;
    }
};

const fetchAnalysisStatus = async (replayId) => {
    const requestOptions = {
        headers: {
            "cache-control": "no-cache",
        }
    }

    try {
        return await api.get(`${config.analyzeBaseUrl}/status/${replayId}`, requestOptions);
    } catch (error) {
        console.log("Error fetching analysis", error);
        throw error;
    }
};

const getTestConfig = async (app, testConfigName) => {
    const user = JSON.parse(localStorage.getItem('user'));
    try {
        return await api.get(`${config.apiBaseUrl}/test_config/${user.customer_name}/${app}/${testConfigName}`);
    } catch (error) {
        console.error("Error fetching Test Config for Test summary!", error);
        throw error;
    }
};

const fetchFacetData = async (replayId) => {
    const searchParams = new URLSearchParams();
    
    searchParams.set("numResults", 0);

    try {
        const dataList = await api.get(`${config.analyzeBaseUrl}/analysisResByPath/${replayId}?${searchParams.toString()}`);
        
        if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.facets)) {
            console.log("facets data is empty")
        }

        return dataList;
    } catch(error) {
        console.error("Error fetching facet data \n", error);
        throw error;
    }

};

const removeReplay = async (replayId) => {
    try {
        return await api.post(`${config.replayBaseUrl}/softDelete/${replayId}`);
    } catch(error) {
        console.log("Error deleting replay\n", error);
        throw error;
    }
};

const getNewTemplateVerInfo = async (app, currentTemplateVer) => {
    const user = JSON.parse(localStorage.getItem('user'));
    const requestOptions = {
        headers: {
            "Access-Control-Allow-Origin": "*",
        }
    };

    try {
        return await api.post(`${config.analyzeBaseUrl}/initTemplateOperationSet/${user.customer_name}/${app}/${currentTemplateVer}`, null, requestOptions);
    } catch (error) {
        console.log("Error getting new template version info\n", error);
        throw error;
    }
};

const getProjectList = async () => {
    try {
        return await api.get(`${config.apiBaseUrl}/jira/projects`);
    } catch (error) {
        console.log("Error fetching jira projects", error);
        throw error;
    }
}

const createJiraIssue = async (summary, description, issueTypeId, projectId, replayId, apiPath, requestId, jsonPath) => {
    const reqBody = {
        summary: summary,
        description: description,
        issueTypeId: issueTypeId,
        projectId: projectId,
        replayId: replayId,
        apiPath: apiPath,
        requestId : requestId,
        jsonPath: jsonPath,
    }

    try {
        return await api.post(`${config.apiBaseUrl}/jira/issue/create`, reqBody);
    } catch (error) {
        const { response: { data: { message } } } = error;
        console.log("Error creating jira issue\n", error.message);
        throw new Error(message);
    }
}

const getResponseTemplate = async (selectedApp, pathResultsParams, reqOrRespCompare, jsonPath) => {
    const user = JSON.parse(localStorage.getItem('user')); // TODO: Take this from auth reducer
    const { currentTemplateVer, service, path } = pathResultsParams;
    const url = `${config.analyzeBaseUrl}/getRespTemplate/${user.customer_name}/${selectedApp}/${currentTemplateVer}/${service}/${reqOrRespCompare}?apiPath=${path}&jsonPath=${jsonPath}`;

    const requestOptions = {
        headers: {
            "cache-control": "no-cache",
        }
    };

    try {
        return await api.get(url, requestOptions);
    } catch (error) {
        console.log("Error fetching response template", error);
        throw error;
    }
};

const fetchAnalysisResults = async (replayId, searchParams) => {
    try {
        const dataList = await api.get(`${config.analyzeBaseUrl}/analysisResByPath/${replayId}?${searchParams.toString()}`);
        
        if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.res)) {
            console.log("results list is empty")
        } 

        return dataList;
    } catch (error) {
        throw error;
    }
};

const unifiedGoldenUpdate = async (data) => {
    const requestOptions = {
        headers: {
            'Access-Control-Allow-Origin': '*',
        }
    }

    try {
        return await api.post(`${config.analyzeBaseUrl}/goldenUpdateUnified`, data, requestOptions);
    } catch (error) {
        console.log("Failed to update golden \n");
        throw error;
    }
};

const deleteGolden = async (recordingId) => {
    try {
        return await api.post(`${config.recordBaseUrl}/softDelete/${recordingId}`);
    } catch (error) {
        console.log("Error deleting Golden \n", error);
        throw error;
    }
};

const fetchClusterList = async () => {
    try {
        return await api.get("https://www.mocky.io/v2/5ed0786c3500006000ff9c6d");
    } catch (error) {
        console.log("Error fetching cluster list \n", error);
        throw error;
    }
};

// TODO: Refactor the calls below
const fetchAPIFacetData = async (app, startTime, endTime) => {
    const user = JSON.parse(localStorage.getItem('user'));

    let apiFacetURL = `${config.analyzeBaseUrl}/getApiFacets/${user.customer_name}/${app}`;
    
    let searchParams = new URLSearchParams();
    searchParams.set("startDate", startTime);
    searchParams.set("endDate", endTime);

    let url = apiFacetURL + "?" + searchParams.toString();

    try {
        return api.get(url);
    } catch (e) {
        console.error("Error fetching API facet data");
        throw e;
    }
}

const fetchAPITraceData = async (app, startTime, endTime, selectedService, selectedApiPath, selectedInstance) => {
    const user = JSON.parse(localStorage.getItem('user'));

    let apiTraceURL = `${config.analyzeBaseUrl}/getApiTrace/${user.customer_name}/${app}`;
    
    let searchParams = new URLSearchParams();
    searchParams.set("startDate", startTime);
    searchParams.set("endDate", endTime);
    searchParams.set("depth", 2);
    searchParams.set("service", selectedService);
    searchParams.set("apiPath", selectedApiPath);
    searchParams.set("instanceId",selectedInstance);

    let url = apiTraceURL + "?" + searchParams.toString();

    try {
        return api.get(url);
    } catch (e) {
        console.error("Error fetching API Trace data");
        throw e;
    }
}

const fetchAPIEventData = async (app, reqIds, eventTypes=[]) => {
    const user = JSON.parse(localStorage.getItem('user'));

    let apiEventURL = `${config.recordBaseUrl}/getEvents`;

    let body = {
        "customerId":user.customer_name,
        "app": app,
        "eventTypes": eventTypes,
        "services": [],
        "traceIds": [],
        "reqIds": reqIds,
        "paths": [],
        // "limit": 2
    }

    try {
        return api.post(apiEventURL,body);
    } catch (e) {
        console.error("Error fetching API Event data");
        throw e;
    }
}

export const cubeService = {
    fetchAppsList,
    getInstanceList,
    getGraphData,
    getTestConfigByAppId,
    getGraphDataByAppId,
    fetchCollectionList,
    forceCompleteReplay,
    checkStatusForReplay,
    fetchTimelineData,
    getCollectionUpdateOperationSet,
    getNewTemplateVerInfo,
    fetchJiraBugData,
    fetchAnalysisStatus, 
    getTestConfig,
    fetchFacetData,
    removeReplay,
    getProjectList,
    createJiraIssue,
    getResponseTemplate,
    fetchAnalysisResults,
    unifiedGoldenUpdate,
    deleteGolden,
    fetchAPIFacetData,
    fetchAPITraceData,
    fetchAPIEventData,
    fetchClusterList
};
