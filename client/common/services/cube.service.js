import config from '../config';
import axios from 'axios';

let user = JSON.parse(localStorage.getItem('user'));

export const cubeService = {
    fetchAppsList,
    getGraphData,
    fetchCollectionList,
    getReplayId,
    startReplay,
    checkStatusForReplay,
    fetchAnalysis,
    fetchReport,
    fetchTimelineData,
    getDiffData,
};

async function fetchAppsList() {
    let response, json;
    let url = `${config.baseUrl}/api/app`;
    let appsList = {};
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
            appsList = json.apps;
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

async function getGraphData(app) {
    if (app == 'Cube') {
        return {
            "edges": [
                {
                    "id": "ui_record",
                    "source": 17,
                    "target": 14
                },
                {
                    "id": "ui_replay",
                    "source": 17,
                    "target": 15
                }
            ],
            "nodes": [
                {
                    "data": {
                        "id": 14,
                        "text": "Record"
                    },
                    "style": {
                        "text-wrap": "wrap"
                    }
                },
                {
                    "data": {
                        "id": 15,
                        "text": "Replay"
                    },
                    "style": {
                        "text-wrap": "wrap"
                    }
                },
                {
                    "data": {
                        "id": 16,
                        "text": "Mock"
                    },
                    "style": {
                        "text-wrap": "wrap"
                    }
                },
                {
                    "data": {
                        "id": 17,
                        "text": "UI"
                    },
                    "style": {
                        "text-wrap": "wrap"
                    }
                }
            ]
        };
    }
    return {
        nodes: [
            {
                "data": {
                    "id": "movieinfo",
                    "text": "MovieInfo"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "restwrapjdbc",
                    "text": "Rentals"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "details",
                    "text": "Details"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "ratings",
                    "text": "Ratings"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            },
            {
                "data": {
                    "id": "reviews",
                    "text": "Reviews"
                },
                "style": {
                    "text-wrap": "wrap",
                }
            }
        ],
        edges: [
            {
                id: 's1_s2',
                source: 'movieinfo',
                target: 'restwrapjdbc'
            },
            {
                id: 's1_s3',
                source: 'movieinfo',
                target: 'details'
            },
            {
                id: 's1_s4',
                source: 'movieinfo',
                target: 'ratings'
            },
            {
                id: 's1_s5',
                source: 'movieinfo',
                target: 'reviews'
            }
        ]
    };
}

async function getTestIds (options) {
    let response, json;
    try {
        let url = `${config.apiUrl}/api/getTestIds`;
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
    let response, json;
    let url = `${config.baseUrl}/cs/recordings?customerid=${user.username}&app=${app}`;
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

async function getReplayId(collectionId, app) {
    let response, json;
    let url = `${config.baseUrl}/rs/init/${user.username}/${app}/${collectionId}`;
    let replayId;
    const searchParams = new URLSearchParams();
    const ep = app == 'Cube' ? 'http://staging.cubecorp.io' : 'http://dogfooding.cubecorp.io';
    searchParams.set('endpoint', ep);
    searchParams.set('instanceid', 'prod');
    if (app != 'Cube') {
        searchParams.set('paths', 'minfo/listmovies');
        searchParams.append('paths', 'minfo/returnmovie');
        searchParams.append('paths', 'minfo/rentmovie');
        searchParams.append('paths', 'minfo/liststores');
    }
    try {
        let urrl = url;
        response = await fetch(urrl, {
            method: "post",
            body: searchParams,
            headers: new Headers({
                "Content-Type": "application/x-www-form-urlencoded",
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            return json;
        } else {
            throw new Error("Response not ok getReplayId");
        }
    } catch (e) {
        throw e;
    }
}

async function startReplay(collectionId, replayId, app) {
    let response, json;
    let url = `${config.baseUrl}/rs/start/${user.username}/${app}/${collectionId}/${replayId}`;

    try {
        let urrl = url;
        response = await fetch(urrl, {
            method: "post",
            headers: new Headers({
                "cache-control": "no-cache"
            })
        });
        if (response.ok) {
            json = await response.json();
            return json;
        } else {
            throw new Error("Response not ok startReplay");
        }
    } catch (e) {
        throw e;
    }
}

async function checkStatusForReplay(collectionId, replayId, app) {
    let response, json;
    let url = `${config.baseUrl}/rs/status/${user.username}/${app}/${collectionId}/${replayId}`;
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
    let url = `${config.baseUrl}/as/analyze/${replayId}`;
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
    let url = `${config.baseUrl}/as/aggrresult/${replayId}?bypath=y`;
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

async function fetchTimelineData(collectionId, replayData) {
    let response, json;
    let url = `${config.baseUrl}/as/timelineres/${replayData.customerid}/${replayData.app}/${replayData.instanceid}?collectionId=${collectionId}`;
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
    let url = `${config.baseUrl}/as/analysisResByReq/${replayId}?recordReqId=${recordReqId}&replayReqId=${replayReqId}`;
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
