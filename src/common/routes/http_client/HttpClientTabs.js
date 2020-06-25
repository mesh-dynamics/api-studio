import  React , { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Glyphicon, Radio, Checkbox, Tabs, Tab, Panel, Label, Modal, Button, ControlLabel } from 'react-bootstrap';
import {Treebeard, decorators} from 'react-treebeard';

import _ from 'lodash';
import { v4 as uuidv4 } from 'uuid';
import { stringify } from 'query-string';
import arrayToTree  from 'array-to-tree';
import * as moment from 'moment';

import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";
import { cubeService } from "../../services";
import api from '../../api';
import config from '../../config';

import HttpClient from "./HttpClient";
import ResponsiveTabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";
import CollectionTreeCSS from "./CollectionTreeCSS";

class HttpClientTabs extends Component {

    constructor(props) {
        super(props);
        const tabId = uuidv4();
        const urlParameters = new URLSearchParams(window.location.search);
        const selectedApp = urlParameters.get("app");
        this.state = { 
            tabs: [{ 
                id: tabId,
                requestId: "",
                tabName: "",
                httpMethod: "get",
                httpURL: "http://www.mocky.io/v2/5ed952b7310000f4dec4ed0a",
                headers: [],
                queryStringParams: [],
                bodyType: "formData",
                formData: [],
                rawData: "",
                rawDataType: "json",
                responseStatus: "NA",
                responseStatusText: "",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                responseBodyType: "json",
                outgoingRequestIds: [],
                eventData: null,
                showOutgoingRequestsBtn: false
            }],
            outgoingRequests: [],
            toggleTestAndOutgoingRequests: true,
            selectedTabKey: tabId,
            app: selectedApp,
            historyCursor: null,
            userApiTraceHistory: [],
            cubeRunHistory: {},
            userCollections: [],
            userCollectionId: "",
            userHistoryCollection: null,
            showSaveModal: false,
            selectedSaveableTabId: "",
            collectionName: "",
            collectionLabel: ""
        };
        this.addTab = this.addTab.bind(this);
        this.handleTabChange = this.handleTabChange.bind(this);
        this.handleRemoveTab = this.handleRemoveTab.bind(this);

        this.addOrRemoveParam = this.addOrRemoveParam.bind(this);
        this.updateParam = this.updateParam.bind(this);
        this.updateBodyOrRawDataType = this.updateBodyOrRawDataType.bind(this);

        this.driveRequest = this.driveRequest.bind(this);
        this.showOutgoingRequests = this.showOutgoingRequests.bind(this);
        this.saveToCollection = this.saveToCollection.bind(this);

        this.handleClick = this.handleClick.bind(this);

        this.onToggle = this.onToggle.bind(this);
        this.handlePanelClick = this.handlePanelClick.bind(this);

        
        this.handleCloseModal = this.handleCloseModal.bind(this);
        this.showSaveModal = this.showSaveModal.bind(this);
        this.handleSave = this.handleSave.bind(this);

        this.handleChange = this.handleChange.bind(this);
        this.handleCreateCollection = this.handleCreateCollection.bind(this);
    }

    handleCloseModal() {
        this.setState({ showSaveModal: false });
    }
    
    showSaveModal(tabId) {
        this.setState({ showSaveModal: true, collectionName: "", collectionLabel: "", selectedSaveableTabId: tabId});
    }

    onToggle(node, toggled){
        const {historyCursor} = this.state;
        
        if (historyCursor) {
            this.setState(() => ({historyCursor, active: false}));
            /* if (!_.includes(cursor.children, node)) {
                cursor.toggled = false;
                cursor.active = false;
            } */
        }
        node.active = true;
        if (node.children) { 
            node.toggled = toggled; 
        }
        this.setState(() => ({historyCursor: node}));
    }

    getTabIndexGivenTabId (tabId) {
        const { tabs } = this.state;
        let filteredTabs = tabs.filter((e) => e.id === tabId);
        for(let i = 0; i < tabs.length; i++) {
            if(tabs[i].id === tabId){
                return i;
            }
        }
        return -1;
    }

    addOrRemoveParam(tabId, type, op, id) {
        let tabIndex = this.getTabIndexGivenTabId(tabId);
        if(tabIndex < 0) return;
        if(op === "delete") {
            this.setState({
                tabs: this.state.tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab[type] = eachTab[type].filter((e) => e.id !== id);
                    }
                    return eachTab; 
                })
            });
        } else {
            this.setState({
                tabs: this.state.tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab[type] = [...eachTab[type], {
                            id: uuidv4(),
                            name: "",
                            value: "",
                            description: ""
                        }];
                    }
                    return eachTab; 
                })
            });
        }
    }

    updateParam(tabId, type, key, value, id) {
        let tabIndex = this.getTabIndexGivenTabId(tabId);
        if(tabIndex < 0) return;
        let params = this.state.tabs[tabIndex][type];
        if(_.isArray(params)) {
            let specificParamArr = params.filter((e) => e.id === id);
            if(specificParamArr.length > 0) {
                specificParamArr[0][key] = value;
            }
        } else {
            params = value;
        }
        this.setState({
            tabs: this.state.tabs.map(eachTab => {
                if (eachTab.id === tabId) {
                    eachTab[type] = params;
                    if(type === "httpURL") eachTab.tabName = params;
                }
                return eachTab; 
            })
        });
        //this.setState({[type]: params})
    }

    updateBodyOrRawDataType(tabId, type, value) {
        let tabIndex = this.getTabIndexGivenTabId(tabId);
        if(tabIndex < 0) return;
        // this.setState({[type]: value});
        this.setState({
            tabs: this.state.tabs.map(eachTab => {
                if (eachTab.id === tabId) {
                    eachTab[type] = value;
                }
                return eachTab; 
            })
        });
    }

    showOutgoingRequests(tabId, reqId) {    
        const tabIndex = this.getTabIndexGivenTabId(tabId), { tabs, app } = this.state;
        const reqIdArray = tabs[tabIndex]["outgoingRequestIds"];
        if(reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(app, reqIdArray, eventTypes).then((result) => {
                if(result && result.numResults > 0) {
                    let outgoingRequests = [];
                    for(let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if(reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            let headers = [], queryParams = [], formData = [];
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: ""
                                });
                            }
                            for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam].join(","),
                                    description: ""
                                });
                            }
                            for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: ""
                                });
                            }
                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: "formData",
                                formData: formData,
                                rawData: "",
                                rawDataType: "json",
                                responseStatus: "NA",
                                responseStatusText: "NA",
                                responseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4): "",
                                responseBody: httpResponseEvent ?  httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                recordedResponseHeaders: "",
                                recordedResponseBody: "",
                                responseBodyType: "json",
                                showOutgoingRequestsBtn: false
                            };
                            const tabId = uuidv4();
                            outgoingRequests.push({
                                id: tabId,
                                requestId: eachReqId,
                                eventData: reqResPair,
                                tabName: reqObject.httpURL ? reqObject.httpURL : "New",
                                ...reqObject
                            })
                        }
                    }
                    this.setState({
                        toggleTestAndOutgoingRequests: !this.state.toggleTestAndOutgoingRequests,
                        outgoingRequests
                    });
                }
            });
        }
    }

    handleClick() {
        this.setState({
            toggleTestAndOutgoingRequests: !this.state.toggleTestAndOutgoingRequests
        });
    }

    extractHeaders(httpReqestHeaders) {
        let headers = new Headers();
        headers.delete('Content-Type');
        httpReqestHeaders.forEach(each => {
            if(each.name && each.value && each.name.indexOf(":") < 0 && each.name.indexOf("x-") < 0) headers.append(each.name, each.value);
        })
        return headers;
    }

    extractBody(httpRequestBody) {
        let formData = new FormData();
        if(_.isArray(httpRequestBody)) {
            httpRequestBody.forEach(each => {
                if(each.name && each.value) formData.append(each.name, each.value);
            })
            return formData;
        } else {
            return httpRequestBody;
        }
    }

    extractQueryStringParams(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams.forEach(each => {
            if(each.name && each.value) qsParams[each.name] = each.value;
        })
        return qsParams;
    }

    driveRequest(tabId) {
        let tabIndex = this.getTabIndexGivenTabId(tabId);
        if(tabIndex < 0) return;
        // make the request and update response status, headers & body
        // extract headers
        // extract body
        const { headers, queryStringParams, bodyType, rawDataType } = this.state.tabs[tabIndex];
        const httpReqestHeaders = this.extractHeaders(headers);

        const httpRequestQueryStringParams = this.extractQueryStringParams(queryStringParams);
        let httpRequestBody;
        if(bodyType === "formData") {
            const { formData } = this.state.tabs[tabIndex];
            httpRequestBody = this.extractBody(formData);
        }
        if(bodyType === "rawData") {
            const { rawData } = this.state.tabs[tabIndex];
            httpRequestBody = this.extractBody(rawData);
        }
        const httpMethod = this.state.tabs[tabIndex].httpMethod;
        const httpRequestURL = this.state.tabs[tabIndex].httpURL;

        let fetchConfig = {
            method: httpMethod,
            headers: httpReqestHeaders
        }
        if(httpMethod !== "GET".toLowerCase() && httpMethod !== "HEAD".toLowerCase()) {
            fetchConfig["body"] = httpRequestBody;
        }
        let fetchURL = httpRequestURL + (httpRequestQueryStringParams ? "?" + stringify(httpRequestQueryStringParams) : "");
        this.setState({
            tabs: this.state.tabs.map(eachTab => {
                if (eachTab.id === tabId) {
                    eachTab["responseStatus"] = "WAITING...";
                }
                return eachTab; 
            })
        });
        // Make request
        // https://www.mocky.io/v2/5185415ba171ea3a00704eed
        let fetchedResponseHeaders = {}, responseStatus = "", responseStatusText = "";
        const {userHistoryCollection} = this.state;
        return fetch(fetchURL, fetchConfig).then((response) => {
            responseStatus = response.status;
            responseStatusText = response.statusText;
            for(const header of response.headers){
                fetchedResponseHeaders[header[0]] = header[1];
            }
            if (response.headers.get("content-type").indexOf("application/json") !== -1) {// checking response header
                return response.json();
            } else {
                return response.text();
                //throw new TypeError('Response from has unexpected "content-type"');
            }
        })
        .then((data) => {
            // handle success
            this.setState({
                tabs: this.state.tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab["responseHeaders"] = JSON.stringify(fetchedResponseHeaders, undefined, 4);
                        eachTab["responseBody"] = JSON.stringify(data, undefined, 4);
                        eachTab["responseStatus"] = responseStatus;
                        eachTab["responseStatusText"] = responseStatusText;
                    }
                    return eachTab; 
                })
            }, () => {
                this.saveToCollection(tabId, userHistoryCollection.id);
            });
        })
        .catch((error) => {
            console.error(error);
            this.setState({
                tabs: this.state.tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab["responseStatus"] = error.message;
                    }
                    return eachTab; 
                })
            });
        }, () => {
            this.saveToCollection(tabId, userHistoryCollection.id);
        });
    }

    handleTabChange(tabKey) {
        this.setState({
            selectedTabKey: tabKey
        });
    }

    handleRemoveTab(key, evt) {
        evt.stopPropagation();
    
        // current tabs
        const currentTabs = this.state.tabs;
    
        // find index to remove
        const indexToRemove = currentTabs.findIndex(tab => tab.id === key);
    
        // create a new array without [indexToRemove] item
        const newTabs = [...currentTabs.slice(0, indexToRemove), ...currentTabs.slice(indexToRemove + 1)];
    
        const nextSelectedIndex = newTabs[indexToRemove] ? indexToRemove : indexToRemove - 1;
        if (!newTabs[nextSelectedIndex]) {
          alert('You can not delete the last tab!');
          return;
        }
    
        this.setState({ tabs: newTabs, selectedTabKey: newTabs[nextSelectedIndex].id });
    }

    extractHeadersToCubeFormat(headersReceived) {
        let headers = {};
        if(_.isArray(headersReceived)) {
            headersReceived.forEach(each => {
                if(each.name && each.value) headers[each.name] = each.value.split(",");
            });
        } else if(_.isObject(headersReceived)) {
            Object.keys(headersReceived).map((eachHeader) => {
                if(eachHeader && headersReceived[eachHeader]) headers[eachHeader] = headersReceived[eachHeader].split(",");
            })
        }
        
        return headers;
    }

    extractQueryStringParamsToCubeFormat(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams.forEach(each => {
            if(each.name && each.value) qsParams[each.name] = each.value.split(",");
        })
        return qsParams;
    }

    extractBodyToCubeFormat(httpRequestBody) {
        let formData = {};
        if(_.isArray(httpRequestBody)) {
            httpRequestBody.forEach(each => {
                if(each.name && each.value) formData[each.name] = each.value.split(",");
            })
            return formData;
        } else {
            return httpRequestBody;
        }
    }

    saveToCollection(tabId, recordingId) {
        const tabToSave = this.state.tabs.find(eachTab => eachTab.id === tabId);
        if(!tabToSave.eventData) return;
        const reqResPair = tabToSave.eventData;
        if(reqResPair.length > 0) {
            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];

            const { headers, queryStringParams, bodyType, rawDataType, responseHeaders,responseBody } = tabToSave;
            const httpReqestHeaders = this.extractHeadersToCubeFormat(headers);
            const httpRequestQueryStringParams = this.extractQueryStringParamsToCubeFormat(queryStringParams);
            let httpRequestBody;
            if(bodyType === "formData") {
                const { formData } = tabToSave;
                httpRequestBody = this.extractBodyToCubeFormat(formData);
            }
            if(bodyType === "rawData") {
                const { rawData } = tabToSave;
                httpRequestBody = this.extractBodyToCubeFormat(rawData);
            }
            const httpMethod = tabToSave.httpMethod;
            const httpRequestURL = tabToSave.httpURL;
            const user = JSON.parse(localStorage.getItem('user'));
            const {app, collectionName, collectionLabel} = this.state;
            const userId = encodeURIComponent(user.username),
            customerId = encodeURIComponent(user.customer_name);
            const apiPath = httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : ""; 
            const httpResponseHeaders = responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : null;
            const data = [
                {
                    request: {
                        ...httpRequestEvent,
                        payload: [
                            "HTTPRequestPayload",
                            {
                                hdrs: httpReqestHeaders,
                                queryParams: httpRequestQueryStringParams,
                                formParams: httpRequestBody,
                                method: httpMethod.toUpperCase(),
                                path: apiPath,
                                pathSegments: apiPath.split("/")
                            }
                        ]
                    },
                    response: {
                        ...httpResponseEvent,
                        payload: [
                            "HTTPRequestPayload",
                            {
                                hdrs: httpResponseHeaders,
                                body: responseBody ? JSON.parse(responseBody) : null,
                                status: tabToSave.responseStatus,
                                statusCode: tabToSave.responseStatus
                            }
                        ] 
                    }
                }
            ];

            try {
                api.post(`${config.apiBaseUrl}/cs/storeUserReqResp/${recordingId}`, data)
                    .then((serverRes) => {
                        setTimeout(() => {
                            this.loadFromHistory();
                            this.loadUserCollections();
                        }, 2000);
                    }, (error) => {
                        console.log("error: ", error);
                    })
            } catch(error) {
                console.log("Error ", error);
                throw new Error("Error");
            }
        }
    }

    handleChange(evt) {
        this.setState({
            [evt.target.name]: evt.target.value
        })
    }

    handleCreateCollection() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {app, collectionName, collectionLabel} = this.state;
        const userId = user.username,
        customerId = user.customer_name;
        const searchParams = new URLSearchParams();

        searchParams.set('name', collectionName);
        searchParams.set('userId', userId);
        searchParams.set('label', collectionLabel);
        searchParams.set('recordingType', "UserGolden");

        const configForHTTP = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        };

        try {
            api.post(`${config.apiBaseUrl}/cs/start/${user.customer_name}/${app}/dev/Default${app}`, searchParams, configForHTTP)
                .then((serverRes) => {
                    this.loadUserCollections();
                }, (error) => {
                    console.log("error: ", error);
                })
        } catch(error) {
            console.log("Error ", error);
            throw new Error("Error");
        }
    }

    handleSave() {
        const { userCollectionId, userCollections, selectedSaveableTabId } = this.state;
        const selectedCollection = userCollections.find((eachCollection) => {
            return eachCollection.collec = userCollectionId;
        });
        this.saveToCollection(selectedSaveableTabId, selectedCollection.id);
    }

    loadUserCollections() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {app} = this.state;
        const userId = encodeURIComponent(user.username),
        customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=UserGolden`)
                .then((serverRes) => {
                    const userCollections = serverRes.filter((eachCollection) => {
                        return eachCollection.recordingType !== "History"
                    });
                    this.setState({
                        userCollections: userCollections
                    });
                }, (error) => {
                    console.log("error: ", error);
                })
        } catch(error) {
            console.log("Error ", error);
            throw new Error("Error");
        }
    }

    loadFromHistory() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {app} = this.state;
        const userId = encodeURIComponent(user.username),
            customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=History`)
                .then((serverRes) => {
                    const { userHistoryCollection } = this.state;
                    const fetchedUserHistoryCollection = serverRes.find((eachCollection) => {
                        return eachCollection.recordingType === "History";
                    });
                    if(!userHistoryCollection && fetchedUserHistoryCollection) {
                        this.setState({
                            userHistoryCollection: fetchedUserHistoryCollection
                        });
                    }
                    api.get(`${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&recordingType=History&collection=${fetchedUserHistoryCollection.collec}`)
                        .then((res) => {
                            const apiTraces = res.response;
                            const cubeRunHistory = {};
                            apiTraces.sort((a, b) => {
                                return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
                            });
                            apiTraces.forEach((eachApiTrace) => {
                                const timeStamp = eachApiTrace.res[0].reqTimestamp,
                                    objectKey = new Date(timeStamp * 1000).toDateString();
                                eachApiTrace.res.map((eachApiTraceEvent) => {
                                    eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                                    eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                                    eachApiTraceEvent["toggled"] = true;
                                });
                                
                                if(objectKey in cubeRunHistory) {
                                    const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                                        customID: "spanId", parentProperty: "parentSpanId"
                                    });
                                    cubeRunHistory[objectKey].push({
                                        ...apiFlatArrayToTree[0]
                                    });
                                } else {
                                    cubeRunHistory[objectKey] = [];
                                    const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                                        customID: "spanId", parentProperty: "parentSpanId"
                                    });
                                    cubeRunHistory[objectKey].push({
                                        ...apiFlatArrayToTree[0]
                                    });
                                }
                            });
                            this.setState({
                                userApiTraceHistory: apiTraces,
                                cubeRunHistory
                            });
                        }, (err) => {
                            console.log("err: ", err);
                        })
                }, (error) => {
                    console.log("error: ", error);
                })
        } catch(error) {
            console.log("Error ", error);
            throw new Error("Error");
        }
    }

    handlePanelClick(selectedCollectionId) {
        if(!selectedCollectionId) return;
        const user = JSON.parse(localStorage.getItem('user'));
        const {app, userCollections} = this.state;
        const customerId = encodeURIComponent(user.customer_name);
        const selectedCollection = userCollections.find(eachCollection => eachCollection.collec === selectedCollectionId);
        const apiTracesForACollection = selectedCollection.apiTraces;
        try {
            if(!apiTracesForACollection) {
                api.get(`${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${selectedCollectionId}`)
                .then((res) => {
                    const apiTraces = [];

                    res.response.map(eachApiTrace => {
                        eachApiTrace.res.map((eachApiTraceEvent) => {
                            eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                            eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                            eachApiTraceEvent["toggled"] = false;
                        });
                        const apiFlatArrayToTree = arrayToTree(eachApiTrace.res, {
                            customID: "spanId", parentProperty: "parentSpanId"
                        });
                        apiTraces.push({
                            ...apiFlatArrayToTree[0]
                        })
                    });

                    selectedCollection.apiTraces = apiTraces;
                    this.setState({
                        userCollections: userCollections
                    });
                }, (err) => {
                    console.log("err: ", err);
                });
            }
            
        } catch(error) {
            console.log("Error ", error);
            throw new Error("Error");
        }
    }

    addTab(evt, reqObject, givenApp) {
        const tabId = uuidv4();
        const requestId = uuidv4();
        const { app } = this.state;
        const appAvailable =  givenApp ? givenApp : app ? app : "";
        if(!reqObject) {
            reqObject = {
                httpMethod: "get",
                httpURL: "",
                headers: [],
                queryStringParams: [],
                bodyType: "formData",
                formData: [],
                rawData: "",
                rawDataType: "json",
                responseStatus: "NA",
                responseStatusText: "NA",
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                responseBodyType: "",
                requestId: "",
                outgoingRequestIds: [],
                eventData: null
            };
        }
        this.setState({
            tabs: [...this.state["tabs"], {
                id: tabId,
                tabName: reqObject.httpURL ? reqObject.httpURL : "New",
                ...reqObject
            }],
            selectedTabKey: tabId,
            app: appAvailable
        });
    }

    getRequestIds(urlParams) {
        let requestIds = {};
        for(const eachUrlParam of urlParams.keys()) {
            const requestIdMatches = eachUrlParam.match(/\[(.*?)\]/);
            if(requestIdMatches && requestIdMatches.length > 0) {
                requestIds[requestIdMatches[1]] = urlParams.get(eachUrlParam).split(",");
            }
        }
        return requestIds;
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
        this.loadFromHistory();
        this.loadUserCollections();
        let urlParameters = new URLSearchParams(window.location.search);
        const requestIds = this.getRequestIds(urlParameters), selectedApp = urlParameters.get("app"), reqIdArray = Object.keys(requestIds);
        if(reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(selectedApp, reqIdArray, eventTypes).then((result) => {
                if(result && result.numResults > 0) {
                    for(let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if(reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            let headers = [], queryParams = [], formData = [];
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: ""
                                });
                            }
                            for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam].join(","),
                                    description: ""
                                });
                            }
                            for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: ""
                                });
                            }
                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: "https://moviebook.dev.cubecorp.io/" + httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: "formData",
                                formData: formData,
                                rawData: "",
                                rawDataType: "json",
                                responseStatus: "NA",
                                responseStatusText: "NA",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
                                recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                responseBodyType: "json",
                                requestId: eachReqId,
                                outgoingRequestIds: requestIds[eachReqId],
                                eventData: reqResPair,
                                showOutgoingRequestsBtn: requestIds[eachReqId].length > 0
                            };
                            const mockEvent = {};
                            this.addTab(mockEvent, reqObject, selectedApp);
                        }
                    }
                }
            });
        }
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
    }

    getTabs(givenTabs) {
        return givenTabs.map((eachTab, index) => ({
            title: (
                <div className="tab-container">
                  <div className="tab-name">{eachTab.tabName ? eachTab.tabName : eachTab.httpURL ? eachTab.httpURL : "New"}</div>
                </div>
              ),
            getContent: () => {
                return (
                    <div className="tab-container">
                        <HttpClient tabId={eachTab.id}
                        requestId={eachTab.requestId}
                        httpMethod={eachTab.httpMethod}
                        httpURL={eachTab.httpURL}
                        headers={eachTab.headers} 
                        queryStringParams={eachTab.queryStringParams}
                        bodyType={eachTab.bodyType}
                        formData={eachTab.formData} 
                        rawData={eachTab.rawData}
                        rawDataType={eachTab.rawDataType}
                        addOrRemoveParam={this.addOrRemoveParam} 
                        updateParam={this.updateParam}
                        updateBodyOrRawDataType={this.updateBodyOrRawDataType}
                        driveRequest={this.driveRequest}
                        responseStatus={eachTab.responseStatus}
                        responseStatusText={eachTab.responseStatusText}
                        responseHeaders={eachTab.responseHeaders}
                        responseBody={eachTab.responseBody}
                        recordedResponseHeaders={eachTab.recordedResponseHeaders}
                        recordedResponseBody={eachTab.recordedResponseBody}
                        responseBodyType={eachTab.responseBodyType}
                        showOutgoingRequests={this.showOutgoingRequests}
                        showOutgoingRequestsBtn={eachTab.showOutgoingRequestsBtn}
                        showSaveModal={this.showSaveModal} >
                        </HttpClient>
                    </div>
              )},
            /* Optional parameters */
            key: eachTab.id,
            tabClassName: 'md-hc-tab',
            panelClassName: 'md-hc-tab-panel'
        }));
    }

    renderTreeNodeHeader(props) {
        return (
            <div style={props.style.base}>
                <div style={props.style.title}>
                    <div style={{paddingLeft: "9px", backgroundColor: "", display: "flex", width: "450px"}}>
                        <div style={{flexDirection: "column", width: "36px", verticalAlign: "top", }}>
                            <Label bsStyle="default" style={{fontWeight: "600", fontSize: "9px"}}>{props.node.method}</Label>
                        </div>
                        <div style={{flex: "1", wordBreak: "break-word", verticalAlign: "top", fontSize: "12px"}}>
                            <span style={{paddingLeft: "5px", marginLeft: "5px", borderLeft: "2px solid #fc6c0a"}} >{props.node.name + " " + moment(props.node.reqTimestamp * 1000).format("hh:mm:ss")}</span>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    render() {
        const { cube } = this.props;
        const { app, cubeRunHistory, userCollections, collectionName, collectionLabel } = this.state;

        return (
            <div className="" style={{display: "flex", height: "100%"}}>
                
                <aside className="" style={{ "width": "250px", "height": "100%", "background": "#EAEAEA", "padding": "10px", "display": "flex", "flexDirection": "column", overflow: "auto"}}>
                    <div style={{marginTop: "10px", marginBottom: "10px"}}>
                        <div className="label-n">APPLICATION</div>
                        <div className="application-name">{app}</div>
                    </div>
                    <Tabs defaultActiveKey={1} id="uncontrolled-tab-example">
                        <Tab eventKey={1} title="History">
                            <div className="margin-top-10">
                                <div className="value-n"></div>
                            </div>
                            <div className="margin-top-10">
                                {Object.keys(cubeRunHistory).sort().map((k, i) => {
                                    return (
                                        <Panel key={k + "_" + i} id="collapsible-panel-example-2" defaultExpanded>
                                            <Panel.Heading style={{paddingLeft: "9px"}}>
                                                <Panel.Title toggle>
                                                    {k}
                                                </Panel.Title>
                                            </Panel.Heading>
                                            <Panel.Collapse>
                                                <Panel.Body style={{padding: "3px"}}>
                                                    {cubeRunHistory[k].map(eachTabRun => {
                                                        /* return (
                                                            <div key={eachTabRun.reqTimestamp} style={{padding: "5px", backgroundColor: ""}}>
                                                                <div style={{display: "inline-block", width: "21%"}}>
                                                                    <Label bsStyle="default" style={{fontWeight: "600"}}>{eachTabRun.method.toUpperCase()}</Label>
                                                                </div>
                                                                <div style={{paddingLeft: "5px", display: "inline-block", wordBreak: "break-word", width: "78%", verticalAlign: "middle", fontSize: "12px", color: "#9CA5AB" , cursor: "pointer"}}>
                                                                    {eachTabRun.apiPath}
                                                                </div>
                                                            </div>
                                                        ); */
                                                        return (
                                                            <Treebeard key={eachTabRun.id}
                                                                data={eachTabRun}
                                                                style={CollectionTreeCSS}
                                                                onToggle={this.onToggle}
                                                                decorators={{...decorators, Header: this.renderTreeNodeHeader}}
                                                            />
                                                        );
                                                    })}
                                                </Panel.Body>
                                            </Panel.Collapse>
                                        </Panel>
                                    )
                                })}
                            </div>
                        </Tab>
                        <Tab eventKey={2} title="Collections">
                            <div className="margin-top-10">
                                <div className="value-n"></div>
                            </div>
                            <div className="margin-top-10">
                                {userCollections && userCollections.map(eachCollec => {
                                    return (
                                        <Panel id="collapsible-panel-example-2" key={eachCollec.collec} value={eachCollec.collec} onClick={() => this.handlePanelClick(eachCollec.collec)}>
                                            <Panel.Heading style={{paddingLeft: "9px"}}>
                                                <Panel.Title toggle>
                                                    {eachCollec.name}
                                                </Panel.Title>
                                            </Panel.Heading>
                                            <Panel.Collapse>
                                                <Panel.Body style={{padding: "3px", width: "100%", overflow: "scroll"}}>
                                                    {eachCollec.apiTraces && eachCollec.apiTraces.map((eachApiTrace) => {
                                                        return (
                                                            <Treebeard key={eachApiTrace.id}
                                                                data={eachApiTrace}
                                                                style={CollectionTreeCSS}
                                                                onToggle={this.onToggle}
                                                                decorators={{...decorators, Header: this.renderTreeNodeHeader}}
                                                            />
                                                        );
                                                    })}
                                                    
                                                </Panel.Body>
                                            </Panel.Collapse>
                                        </Panel>
                                    );
                                })}
                            </div>
                        </Tab>
                    </Tabs>
                </aside>
                <main className="content-wrapper" style={{flex:"1", overflow: "auto", padding: "25px", margin: "0"}}>
                    <div>
                        <div className="vertical-middle inline-block">
                            <svg height="21"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                            </svg>
                        </div>
                        <div className="inline-block vertical-middle" style={{fontWeight: "bold", position: "relative", bottom: "3px", opacity: "0.5", paddingLeft: "10px"}}>API CATALOG - VIEW REQUEST DETAILS</div>
                    </div>

                    <div>
                        <FormGroup>
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                type="text"
                                placeholder="Search"
                            />
                        </FormGroup>
                    </div>
                    <div style={{marginRight: "7px"}}>
                        <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}></div>
                        <div style={{display: "inline-block", width: "80%", textAlign: "right"}}>
                            <div className="btn btn-sm cube-btn text-center" style={{display: this.state.toggleTestAndOutgoingRequests ? "" : "none"}} onClick={this.addTab}>
                                <Glyphicon glyph="plus" /> ADD TAB
                            </div>
                        </div>
                    </div>
                    <div style={{marginTop: "10px", display: this.state.toggleTestAndOutgoingRequests ? "" : "none"}}>
                        <ResponsiveTabs items={this.getTabs(this.state.tabs)} tabsWrapperClass={"md-hc-tabs-wrapper"} allowRemove={true} removeActiveOnly={false} showMore={true} selectedTabKey={this.state.selectedTabKey} onChange={this.handleTabChange} onRemove={this.handleRemoveTab} />
                    </div>
                    <div style={{marginTop: "10px", display: !this.state.toggleTestAndOutgoingRequests ? "" : "none"}}>
                        <div style={{marginBottom: "10px"}}>
                            <div className="btn btn-sm cube-btn text-center" style={{ padding: "2px 10px", display: "inline-block"}} onClick={this.handleClick}>
                                <Glyphicon glyph="chevron-left" /> BACK
                            </div>
                        </div>
                        <ResponsiveTabs items={this.getTabs(this.state.outgoingRequests)} tabsWrapperClass={"md-hc-tabs-wrapper"} allowRemove={false} showMore={true} />
                    </div>
                    <div>
                        <Modal show={this.state.showSaveModal} onHide={this.handleCloseModal}>
                            <Modal.Header closeButton>
                                <Modal.Title>Save to collection</Modal.Title>
                            </Modal.Header>
                            <Modal.Body>
                                <h5 style={{textAlign: 'center'}}>
                                    Create a new collection
                                </h5>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Name</ControlLabel>
                                        <FormControl componentClass="input" placeholder="Name" name="collectionName" value={collectionName} onChange={this.handleChange} />
                                    </FormGroup>
                                </div>
                                <div>
                                    <FormGroup>
                                        <ControlLabel>Label</ControlLabel>
                                        <FormControl componentClass="input" placeholder="Label" name="collectionLabel" value={collectionLabel} onChange={this.handleChange} />
                                    </FormGroup>
                                </div>
                                <div>
                                    <Button onClick={this.handleCreateCollection}>Create</Button>
                                </div>
                                <hr />
                                <h5 style={{textAlign: 'center'}}>
                                    Select an exisiting collection
                                </h5>
                                <div>
                                    <FormGroup style={{marginBottom: "0px"}}>
                                        <FormControl componentClass="select" placeholder="Select" name="userCollectionId" value={this.userCollectionId} onChange={this.handleChange}>
                                        <option value=""></option>
                                            {userCollections && userCollections.map((eachUserCollection) => {
                                                return (
                                                    <option key={eachUserCollection.id} value={eachUserCollection.id}>{eachUserCollection.name}</option>
                                                );
                                            })}
                                        </FormControl>
                                    </FormGroup>
                                </div>
                                
                            </Modal.Body>
                            <Modal.Footer>
                                <Button onClick={this.handleSave}>Save</Button>
                                <Button onClick={this.handleCloseModal}>Close</Button>
                            </Modal.Footer>
                        </Modal>
                    </div>
                </main>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedHttpClientTabs = connect(mapStateToProps)(HttpClientTabs);

export default connectedHttpClientTabs
