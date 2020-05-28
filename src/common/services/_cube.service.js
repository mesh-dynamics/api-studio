// fetchAnalysis, // Looks unused
// fetchReport, // Looks unused
// updateGoldenSet,// Looks unused
/**
 * Seemingly Unused functions
 */
// async function fetchAnalysis(collectionId, replayId) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let url = `${config.analyzeBaseUrl}/analyze/${replayId}`;
//     const searchParams = new URLSearchParams();
//     searchParams.set('tracefield', 'x-b3-traceid');
//     let analysis = {};
//     try {
//         response = await fetch(url, {
//             method: "post",
//             body: searchParams,
//             headers: new Headers({
//                 "Content-Type": "application/x-www-form-urlencoded",
//                 "cache-control": "no-cache",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             analysis = json;
//         } else {
//             throw new Error("Response not ok fetchAnalysis");
//         }
//     } catch (e) {
//         console.log("fetchAnalysis has errors!", e);
//         throw e;
//     }
//     return analysis;
// }

// async function fetchReport(collectionId, replayId) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let url = `${config.analyzeBaseUrl}/aggrresult/${replayId}?bypath=y`;
//     let report = {};
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "cache-control": "no-cache",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             report = json;
//         } else {
//             console.log("Response not ok in fetchReport", response);
//             throw new Error("Response not ok fetchReport");
//         }
//     } catch (e) {
//         console.log("fetchReport has errors!", e);
//         throw e;
//     }
//     return report;
// }
// async function getTestIds (options) {
//     let response, json;
//     try {
//         let url = `${config.apiBaseUrl}/getTestIds`;
//         let dataLen = JSON.stringify(options).length.toString();
//         response = await fetch(url, {
//             method: "post",
//             body: JSON.stringify(options),
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Content-Length": dataLen
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             return json;
//         } else {
//             throw new Error("Response not ok getTestIds");
//         }
//     } catch (e) {
//         throw e;
//     }
// }
// async function updateGoldenSet(name, replayId, collectionUpdOpSetId, templateVer, recordingId) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let searchParams = new URLSearchParams();
//     searchParams.set('name', name);
//     searchParams.set('userId', user.username);
//     let url = `${config.analyzeBaseUrl}/updateGoldenSet/${recordingId}/${replayId}/${collectionUpdOpSetId}/${templateVer}`;
//     let updateRes;
//     try {
//         response = await fetch(url, {
//             method: "post",
//             headers:{
//                 'Access-Control-Allow-Origin': '*',
//                 "Content-Type": "application/x-www-form-urlencoded",
//                 "Authorization": "Bearer " + user['access_token']
//             },
//             body: searchParams
//         });
//         if (response.ok) {
//             json = await response.json();
//             updateRes = json;
//         } else {
//             console.log("Response not ok in updateRecordingOperationSet", response);
//             throw new Error("Response not ok updateRecordingOperationSet");
//         }
//     } catch (e) {
//         console.log("updateRecordingOperationSet has errors!", e);
//         throw e;
//     }
//     return updateRes;



/**
 * Migrated Calls
 */
// async function checkStatusForReplay(replayId) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.replayBaseUrl}/status/${replayId}`;
//     let status = {};
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "cache-control": "no-cache",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             status = json;
//         } else {
//             console.log("Response not ok in checkStatusForReplay", response);
//             throw new Error("Response not ok checkStatusForReplay");
//         }
//     } catch (e) {
//         console.log("checkStatusForReplay has errors!", e);
//         throw e;
//     }
//     console.log('checkStatusForReplay success: ', JSON.stringify(status, null, 4));
//     return status;
// }
// async function fetchAppsList() {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.apiBaseUrl}/app`;
//     let appsList;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             appsList = json;
//         } else {
//             console.log("Response not ok in fetchAppsList", response);
//             throw new Error("Response not ok fetchAppsList");
//         }
//     } catch (e) {
//         console.log("fetchAppsList has errors!", e);
//         throw e;
//     }
//     return appsList;

// }
//             "Content-Type": "application/x-www-form-urlencoded",
// const url = `${config.apiBaseUrl}/app`;
    // const requestOptions = {
    //     headers: {
    //         "Authorization": "Bearer " + user['access_token']
    //     }
    // };
    // async function getGraphDataByAppId(appId) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let url = `${config.apiBaseUrl}/app/${appId}/service-graphs`;
//     let graphData;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             graphData = json;
//         } else {
//             console.error("Response not ok in getInstanceList", response);
//             throw new Error("Response not ok getInstanceList");
//         }
//     } catch (e) {
//         console.error("getInstanceList has errors!", e);
//         throw e;
//     }
//     return graphData;
// }
// async function getInstanceList() {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let pageLocationURL = window.location.href;
//     let response, json;
//     let url = `${config.apiBaseUrl}/instance`;
//     let iList;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             iList = json;
//             console.log({ iList });
//             if (pageLocationURL.indexOf('.prod.v2.') != -1) {
//                 for (const il of iList) {
//                     il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.v2.");
//                 }
//             } else if (pageLocationURL.indexOf('.prod.') != -1) {
//                 for (const il of iList) {
//                     il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.");
//                 }
//             }
//         } else {
//             console.log("Response not ok in getInstanceList", response);
//             throw new Error("Response not ok getInstanceList");
//         }
//     } catch (e) {
//         console.log("getInstanceList has errors!", e);
//         throw e;
//     }
//     return iList;
// }
// async function getTestConfigByAppId(appId) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let url = `${config.apiBaseUrl}/app/${appId}/test-configs`;
//     let tcList;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             tcList = json;
//         } else {
//             console.error("Response not ok in getInstanceList", response);
//             throw new Error("Response not ok getInstanceList");
//         }
//     } catch (e) {
//         console.error("getInstanceList has errors!", e);
//         throw e;
//     }
//     return tcList;
// }
// async function getGraphData(app) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.apiBaseUrl}/service_graph`;
//     let graphData;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             graphData = json;
//         } else {
//             console.log("Response not ok in getInstanceList", response);
//             throw new Error("Response not ok getInstanceList");
//         }
//     } catch (e) {
//         console.log("getInstanceList has errors!", e);
//         throw e;
//     }
//     return graphData;
// }

// async function fetchCollectionList(app) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.recordBaseUrl}/searchRecording?customerId=${user.customer_name}&app=${app}`;
//     let collections = [];
//     try {
//         response = await fetch(url, {
//             method: "get",
//             mode: 'cors',
//             headers:{
//                 'Access-Control-Allow-Origin': '*',
//                 "Authorization": "Bearer " + user['access_token']
//             }
//         });
//         if (response.ok) {
//             json = await response.json();
//             collections = json;
//         } else {
//             console.log("Response not ok in fetchCollectionList", response);
//             throw new Error("Response not ok fetchCollectionList");
//         }
//     } catch (e) {
//         console.log("fetchCollectionList has errors!", e);
//         throw e;
//     }
//     return collections;
// }
// async function fetchTimelineData(app, userId, endDate, startDate, numResults, testConfigName, goldenName ) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let ed = endDate.toISOString();

//     let params = new URLSearchParams();
//     params.set("byPath", "y")
//     params.set("endDate", ed)

//     if(startDate) {
//         let sd = startDate.toISOString();
//         params.set("startDate", sd);
//     }

//     if (userId !== 'ALL') {
//         params.set("userId", user.username);
//     }
    
//     if (numResul ts || numResults == 0){
//         params.set("numResults", numResults);
//     }

//     if(testConfigName) {
//         params.set("testConfigName", testConfigName);
//     }
    
//     if(goldenName) {
//         params.set("golden_name", goldenName);
//     }

//     let url = `${config.analyzeBaseUrl}/timelineres/${user.customer_name}/${app}?` + params.toString();

//     let timelineData = {};
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "cache-control": "no-cache",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             timelineData = json;
//         } else {
//             console.log("Response not ok in fetchTimeline", response);
//             throw new Error("Response not ok fetchTimeline");
//         }
//     } catch (e) {
//         console.log("fetchTimeline has errors!", e);
//         throw e;
//     }
//     return timelineData;
// }
// async function getCollectionUpdateOperationSet(app) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.analyzeBaseUrl}/goldenUpdate/recordingOperationSet/create?customer=${user.customer_name}&app=${app}`;
//     let collectionUpdateOperationSetId;
//     try {
//         response = await fetch(url, {
//             method: "post",
//             headers:{
//                 'Access-Control-Allow-Origin': '*',
//                 "Authorization": "Bearer " + user['access_token']
//             }
//         });
//         if (response.ok) {
//             json = await response.json();
//             collectionUpdateOperationSetId = json;
//         } else {
//             console.log("Response not ok in getCollectionUpdateOperationSet", response);
//             throw new Error("Response not ok getCollectionUpdateOperationSet");
//         }
//     } catch (e) {
//         console.log("getCollectionUpdateOperationSet has errors!", e);
//         throw e;
//     }
//     return collectionUpdateOperationSetId;
// }
// async function fetchJiraBugData(replayId, apiPath) {  
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json, data;
//     let url = `${config.apiBaseUrl}/jira/issue/details?replayId=${replayId}&apiPath=${apiPath}`;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             }),
//         });
//         if (response.ok) {
//             data = await response.json();
//         } else {
//             throw new Error("Could not get list of Jira Bugs");
//         }
//     } catch (error) {
//         console.log("Error fetching Jira Bugs", error);
//         throw error;
//     }

//     return data;   
// }
// async function fetchAnalysisStatus(replayId) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.analyzeBaseUrl}/status/${replayId}`;
//     let status = {};
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "cache-control": "no-cache",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             status = json.data;
//         } else {
//             console.log("Response not ok in fetchAnalysisStatus", response);
//             throw new Error("Response not ok fetchAnalysisStatus");
//         }
//     } catch (e) {
//         console.log("fetchAnalysisStatus has errors!", e);
//         throw e;
//     }
//     console.log('fetchAnalysisStatus success: ', JSON.stringify(status, null, 4));
//     return status;
// }

// async function getTestConfig(app, testConfigName) {
//     let response, json;
//     let user = JSON.parse(localStorage.getItem('user'));
//     let url = `${config.apiBaseUrl}/test_config/${user.customer_name}/${app}/${testConfigName}`;
//     let tc;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             })
//         });
//         if (response.ok) {
//             json = await response.json();
//             tc = json;
//         } else {
//             console.error("Response not ok in getTestConfig", response);
//             throw new Error("Response not ok getTestConfig");
//         }
//     } catch (e) {
//         console.error("getTestConfig has errors!", e);
//         throw e;
//     }
//     return tc;
// }
// async function fetchFacetData(replayId) {
//     let analysisResUrl = `${config.analyzeBaseUrl}/analysisResByPath/${replayId}`;
//     let searchParams = new URLSearchParams();
//     searchParams.set("numResults", 0);

//     let url = analysisResUrl + "?" + searchParams.toString();

//     let user = JSON.parse(localStorage.getItem('user'));
//     try {
    
//         let response = await fetch(url, { 
//             headers: { 
//                 "Authorization": "Bearer " + user['access_token']
//             }, 
//             "method": "GET", 
//         });
        
//         if (response.ok) {
//             let dataList = {}
//             let json = await response.json();
//             dataList = json;
//             if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.facets)) {
//                 console.log("facets data is empty")
//             }
//             return dataList;
//         } else {
//             console.error("unable to fetch facet data");
//             throw new Error("unable to fetch facet data");
//         }
//     } catch (e) {
//         console.error("Error fetching facet data");
//         throw e;
//     }
// }
// async function removeReplay(replayId) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response,data;
//     let url = `${config.replayBaseUrl}/softDelete/${replayId}`;
//     try {
//         response = await fetch(url, {
//             method: "post",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             }),
//         });
//         if (response.ok) {
//             data = await response.json();
//         } else {
//             throw new Error("Could not delete the Replay");
//         }

//     } catch (error) {
//         console.log("Error deleting Replay", error);
//         throw error;
//     }
//     return data;
// }
// async function getNewTemplateVerInfo(app, currentTemplateVer) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.analyzeBaseUrl}/initTemplateOperationSet/${user.customer_name}/${app}/${currentTemplateVer}`;
//     let newTemplateInfo;
//     try {
//         response = await fetch(url, {
//             method: "post",
//             headers:{
                
//                 "Authorization": "Bearer " + user['access_token'],
//                 "Content-Type": "application/json"
//             }
//         });
//         if (response.ok) {
//             json = await response.json();
//             newTemplateInfo = json;
//         } else {
//             console.log("Response not ok in getNewTemplateVerInfo", response);
//             throw new Error("Response not ok getNewTemplateVerInfo");
//         }
//     } catch (e) {
//         console.log("getNewTemplateVerInfo has errors!", e);
//         throw e;
//     }
//     return newTemplateInfo;
// }

// async getProjectList() {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.apiBaseUrl}/jira/projects`;
//     let resp;
//     try {
//         response = await fetch(url, {
//             method: "get",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             }),
//         });
//         if (response.ok) {
//             json = await response.json();
//             resp = json;
//         } else {
//             console.log("Response not ok in getProjectList", response);
//             throw new Error("Response not ok getProjectList");
//         }
//     } catch (e) {
//         console.log("getProjectList has errors!", e);
//         throw e;
//     }

//     return resp;
// }
// async createJiraIssue(summary, description, issueTypeId, projectId, replayId, apiPath, requestId, jsonPath) {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let response, json;
//     let url = `${config.apiBaseUrl}/jira/issue/create`;
//     let resp;

//     let reqBody = {
//         summary: summary,
//         description: description,
//         issueTypeId: issueTypeId,
//         projectId: projectId,
//         replayId: replayId,
//         apiPath: apiPath,
//         requestId : requestId,
//         jsonPath: jsonPath,
//     }

//     try {
//         response = await fetch(url, {
//             method: "post",
//             headers: new Headers({
//                 "Content-Type": "application/json",
//                 "Authorization": "Bearer " + user['access_token']
//             }),
//             body: JSON.stringify(reqBody),
//         });
//         json = await response.json();
//         if (response.ok) {
//             resp = json;
//         } else {
//             console.log("Response not ok in createJiraIssue", response);
//             throw new Error("Response not ok createJiraIssue");
//         }
//     } catch (e) {
//         console.log("createJiraIssue has errors! ", e.message);
//         throw new Error(json.message);
//     }

//     return resp;
// }

// async getResponseTemplate() {
//     let user = JSON.parse(localStorage.getItem('user'));
//     let { cube, jsonPath, eventType } = this.props;
//     jsonPath = jsonPath.replace("<BEGIN>", "");
//     let reqOrRespCompare = eventType==="Response" ? "ResponseCompare" : "RequestCompare";

//     let url = `${config.analyzeBaseUrl}/getRespTemplate/${user.customer_name}/${cube.selectedApp}/${cube.pathResultsParams.currentTemplateVer}/${cube.pathResultsParams.service}/${reqOrRespCompare}?apiPath=${cube.pathResultsParams.path}&jsonPath=${jsonPath}`;
    
//     try {
//         const response = await fetch(url, {
//                 method: "get",
//                 headers: new Headers({
//                     "cache-control": "no-cache",
//                     "Authorization": "Bearer " + user['access_token']
//                 })
//             });

//         if (response.ok) {
//             return await response.json();
//         } else {
//             console.log("Response not ok in fetchTimeline", response);
//             throw new Error("Response not ok fetchTimeline");
//         }
//     } catch (e) {
//         console.log("fetchTimeline has errors!", e);
//         throw e;
//     }
// }
            // let url = analysisResUrl + "?" + searchParams.toString();
        // let user = JSON.parse(localStorage.getItem('user'));
        // try {
        //     let response = await fetch(url, { 
        //         headers: { 
        //             "Authorization": "Bearer " + user['access_token']
        //         }, 
        //         "method": "GET", 
        //     });
            
        //     if (response.ok) {
        //         let dataList = {}
        //         let json = await response.json();
        //         dataList = json;
        //         if (_.isEmpty(dataList.data) || _.isEmpty(dataList.data.res)) {
        //             console.log("results list is empty")
        //         } 
        //         return dataList;
        //     } else {
        //         console.error("unable to fetch analysis results");
        //         throw new Error("unable to fetch analysis results");
        //     }
        // } catch (e) {
        //     console.error("Error fetching analysis results list");
        //     throw e;
        // }
        // updateGolden = async () => {
        //     const { cube, dispatch } = this.props;
    
        //     let user = JSON.parse(localStorage.getItem('user'));
    
        //     const headers = {
        //         "Content-Type": "application/json",
        //         'Access-Control-Allow-Origin': '*',
        //         "Authorization": "Bearer " + user['access_token']
        //     };
    
        //     let tagList = [];
        //     if (this.state.tag.trim()) {
        //         tagList = this.state.tag.split(",");
        //         for (let tag of tagList) {
        //             tag = tag.trim();
        //         }
        //     }
            
        //     let data = {
        //         templateOperationSet: {
        //             params: {
        //                 operationSetId: cube.newTemplateVerInfo['ID'],
        //             },
        //             body: cube.templateOperationSetObject,
        //         },
    
        //         updateMultiPath : {
        //             body: cube.multiOperationsSet,
        //         },
    
        //         updateGoldenSet: {
        //             params: {
        //                 recordingId: this.state.recordingId,
        //                 replayId: this.state.replayId,
        //                 collectionUpdOpSetId: cube.collectionUpdateOperationSetId.operationSetId,
        //                 templateUpdOpSetId: cube.newTemplateVerInfo['ID'],
        //             },
                    
        //             body: {
        //                 name: this.state.nameG,
        //                 label: this.state.labelG,
        //                 userId: user.username,
        //                 codeVersion:  this.state.version.trim(),
        //                 branch:  this.state.branch.trim(),
        //                 gitCommitId: this.state.commitId.trim(),
        //                 tags: tagList,
        //             },
        //         }
        //     }
    
        //     axios({
        //         method: 'post',
        //         url: `${config.analyzeBaseUrl}/goldenUpdateUnified`,
        //         data: data,
        //         headers: headers
        //     })
        //     .then((result) => {
        //         this.setState({showSaveGoldenModal: false, saveGoldenError: ""});
        //         dispatch(cubeActions.updateGoldenSet(result.data));
        //         dispatch(cubeActions.getTestIds(this.state.app));
        //     })
        //     .catch((err) => {
        //         dispatch(cubeActions.clearGolden());
        //         this.setState({saveGoldenError: err.response.data["Error"]});
        //     });
            
        //     // needed for showing the updating dialog. (is this a good idea?)
        //     dispatch(cubeActions.updateRecordingOperationSet()); 
        // }

                // let user = JSON.parse(localStorage.getItem('user'));

        // const headers = {
        //     "Content-Type": "application/json",
        //     'Access-Control-Allow-Origin': '*',
        //     "Authorization": "Bearer " + user['access_token']
        // };

                // axios({
        //     method: 'post',
        //     url: `${config.analyzeBaseUrl}/goldenUpdateUnified`,
        //     data: data,
        //     headers: headers
        // })
        // .then((result) => {
        //     this.setState({showSaveGoldenModal: false, saveGoldenError: ""});
        //     dispatch(cubeActions.updateGoldenSet(result.data));
        //     dispatch(cubeActions.getTestIds(this.state.app));
        // })
        // .catch((err) => {
        //     dispatch(cubeActions.clearGolden());
        //     this.setState({saveGoldenError: err.response.data["Error"]});
        // });

            // TODO: Critical refactor this
    // replay = async () => {
    //     const { cube, dispatch, authentication, checkReplayStatus } = this.props;
    //     const { testConfig: { testPaths, testMockServices, testConfigName }} = cube;
    //     const selectedInstances = cube.instances
    //         .filter((item) => item.name == cube.selectedInstance && item.app.name == cube.selectedApp);
    //     cubeActions.clearReplayStatus();
    //     if(!cube.selectedInstance){
    //         alert('select an instance to replay')
    //     } else if (!cube.selectedTestId) {
    //         alert('select golden to replay');
    //     } else if(selectedInstances.length === 0) {
    //         alert('Gateway endpoint is unavailable')
    //     } else {
    //         this.setState({showReplayModal: true});
    //         let user = authentication.user;
    //         let url = `${config.replayBaseUrl}/start/${cube.selectedGolden}`;

    //         const transforms = JSON.stringify(getTransformHeaders(this.state.customHeaders));

    //         const searchParams = new URLSearchParams();
    //         searchParams.set('endPoint', selectedInstances[0].gatewayEndpoint);
    //         searchParams.set('instanceId', cube.selectedInstance);
    //         searchParams.set('templateSetVer', cube.collectionTemplateVersion);
    //         searchParams.set('userId', user.username);
    //         searchParams.set('transforms', transforms);
    //         testMockServices && testMockServices.length != 0 &&
    //              testMockServices.map(testMockService => searchParams.append('mockServices',testMockService))
    //         searchParams.set('testConfigName', testConfigName);
    //         searchParams.set('analyze', true);
    //         // Append Test Paths
    //         // If not specified, it will run all paths
    //         if(testPaths && testPaths.length !== 0) {
    //             testPaths.map(path => searchParams.append("paths", path))
    //         }

    //         const configForHTTP = {
    //             headers: {
    //                 'Content-Type': 'application/x-www-form-urlencoded',
    //                 // "Authorization": "Bearer " + user['access_token']
    //             }
    //         };

    //         // axios.post(url, searchParams, configForHTTP)
    //         api.post(url, searchParams, configForHTTP).then((data) => {
    //             console.log(data);
    //             this.setState({replayId: data});
    //             // check replay status periodically and call analyze at the end; and update timeline
    //             // this method is run in the parent component (Navigation)
    //             checkReplayStatus(this.state.replayId.replayId); 
    //         }).catch((error) => {
    //             if(error.response.data) {
    //                 if (error.response.data['replayId'] !== "None") {
    //                     this.setState({
    //                         fcId: error.response.data['replayId'], 
    //                         fcEnabled: (error.response.data['userId']===user.username), 
    //                         showReplayModal: false
    //                     });
    //                 } else {
    //                     this.setState({showReplayModal: false});
    //                     alert(error.response.data['message']);
    //                 }
    //             } else {
    //                 this.setState({showReplayModal: false});
    //                 alert(error.response.statusText);
    //             }
    //         });
    //     }
    // };

//     async function deleteGolden(recordingId) {
//         let user = JSON.parse(localStorage.getItem('user'));
//         let response,data;
//         let url = `${config.recordBaseUrl}/softDelete/${recordingId}`;
//         try {
//             response = await fetch(url, {
//                 method: "post",
//                 headers: new Headers({
//                     "Content-Type": "application/json",
//                     "Authorization": "Bearer " + user['access_token']
//                 }),
//             });
//             if (response.ok) {
//                 data = await response.json();
//             } else {
//                 throw new Error("Could not delete the Golden");
//             }
    
//         } catch (error) {
//             console.log("Error deleting Golden", error);
//             throw error;
//         }
//         return data;
//     }