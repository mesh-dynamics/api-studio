import config from '../config';
import axios from 'axios';


export const cubeService = {
    fetchAppsList,
    getInstanceList,
    getGraphData,
    getTestConfigByAppId,
    getGraphDataByAppId,
    fetchCollectionList,
    forceCompleteReplay,
    checkStatusForReplay,
    fetchAnalysis,
    fetchReport,
    fetchTimelineData,
    getDiffData,
    getCollectionUpdateOperationSet,
    updateRecordingOperationSet,
    updateGoldenSet,
    getNewTemplateVerInfo,
    updateTemplateOperationSet,
};

async function fetchAppsList() {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.apiBaseUrl}/app`;
    let appsList;
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
                "Authorization": "Bearer " + user['access_token']
            })
        });
        if (response.ok) {
            json = await response.json();
            appsList = json;
        } else {
            console.log("Response not ok in fetchAppsList", response);
            throw new Error("Response not ok fetchAppsList");
        }
    } catch (e) {
        console.log("fetchAppsList has errors!", e);
        throw e;
    }
    return appsList;

}

async function updateGoldenSet(replayId, collectionUpdOpSetId, templateVer, recordingId) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/updateGoldenSet/${recordingId}/${replayId}/${collectionUpdOpSetId}/${templateVer}`;
    let updateRes;
    try {
        response = await fetch(url, {
            method: "get",
            headers:{
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            updateRes = json;
        } else {
            console.log("Response not ok in updateRecordingOperationSet", response);
            throw new Error("Response not ok updateRecordingOperationSet");
        }
    } catch (e) {
        console.log("updateRecordingOperationSet has errors!", e);
        throw e;
    }
    return updateRes;
}

async function updateTemplateOperationSet(templateVer, body) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/updateTemplateOperationSet/${templateVer}`;
    let updateRes;
    try {
        response = await fetch(url, {
            method: "post",
            body: JSON.stringify(body),
            headers:{
                "Content-Type": "application/json",
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            updateRes = json;
        } else {
            console.log("Response not ok in updateTemplateOperationSet", response);
            throw new Error("Response not ok updateTemplateOperationSet");
        }
    } catch (e) {
        console.log("updateRecordingOperationSet has errors!", e);
        throw e;
    }
    return updateRes;
}

async function updateRecordingOperationSet(rosData) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/update`;
    let updateRes;
    try {
        response = await fetch(url, {
            method: "post",
            body: JSON.stringify(rosData),
            headers:{
                "Content-Type": "application/json",
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            updateRes = json;
        } else {
            console.log("Response not ok in updateRecordingOperationSet", response);
            throw new Error("Response not ok updateRecordingOperationSet");
        }
    } catch (e) {
        console.log("updateRecordingOperationSet has errors!", e);
        throw e;
    }
    return updateRes ? updateRes : "done";
}

async function getNewTemplateVerInfo(app, currentTemplateVer) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.analyzeBaseUrl}/initTemplateOperationSet/${user.customer_name}/${app}/${currentTemplateVer}`;
    let newTemplateInfo;
    try {
        response = await fetch(url, {
            method: "post",
            headers:{
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            newTemplateInfo = json;
        } else {
            console.log("Response not ok in getNewTemplateVerInfo", response);
            throw new Error("Response not ok getNewTemplateVerInfo");
        }
    } catch (e) {
        console.log("getNewTemplateVerInfo has errors!", e);
        throw e;
    }
    return newTemplateInfo;
}

async function getCollectionUpdateOperationSet(app) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/create?customer=${user.customer_name}&app=${app}`;
    let collectionUpdateOperationSetId;
    try {
        response = await fetch(url, {
            method: "post",
            headers:{
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            collectionUpdateOperationSetId = json;
        } else {
            console.log("Response not ok in getCollectionUpdateOperationSet", response);
            throw new Error("Response not ok getCollectionUpdateOperationSet");
        }
    } catch (e) {
        console.log("getCollectionUpdateOperationSet has errors!", e);
        throw e;
    }
    return collectionUpdateOperationSetId;
}

async function getInstanceList() {
    let user = JSON.parse(localStorage.getItem('user'));
    let pageLocationURL = window.location.href;
    let response, json;
    let url = `${config.apiBaseUrl}/instance`;
    let iList;
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
                "Authorization": "Bearer " + user['access_token']
            })
        });
        if (response.ok) {
            json = await response.json();
            iList = json;
            if (pageLocationURL.indexOf('.prod.v2.') != -1) {
                for (const il of iList) {
                    il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.v2.");
                }
            } else if (pageLocationURL.indexOf('.prod.') != -1) {
                for (const il of iList) {
                    il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.");
                }
            }
        } else {
            console.log("Response not ok in getInstanceList", response);
            throw new Error("Response not ok getInstanceList");
        }
    } catch (e) {
        console.log("getInstanceList has errors!", e);
        throw e;
    }
    return iList;
}

async function getGraphData(app) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.apiBaseUrl}/service_graph`;
    let graphData;
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
                "Authorization": "Bearer " + user['access_token']
            })
        });
        if (response.ok) {
            json = await response.json();
            graphData = json;
        } else {
            console.log("Response not ok in getInstanceList", response);
            throw new Error("Response not ok getInstanceList");
        }
    } catch (e) {
        console.log("getInstanceList has errors!", e);
        throw e;
    }
    return graphData;
}

async function getTestConfigByAppId(appId) {
    let response, json;
    let user = JSON.parse(localStorage.getItem('user'));
    let url = `${config.apiBaseUrl}/app/${appId}/test-configs`;
    let tcList;
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
                "Authorization": "Bearer " + user['access_token']
            })
        });
        if (response.ok) {
            json = await response.json();
            tcList = json;
        } else {
            console.error("Response not ok in getInstanceList", response);
            throw new Error("Response not ok getInstanceList");
        }
    } catch (e) {
        console.error("getInstanceList has errors!", e);
        throw e;
    }
    return tcList;
}

async function getGraphDataByAppId(appId) {
    let response, json;
    let user = JSON.parse(localStorage.getItem('user'));
    let url = `${config.apiBaseUrl}/app/${appId}/service-graphs`;
    let graphData;
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "Content-Type": "application/json",
                "Authorization": "Bearer " + user['access_token']
            })
        });
        if (response.ok) {
            json = await response.json();
            graphData = json;
        } else {
            console.error("Response not ok in getInstanceList", response);
            throw new Error("Response not ok getInstanceList");
        }
    } catch (e) {
        console.error("getInstanceList has errors!", e);
        throw e;
    }
    return graphData;
}

async function getTestIds (options) {
    let response, json;
    try {
        let url = `${config.apiBaseUrl}/getTestIds`;
        let dataLen = JSON.stringify(options).length.toString();
        response = await fetch(url, {
            method: "post",
            body: JSON.stringify(options),
            headers: new Headers({
                "Content-Type": "application/json",
                "Content-Length": dataLen
            })
        });
        if (response.ok) {
            json = await response.json();
            return json;
        } else {
            throw new Error("Response not ok getTestIds");
        }
    } catch (e) {
        throw e;
    }
}

async function fetchCollectionList(app) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.recordBaseUrl}/recordings?customerid=${user.customer_name}&app=${app}`;
    let collections = [];
    try {
        response = await fetch(url, {
            method: "get",
            mode: 'cors',
            headers:{
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            json = await response.json();
            collections = json;
        } else {
            console.log("Response not ok in fetchCollectionList", response);
            throw new Error("Response not ok fetchCollectionList");
        }
    } catch (e) {
        console.log("fetchCollectionList has errors!", e);
        throw e;
    }
    return collections;
}

async function forceCompleteReplay(fcId) {
    let url = `${config.replayBaseUrl}/forcecomplete/${fcId}`;
    await axios.post(url, {}).then(function(response){
        return response;
    }).catch(function(error){
        throw (error.response);
    });
}

async function checkStatusForReplay(collectionId, replayId, app) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let url = `${config.replayBaseUrl}/status/${user.customer_name}/${app}/${collectionId}/${replayId}`;
    let status = {};
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            status = json;
        } else {
            console.log("Response not ok in checkStatusForReplay", response);
            throw new Error("Response not ok checkStatusForReplay");
        }
    } catch (e) {
        console.log("checkStatusForReplay has errors!", e);
        throw e;
    }
    console.log('checkStatusForReplay success: ', JSON.stringify(status, null, 4));
    return status;
}

async function fetchAnalysis(collectionId, replayId) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/analyze/${replayId}`;
    const searchParams = new URLSearchParams();
    searchParams.set('tracefield', 'x-b3-traceid');
    let analysis = {};
    try {
        response = await fetch(url, {
            method: "post",
            body: searchParams,
            headers: new Headers({
                "Content-Type": "application/x-www-form-urlencoded",
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            analysis = json;
        } else {
            throw new Error("Response not ok fetchAnalysis");
        }
    } catch (e) {
        console.log("fetchAnalysis has errors!", e);
        throw e;
    }
    return analysis;
}

async function fetchReport(collectionId, replayId) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/aggrresult/${replayId}?bypath=y`;
    let report = {};
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            report = json;
        } else {
            console.log("Response not ok in fetchReport", response);
            throw new Error("Response not ok fetchReport");
        }
    } catch (e) {
        console.log("fetchReport has errors!", e);
        throw e;
    }
    return report;
}

async function fetchTimelineData(app, userId, endDate) {
    let user = JSON.parse(localStorage.getItem('user'));
    let response, json;
    let ed = endDate.getFullYear() + '-' + (endDate.getMonth() + 1) + '-' + (endDate.getDate() + 1);
    let url = `${config.analyzeBaseUrl}/timelineres/${user.customer_name}/${app}?byPath=y&endDate=${ed}`;
    if (userId !== 'ALL') {
        url = `${config.analyzeBaseUrl}/timelineres/${user.customer_name}/${app}?byPath=y&userid=${user.username}&endDate=${ed}`;
    }
    let timelineData = {};
    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            timelineData = json;
        } else {
            console.log("Response not ok in fetchTimeline", response);
            throw new Error("Response not ok fetchTimeline");
        }
    } catch (e) {
        console.log("fetchTimeline has errors!", e);
        throw e;
    }
    return timelineData;
}

async function getDiffData(replayId, recordReqId, replayReqId) {
    let response, json;
    let url = `${config.analyzeBaseUrl}/analysisResByReq/${replayId}?recordReqId=${recordReqId}&replayReqId=${replayReqId}`;
    let diffData = {};

    try {
        response = await fetch(url, {
            method: "get",
            headers: new Headers({
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            diffData = json;
        } else {
            console.log("Response not ok in diffData", response);
            throw new Error("Response not ok diffData");
        }
    } catch (e) {
        console.log("diffData has errors!", e);
        throw e;
    }
    return diffData;
}
