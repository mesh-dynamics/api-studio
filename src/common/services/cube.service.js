import config from '../config';
import api from '../api';
import _ from 'lodash';
import { getDefaultTraceApiFilters } from "../utils/api-catalog/api-catalog-utils";
import arrayToTree from "array-to-tree";

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

const createUserCollection = async(collectionName, app) => {
    const user = JSON.parse(localStorage.getItem('user')); 
    const userId = user.username;
      const searchParams = new URLSearchParams();

      searchParams.set("name", collectionName);
      searchParams.set("userId", userId);
      searchParams.set("label", userId);
      searchParams.set("recordingType", "UserGolden");

      const configForHTTP = {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      };

    return api
    .post(
      `${config.apiBaseUrl}/cs/start/${user.customer_name}/${app}/dev/Default${app}`,
      searchParams,
      configForHTTP
    );
};

const storeUserReqResponse = async(recordingId, data, apiConfig={}) => {
    const urlToPost = `${config.apiBaseUrl}/cs/storeUserReqResp/${recordingId}`;
    return api.post(urlToPost, data, apiConfig);
}

const fetchCollectionList = async (app, recordingType="", forCurrentUser=false, numResults = 0, start = 0) => {
    const user = JSON.parse(localStorage.getItem('user')); // TODO: Change this to be passed from auth tree
    try {
        let url = `${config.recordBaseUrl}/searchRecording`;
        const params = new URLSearchParams();
        params.set("customerId", user.customer_name);
        params.set("app", app);
        params.set("archived", false);
        
        recordingType && params.set("recordingType", recordingType); // todo
        forCurrentUser && params.set("userId", user.username);
        numResults && params.set("numResults", numResults);
        start && params.set("start", start);
        
        return await api.get(url + "?" + params.toString());
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
    const url = `${config.analyzeBaseUrl}/getTemplate/${user.customer_name}/${selectedApp}/${currentTemplateVer}/${service}/${reqOrRespCompare}?apiPath=${path}&jsonPath=${jsonPath}`;

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
const deleteEventByRequestId = async (requestId) => {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        let body = {
            "customerId":user.customer_name
        }
        return await api.post(`${config.recordBaseUrl}/deleteEventByReqId/${requestId}`, body);
    } catch (error) {
        console.log("Error deleting Collection request \n", error);
        throw error;
    }
};

const deleteEventByTraceId = async (traceId, collectionId) => {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        let body = {
            "customerId":user.customer_name,
            "collection": collectionId
        }
        return await api.post(`${config.recordBaseUrl}/deleteEventByTraceId/${traceId}`, body);
    } catch (error) {
        console.log("Error deleting Collection request \n", error);
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

const fetchAPIFacetData = async (app, recordingType, collectionName, startTime=null, endTime=null) => {
    const user = JSON.parse(localStorage.getItem('user'));

    let apiFacetURL = `${config.analyzeBaseUrl}/getApiFacets/${user.customer_name}/${app}`;
    
    let searchParams = new URLSearchParams();
    startTime && searchParams.set("startDate", startTime);
    endTime && searchParams.set("endDate", endTime);
    recordingType && searchParams.set("recordingType", recordingType); // todo
    collectionName && searchParams.set("collection", collectionName);

    let url = apiFacetURL + "?" + searchParams.toString();

    try {
        return api.get(url);
    } catch (e) {
        console.error("Error fetching API facet data");
        throw e;
    }
}

const fetchAPITraceData = async (traceApiFiltersProps) => {
    const {app, startTime, endTime, service, apiPath, instance, recordingType, collectionName, depth, numResults} = traceApiFiltersProps;
    const user = JSON.parse(localStorage.getItem('user'));

    let apiTraceURL = `${config.analyzeBaseUrl}/getApiTrace/${user.customer_name}/${app}`;
    
    let searchParams = new URLSearchParams();
    startTime && searchParams.set("startDate", startTime);
    endTime && searchParams.set("endDate", endTime);
    searchParams.set("depth", depth);
    service && searchParams.set("service", service);
    apiPath && searchParams.set("apiPath", apiPath);
    instance && searchParams.set("instanceId", instance);
    recordingType && searchParams.set("recordingType", recordingType); // todo
    collectionName && searchParams.set("collection", collectionName);
    numResults && searchParams.set('numResults', numResults);
    
    let url = apiTraceURL + "?" + searchParams.toString();

    try {
        return api.get(url);
    } catch (e) {
        console.error("Error fetching API Trace data");
        throw e;
    }
}

const loadCollectionTraces = async(selectedCollectionId, app)=> {
        const filterData = {
          ...getDefaultTraceApiFilters(),
          app,
          collectionName: selectedCollectionId,
          depth: 100,
          numResults: 100,
        };
        const res = await fetchAPITraceData(filterData);
        
        const apiTraces = [];
        res.response.sort((a, b) => {
            return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
        });
        res.response.map((eachApiTrace) => {
            eachApiTrace.res.map((eachApiTraceEvent) => {
            eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
            eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
            eachApiTraceEvent["toggled"] = false;
            eachApiTraceEvent["recordingIdAddedFromClient"] =
            selectedCollectionId;
            eachApiTraceEvent["traceIdAddedFromClient"] =
                eachApiTrace.traceId;
            eachApiTraceEvent["collectionIdAddedFromClient"] =
                eachApiTrace.collection;
            });
            const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
            customID: "spanId",
            parentProperty: "parentSpanId",
            });
            apiTraces.push({
            ...apiFlatArrayToTree[0],
            });
        });

        return apiTraces;
}

const fetchAPIEventData = async (app, reqIds, eventTypes=[], apiConfig={}) => {
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
        return api.post(apiEventURL,body, apiConfig);
    } catch (e) {
        console.error("Error fetching API Event data");
        throw e;
    }
}

const fetchAgentConfigs = async (app) => {
    const user = JSON.parse(localStorage.getItem('user')); 
    try {
        return await api.get(`${config.recordBaseUrl}/fetchAgentConfigWithFacets/${user.customer_name}/${app}`);
    } catch(error) {
        console.log("Error Fetching agent configs \n", error);
        throw new Error("Error Fetching agent configs");
    }
}

const updateAgentConfig = async (updatedConfig) => {
    try {
        return await api.post(`${config.recordBaseUrl}/storeAgentConfig`, updatedConfig);
    } catch (error) {
        console.log("Error updating config\n", error);
        throw error;
    }
};

const getAllEnvironments = async () => {
    try {
        return await api.get(`${config.apiBaseUrl}/dtEnvironment/getAll`);
    } catch (e) {
        console.error("Error fetching environments")
        throw e;
    }
}

const insertNewEnvironment = async (environment) => {
    try {
        let url = `${config.apiBaseUrl}/dtEnvironment/insert`
        return await api.post(url, environment);
    } catch (e) {
        console.error("Error inserting environment")
        throw e;
    }
}

const updateEnvironment = async (environment) => {
    try {
        let url = `${config.apiBaseUrl}/dtEnvironment/update/${environment.id}`
        return await api.post(url, environment);
    } catch (e) {
        console.error("Error updating environment")
        throw e;
    }
}

const deleteEnvironment = async (id) => {
    try {
        let url = `${config.apiBaseUrl}/dtEnvironment/delete/${id}`
        return await api.post(url);
    } catch (e) {
        console.error("Error deleting environment")
        throw e;
    }
}

// mock config
const getAllMockConfigs = async (selectedApp) => {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.apiBaseUrl}/config/get`
        let params = new URLSearchParams()
        params.set("customer", user.customer_name)
        params.set("app", selectedApp)
        params.set("configType", "mockConfig")
        return await api.get(url + "?" + params.toString());
    } catch (e) {
        console.error("Error fetching mock configs")
        throw e;
    }
}

const insertNewMockConfig = async (selectedApp, mockConfig) => {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.apiBaseUrl}/config/insert`
        
        let body = {
            customer: user.customer_name,
            app: selectedApp,
            configType: "mockConfig",
            key: mockConfig.name,
            value: JSON.stringify(mockConfig)
        }

        return await api.post(url, body);
    } catch (e) {
        console.error("Error inserting mock config")
        throw e;
    }
}

const updateMockConfig = async (selectedApp, id, mockConfig) => {
    try {
        const user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.apiBaseUrl}/config/update/${id}`
        
        let body = {
            customer: user.customer_name,
            app: selectedApp,
            configType: "mockConfig",
            key: mockConfig.name,
            value: JSON.stringify(mockConfig)
        }

        return await api.post(url, body);
    } catch (e) {
        console.error("Error updating mock config")
        throw e;
    }
}

const deleteMockConfig = async (id) => {
    try {
        let url = `${config.apiBaseUrl}/config/delete/${id}`
        return await api.post(url);
    } catch (e) {
        console.error("Error deleting mock config")
        throw e;
    }
}

const forceStopRecording = async (recordingId) => {
    try {
        let url = `${config.recordBaseUrl}/forcestop/${recordingId}`
        return await api.post(url);
    } catch (e) {
        console.error("Error force stopping recording")
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
    fetchClusterList,
    fetchAgentConfigs,
    updateAgentConfig,
    getAllEnvironments,
    insertNewEnvironment,
    updateEnvironment,
    deleteEnvironment,
    deleteEventByRequestId, 
    deleteEventByTraceId,
    getAllMockConfigs,
    insertNewMockConfig,
    updateMockConfig,
    deleteMockConfig,
    forceStopRecording,
    loadCollectionTraces,
    createUserCollection,
    storeUserReqResponse
};
