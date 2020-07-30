import  React , { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Glyphicon, Radio, Checkbox, Tabs, Tab, Panel, Label, Modal, Button, ControlLabel, OverlayTrigger, Popover } from 'react-bootstrap';
import {Treebeard, decorators} from 'react-treebeard';

import _, { head } from 'lodash';
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
import TreeNodeContainer from "./TreeNodeContainer";
import TreeNodeToggle from "./TreeNodeToggle";
import ResponsiveTabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";
import CollectionTreeCSS from "./CollectionTreeCSS";

import {
    validateAndCreateDiffLayoutData  
} from "../../utils/diff/diff-process.js";
import EnvVar from "./EnvVar";
import Mustache from "mustache"
import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { generateRunId } from "../../utils/http_client/utils";

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
                httpURLShowOnly: "http://www.mocky.io/v2/5ed952b7310000f4dec4ed0a",
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
                showOutgoingRequestsBtn: false,
                showSaveBtn: false,
                outgoingRequests: [],
                diffLayoutData: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: "",
                recordingIdAddedFromClient: "",
                collectionIdAddedFromClient: "",
                traceIdAddedFromClient: "",
                recordedHistory: null
            }],
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
            collectionLabel: "",
            modalErroSaveMessage: "",
            modalErroCreateCollectionMessage: "",
            showEnvVarModal: false,
            selectedEnvironment: "",
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
        this.handleTreeNodeClick = this.handleTreeNodeClick.bind(this);
        this.renderTreeNodeHeader = this.renderTreeNodeHeader.bind(this);

        this.handleRowClick = this.handleRowClick.bind(this);
    }

    handleRowClick(isOutgoingRequest, tabId) {
        
    }

    handleCloseModal() {
        this.setState({ showSaveModal: false });
    }
    
    showSaveModal(isOutgoingRequest, tabId) {
        this.setState({ 
            showSaveModal: true, 
            collectionName: "", 
            collectionLabel: "", 
            selectedSaveableTabId: tabId, 
            modalErroSaveMessage: "", 
            modalErroCreateCollectionMessage: ""
        });
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

    getTabIndexGivenTabId (tabId, tabs) {
        let filteredTabs = tabs.filter((e) => e.id === tabId);
        for(let i = 0; i < tabs.length; i++) {
            if(tabs[i].id === tabId){
                return i;
            }
        }
        return -1;
    }



    addOrRemoveParam(isOutgoingRequest, tabId, type, op, id) {
        const {selectedTabKey, tabs} = this.state;
        if(isOutgoingRequest) {
            if(op === "delete") {
                this.setState({
                    tabs: tabs.map(eachTab => {
                        if(eachTab.id === selectedTabKey) {
                            eachTab.outgoingRequests.map((eachOutgoingTab) => {
                                if (eachOutgoingTab.id === tabId) {
                                    eachOutgoingTab[type] = eachOutgoingTab[type].filter((e) => e.id !== id);
                                }
                            })
                        }
                        return eachTab; 
                    })
                });
            } else {
                this.setState({
                    tabs: tabs.map(eachTab => {
                        if(eachTab.id === selectedTabKey) {
                            eachTab.outgoingRequests.map((eachOutgoingTab) => {
                                if (eachOutgoingTab.id === tabId) {
                                    eachOutgoingTab[type] = [...eachOutgoingTab[type], {
                                        id: uuidv4(),
                                        name: "",
                                        value: "",
                                        description: "",
                                        selected: true,
                                    }];
                                }
                            })
                        }
                        return eachTab; 
                    })
                });
            }
        } else {
            let tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
            if(tabIndex < 0) return;
            if(op === "delete") {
                this.setState({
                    tabs: tabs.map(eachTab => {
                        if (eachTab.id === tabId) {
                            eachTab[type] = eachTab[type].filter((e) => e.id !== id);
                        }
                        return eachTab; 
                    })
                });
            } else {
                this.setState({
                    tabs: tabs.map(eachTab => {
                        if (eachTab.id === tabId) {
                            eachTab[type] = [...eachTab[type], {
                                id: uuidv4(),
                                name: "",
                                value: "",
                                description: "",
                                selected: true,
                            }];
                        }
                        return eachTab; 
                    })
                });
            }
        }
    }

    updateParam(isOutgoingRequest, tabId, type, key, value, id) {
        const {selectedTabKey, tabs} = this.state;
        if(isOutgoingRequest) {
            const selectedTabIndex = this.getTabIndexGivenTabId(selectedTabKey, tabs);
            const selectedOutgoingTabIndex = this.getTabIndexGivenTabId(tabId, tabs[selectedTabIndex]["outgoingRequests"]);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][type];
            if(_.isArray(params)) {
                let specificParamArr = params.filter((e) => e.id === id);
                if(specificParamArr.length > 0) {
                    specificParamArr[0][key] = value;
                }
            } else {
                params = value;
            }
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === tabId) {
                                eachOutgoingTab[type] = params;
                                // if(type === "httpURL") eachOutgoingTab.tabName = params;
                            }
                        })
                    }
                    return eachTab; 
                })
            });
        } else {
            let tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
            if(tabIndex < 0) return;
            let params = tabs[tabIndex][type];
            if(_.isArray(params)) {
                let specificParamArr = params.filter((e) => e.id === id);
                if(specificParamArr.length > 0) {
                    specificParamArr[0][key] = value;
                }
            } else {
                params = value;
            }
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab[type] = params;
                        // if(type === "httpURL") eachTab.tabName = params;
                    }
                    return eachTab; 
                })
            });
            //this.setState({[type]: params})
        }
    }

    updateAllParams = (isOutgoingRequest, tabId, type, key, value) => {
        const {selectedTabKey, tabs} = this.state;
        if(isOutgoingRequest) {
            const selectedTabIndex = this.getTabIndexGivenTabId(selectedTabKey, tabs);
            const selectedOutgoingTabIndex = this.getTabIndexGivenTabId(tabId, tabs[selectedTabIndex]["outgoingRequests"]);
            let params = tabs[selectedTabIndex]["outgoingRequests"][selectedOutgoingTabIndex][type];
            if(_.isArray(params)) {
                params.forEach((param) => {param[key]=value})
            } else {
                params = value;
            }
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === tabId) {
                                eachOutgoingTab[type] = params;
                                // if(type === "httpURL") eachOutgoingTab.tabName = params;
                            }
                        })
                    }
                    return eachTab; 
                })
            });
        } else {
            let tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
            if(tabIndex < 0) return;
            let params = tabs[tabIndex][type];
            if(_.isArray(params)) {
                params.forEach((param) => {param[key]=value})
            } else {
                params = value;
            }
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab[type] = params;
                        // if(type === "httpURL") eachTab.tabName = params;
                    }
                    return eachTab; 
                })
            });
            //this.setState({[type]: params})
        }
    }

    updateBodyOrRawDataType(isOutgoingRequest, tabId, type, value) {
        const {selectedTabKey, tabs} = this.state;
        if(isOutgoingRequest) {
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === selectedTabKey) {
                        eachTab.outgoingRequests.map((eachOutgoingTab) => {
                            if (eachOutgoingTab.id === tabId) {
                                eachOutgoingTab[type] = value;
                            }
                        })
                    }
                    return eachTab; 
                })
            });
        } else {
            let tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
            if(tabIndex < 0) return;
            // this.setState({[type]: value});
            this.setState({
                tabs: tabs.map(eachTab => {
                    if (eachTab.id === tabId) {
                        eachTab[type] = value;
                    }
                    return eachTab; 
                })
            });
        } 
    }

    showOutgoingRequests(tabId, traceId, collectionId, recordingId) {    
        const { tabs, app } = this.state, tabIndex = this.getTabIndexGivenTabId(tabId, tabs);
        const reqIdArray = tabs[tabIndex]["outgoingRequestIds"];
        if(tabs[tabIndex]["outgoingRequests"] && tabs[tabIndex]["outgoingRequests"].length > 0) {
            this.setState({
                toggleTestAndOutgoingRequests: !this.state.toggleTestAndOutgoingRequests
            });
            return;
        };
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
                            let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
                            
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: "",
                                    selected: true,
                                });
                            }
                            for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                                    description: "",
                                    selected: true,
                                });
                            }
                            for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: "",
                                    selected: true,
                                });
                                rawDataType = "";
                            }
                            if(httpRequestEvent.payload[1].body) {
                                if(!_.isString(httpRequestEvent.payload[1].body)) {
                                    try {
                                        rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                                        rawDataType = "json";
                                    } catch (err) {
                                        console.error(err);
                                    }
                                } else {
                                    rawData = httpRequestEvent.payload[1].body;
                                    rawDataType = "text";
                                }
                            }
                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: httpRequestEvent.apiPath,
                                httpURLShowOnly: httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
                                formData: formData,
                                rawData: rawData,
                                rawDataType: rawDataType,
                                responseStatus: "NA",
                                responseStatusText: "",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4): "",
                                recordedResponseBody: httpResponseEvent ?  httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                responseBodyType: "json",
                                showOutgoingRequestsBtn: false,
                                isOutgoingRequest: true,
                                showSaveBtn: true,
                                outgoingRequests: [],
                                service: httpRequestEvent.service,
                                recordingIdAddedFromClient: recordingId,
                                collectionIdAddedFromClient: collectionId,
                                traceIdAddedFromClient: traceId
                            };
                            const tabId = uuidv4();
                            outgoingRequests.push({
                                id: tabId,
                                requestId: eachReqId,
                                eventData: reqResPair,
                                tabName: reqObject.httpURLShowOnly ? reqObject.httpURLShowOnly : "New",
                                ...reqObject
                            })
                        }
                    }
                    this.setState({
                        toggleTestAndOutgoingRequests: !this.state.toggleTestAndOutgoingRequests,
                        tabs: this.state.tabs.map(eachTab => {
                            if (eachTab.id === tabId) {
                                eachTab["outgoingRequests"] = outgoingRequests;
                            }
                            return eachTab; 
                        })
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
        httpReqestHeaders
            .filter((header) => header.selected)
            .forEach(each => {
                if(each.name && each.value && each.name.indexOf(":") < 0) headers.append(each.name, each.value);
                // ideally for ingress requests
                if(each.name === "x-b3-spanid" && each.value) headers.append("baggage-parent-span-id", each.value);
            })
        return headers;
    }

    extractBody(httpRequestBody) {
        let formData = new FormData();
        if(_.isArray(httpRequestBody)) {
            httpRequestBody
                .filter((fparam) => fparam.selected)
                .forEach(each => {
                    if(each.name && each.value) formData.append(each.name, each.value);
                })
            return formData;
        } else {
            return httpRequestBody;
        }
    }

    extractQueryStringParams(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams
            .filter((param) => param.selected)
            .forEach(each => {
                if(each.name && each.value) qsParams[each.name] = each.value;
            })
        return qsParams;
    }

    applyEnvVars = (httpRequestURL, httpRequestQueryStringParams, fetchConfig) => {
        const headers = fetchConfig.headers;
        const body = fetchConfig.body;
        let headersRendered = new Headers(), bodyRendered = "";

        const currentEnvironment = this.getCurrentEnvirnoment();

        // convert list of envvar objects to a map
        const currentEnvVars = currentEnvironment ? Object.fromEntries(
            currentEnvironment.vars.map((v) => ([v.key, v.value]))
        ) : {};

        // define method to render Mustache template
        const renderEnvVars = (input) => input ? Mustache.render(input, currentEnvVars) : input;
    
        const httpRequestURLRendered = renderEnvVars(httpRequestURL);
        
        const httpRequestQueryStringParamsRendered = Object.fromEntries(
            Object.entries(httpRequestQueryStringParams)
            .map(
                   ([key, value]) => [renderEnvVars(key), renderEnvVars(value)]
                )
            );
        
        if (headers) {
            for(let pair of headers.entries()) {
                headersRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
            }
        }

        if (body instanceof FormData) {
            bodyRendered = new FormData();
            for(let pair of body.entries()) {
                bodyRendered.append(renderEnvVars(pair[0]), renderEnvVars(pair[1]))
            }
        } else {
            // string
            bodyRendered = renderEnvVars(body)
        }

        const fetchConfigRendered = {
            method: fetchConfig.method,
            headers: headersRendered,
            body: bodyRendered,
        }

        return [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered]
    }

    driveRequest(isOutgoingRequest, tabId) {
        const {tabs, selectedTabKey, app} = this.state;
        const user = JSON.parse(localStorage.getItem('user'));
        const userId = user.username,
        customerId = user.customer_name;
        let tabsToProcess = tabs;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        if(tabIndex < 0) return;
        const {userHistoryCollection} = this.state;
        // generate a new run id every time a request is run
        const runId = generateRunId();
        // make the request and update response status, headers & body
        // extract headers
        // extract body
        const { headers, queryStringParams, bodyType, rawDataType } = tabToProcess;
        const httpReqestHeaders = this.extractHeaders(headers);

        const httpRequestQueryStringParams = this.extractQueryStringParams(queryStringParams);
        let httpRequestBody;
        if(bodyType === "formData") {
            const { formData } = tabToProcess;
            httpRequestBody = this.extractBody(formData);
        }
        if(bodyType === "rawData") {
            const { rawData } = tabToProcess;
            httpRequestBody = this.extractBody(rawData);
        }
        const httpMethod = tabToProcess.httpMethod;
        const httpRequestURL = tabToProcess.httpURL;

        let fetchConfig = {
            method: httpMethod,
            headers: httpReqestHeaders
        }
        if(httpMethod !== "GET".toLowerCase() && httpMethod !== "HEAD".toLowerCase()) {
            fetchConfig["body"] = httpRequestBody;
        }
        // render environment variables
        const [httpRequestURLRendered, httpRequestQueryStringParamsRendered, fetchConfigRendered] = this.applyEnvVars(httpRequestURL, httpRequestQueryStringParams, fetchConfig);

        const fetchUrlRendered = httpRequestURLRendered + (httpRequestQueryStringParamsRendered ? "?" + stringify(httpRequestQueryStringParamsRendered) : "");

        this.setState({
            tabs: this.state.tabs.map(eachTab => {
                if (eachTab.id === tabId) {
                    eachTab["responseStatus"] = "WAITING...";
                    eachTab["showCompleteDiff"] = false;
                    if(eachTab["clearIntervalHandle"]) clearInterval(eachTab["clearIntervalHandle"]);
                }
                return eachTab; 
            })
        });
        // Make request
        // https://www.mocky.io/v2/5185415ba171ea3a00704eed
        let fetchedResponseHeaders = {}, responseStatus = "", responseStatusText = "";
        const startDate = new Date(Date.now() - 2 * 1000).toISOString();
        return fetch(fetchUrlRendered, fetchConfigRendered).then((response) => {
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
                this.saveToCollection(isOutgoingRequest, tabId, userHistoryCollection.id, "History", runId);
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
            }, () => {
                // this.saveToCollection(isOutgoingRequest, tabId, userHistoryCollection.id, "History");
            });
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
        const tabToProcess = currentTabs[indexToRemove];
        if(tabToProcess && tabToProcess.clearIntervalHandle) clearInterval(tabToProcess.clearIntervalHandle);
    
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
                if(eachHeader && headersReceived[eachHeader]) headers[eachHeader] = headersReceived[eachHeader];
            })
        }
        
        return headers;
    }

    extractQueryStringParamsToCubeFormat(httpRequestQueryStringParams) {
        let qsParams = {};
        httpRequestQueryStringParams.forEach(each => {
            if(each.name && each.value) qsParams[each.name] = [each.value];
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

    getReqResFromTabData(eachPair, tabToSave, runId) {
        const httpRequestEventTypeIndex = eachPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = eachPair[httpRequestEventTypeIndex];
        const httpResponseEvent = eachPair[httpResponseEventTypeIndex];

        const { headers, queryStringParams, bodyType, rawDataType, responseHeaders,responseBody, recordedResponseHeaders, recordedResponseBody } = tabToSave;
        const httpReqestHeaders = this.extractHeadersToCubeFormat(headers);
        const httpRequestQueryStringParams = this.extractQueryStringParamsToCubeFormat(queryStringParams);
        let httpRequestFormParams = {}, httpRequestBody = "";
        if(bodyType === "formData") {
            const { formData } = tabToSave;
            httpRequestFormParams = this.extractBodyToCubeFormat(formData);
        }
        if(bodyType === "rawData") {
            const { rawData } = tabToSave;
            httpRequestBody = this.extractBodyToCubeFormat(rawData);
        }
        const httpMethod = tabToSave.httpMethod;
        const apiPath = httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : ""; 
        const httpResponseHeaders = responseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(responseHeaders)) : recordedResponseHeaders ? this.extractHeadersToCubeFormat(JSON.parse(recordedResponseHeaders)) : null;
        const httpResponseBody = responseBody ? JSON.parse(responseBody) : recordedResponseBody ? JSON.parse(recordedResponseBody) : null;
        const reqResCubeFormattedData = {
            request: {
                ...httpRequestEvent,
                runId: runId,
                payload: [
                    "HTTPRequestPayload",
                    {
                        hdrs: httpReqestHeaders,
                        queryParams: httpRequestQueryStringParams,
                        formParams: httpRequestFormParams,
                        ...(httpRequestBody && {body: httpRequestBody}),
                        method: httpMethod.toUpperCase(),
                        path: apiPath,
                        pathSegments: apiPath.split("/")
                    }
                ]
            },
            response: {
                ...httpResponseEvent,
                runId: runId,
                payload: [
                    "HTTPResponsePayload",
                    {
                        hdrs: httpResponseHeaders,
                        body: httpResponseBody,
                        status: httpResponseEvent.payload[1].status,
                        statusCode: httpResponseEvent.payload[1].statusCode
                    }
                ] 
            }
        }
        return reqResCubeFormattedData;
    }

    saveToCollection(isOutgoingRequest, tabId, recordingId, type, runId="") {
        const {tabs, selectedTabKey, app} = this.state;
        const {dispatch} = this.props;
        let tabsToProcess = tabs;
        const tabIndex = this.getTabIndexGivenTabId(tabId, tabsToProcess);
        const tabToProcess = tabsToProcess[tabIndex];
        if(!tabToProcess.eventData) return;
        const reqResPair = tabToProcess.eventData;
        if(reqResPair.length > 0) {
            const data = [];
            data.push(this.getReqResFromTabData(reqResPair, tabToProcess, runId));
            if(type !== "History") {
                tabToProcess.outgoingRequests.forEach((eachOutgoingTab) => {
                    if(eachOutgoingTab.eventData && eachOutgoingTab.eventData.length > 0) {
                        data.push(this.getReqResFromTabData(eachOutgoingTab.eventData, eachOutgoingTab));
                    }
                });
            }
            try {
                api.post(`${config.apiBaseUrl}/cs/storeUserReqResp/${recordingId}`, data)
                    .then((serverRes) => {
                        let clearIntervalHandle;
                        if(type === "History") {
                            const jsonTraceReqData = serverRes.data.response && serverRes.data.response.length > 0 ? serverRes.data.response[0] : "";
                            try {
                                const parsedTraceReqData = JSON.parse(jsonTraceReqData);
                                const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                                const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                                const apiPath = httpRequestEvent.apiPath ? httpRequestEvent.apiPath : httpRequestEvent.payload[1].path ? httpRequestEvent.payload[1].path : ""; 
                                this.loadRecordedHistory(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath);
                                clearIntervalHandle = setInterval(() => {
                                    this.loadRecordedHistory(tabId, parsedTraceReqData.newTraceId, parsedTraceReqData.newReqId, runId, apiPath);
                                }, 5000);
                                setTimeout(() => {
                                    if(clearIntervalHandle) clearInterval(clearIntervalHandle);
                                }, 120000);
                            } catch (error) {
                                console.error("Error ", error);
                                throw new Error("Error");
                            }
                        }
                        this.setState({
                            showSaveModal : type === "History" ? false : true,
                            modalErroSaveMessage: "Saved Successfully! You can close this modal.",
                            tabs: this.state.tabs.map(eachTab => {
                                if (eachTab.id === tabId) {
                                    if(clearIntervalHandle) eachTab["clearIntervalHandle"] = clearIntervalHandle;
                                }
                                return eachTab; 
                            })
                        });
                        setTimeout(() => {
                            this.loadFromHistory();
                            this.loadUserCollections();
                            // update api catalog golden and collection lists
                            dispatch(apiCatalogActions.fetchGoldenCollectionList(app, "Golden"))
                            dispatch(apiCatalogActions.fetchGoldenCollectionList(app, "UserGolden"))

                        }, 2000);
                    }, (error) => {
                        this.setState({
                            showSaveModal : type === "History" ? false : true,
                            modalErroSaveMessage: "Error saving: " + error
                        })
                        console.error("error: ", error);
                    })
            } catch(error) {
                console.error("Error ", error);
                this.setState({
                    showSaveModal : type === "History" ? false : true,
                    modalErroSaveMessage: "Error saving: " + error
                })
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
                    this.setState({
                        showSaveModal : true,
                        modalErroCreateCollectionMessage: "Created Successfully! Please select this newly created collection from below dropdown and click save."
                    });
                }, (error) => {
                    this.setState({
                        showSaveModal : true,
                        modalErroCreateCollectionMessage: "Error saving: " + error
                    })
                    console.error("error: ", error);
                })
        } catch(error) {
            this.setState({
                showSaveModal : true,
                modalErroCreateCollectionMessage: "Error saving: " + error
            })
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    handleSave() {
        const { userCollectionId, userCollections, selectedSaveableTabId } = this.state;
        let isOutgoingRequest = false;
        const {tabs, selectedTabKey} = this.state;
        let tabsToProcess = tabs;
        let tabIndex = this.getTabIndexGivenTabId(selectedSaveableTabId, tabsToProcess);
        if(tabIndex < 0) {
            const indexToFind = tabs.findIndex(tab => tab.id === selectedTabKey);
            tabsToProcess = tabs[indexToFind]["outgoingRequests"];
            tabIndex = this.getTabIndexGivenTabId(selectedSaveableTabId, tabsToProcess);
            if(tabIndex > -1) {
                isOutgoingRequest = true;
            }
        }
        const selectedCollection = userCollections.find((eachCollection) => {
            return eachCollection.id === userCollectionId;
        });
        this.saveToCollection(isOutgoingRequest, selectedSaveableTabId, selectedCollection.id, "UserGolden");
    }

    loadUserCollections() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {app} = this.state;
        const userId = encodeURIComponent(user.username),
        customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=UserGolden&archived=false`)
                .then((serverRes) => {
                    const userCollections = serverRes.filter((eachCollection) => {
                        return eachCollection.recordingType !== "History"
                    });
                    this.setState({
                        userCollections: userCollections
                    });
                }, (error) => {
                    console.error("error: ", error);
                })
        } catch(error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    loadFromHistory() {
        const user = JSON.parse(localStorage.getItem('user'));
        const {app} = this.state;
        const userId = encodeURIComponent(user.username),
            customerId = encodeURIComponent(user.customer_name);
        try {
            api.get(`${config.apiBaseUrl}/cs/searchRecording?customerId=${user.customer_name}&app=${app}&userId=${userId}&recordingType=History&archived=false`)
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
                    const startTime = new Date(Date.now() - 30 * 60 * 1000).toISOString();
                    api.get(`${config.apiBaseUrl}/as/getApiTrace/${customerId}/${app}?depth=100&collection=${fetchedUserHistoryCollection.collec}&startDate=${startTime}`)
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
                                    eachApiTraceEvent["toggled"] = false;
                                    eachApiTraceEvent["recordingIdAddedFromClient"] = fetchedUserHistoryCollection.id;
                                    eachApiTraceEvent["traceIdAddedFromClient"] = eachApiTrace.traceId;
                                    eachApiTraceEvent["collectionIdAddedFromClient"] = eachApiTrace.collection;
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
                            console.error("err: ", err);
                        })
                }, (error) => {
                    console.error("error: ", error);
                })
        } catch(error) {
            console.error("Error ", error);
            throw new Error("Error");
        }
    }

    formatHttpEventToReqResObject(reqId, httpEventReqResPair) {
        const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
        const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
        let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
        for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
            headers.push({
                id: uuidv4(),
                name: eachHeader,
                value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                description: "",
                selected: true,
            });
        }
        for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
            queryParams.push({
                id: uuidv4(),
                name: eachQueryParam,
                value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                description: "",
                selected: true,
            });
        }
        for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
            formData.push({
                id: uuidv4(),
                name: eachFormParam,
                value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                description: "",
                selected: true,
            });
            rawDataType = "";
        }
        if(httpRequestEvent.payload[1].body) {
            if(!_.isString(httpRequestEvent.payload[1].body)) {
                try {
                    rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                    rawDataType = "json";
                } catch (err) {
                    console.error(err);
                }
            } else {
                rawData = httpRequestEvent.payload[1].body;
                rawDataType = "text";
            }
        }
        let reqObject = {
            httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
            httpURL: httpRequestEvent.apiPath,
            httpURLShowOnly:httpRequestEvent.apiPath,
            headers: headers,
            queryStringParams: queryParams,
            bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
            formData: formData,
            rawData: rawData,
            rawDataType: rawDataType,
            responseStatus: "NA",
            responseStatusText: "",
            responseHeaders: "",
            responseBody: "",
            recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
            recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
            responseBodyType: "json",
            requestId: reqId,
            outgoingRequestIds: [],
            eventData: httpEventReqResPair,
            showOutgoingRequestsBtn: false,
            showSaveBtn: false,
            outgoingRequests: [],
            showCompleteDiff: false,
            isOutgoingRequest: false,
            service: httpRequestEvent.service,
            recordingIdAddedFromClient: "",
            collectionIdAddedFromClient: httpRequestEvent.collection,
            traceIdAddedFromClient: httpRequestEvent.traceId,
            apiPath: httpRequestEvent.apiPath
        };
        return reqObject;
    }

    loadRecordedHistory(tabId, traceId, reqId, runId, apiPath) {
        const { userHistoryCollection, app } = this.state;
        const user = JSON.parse(localStorage.getItem('user'));
        const historyCollectionId = userHistoryCollection.collec;
        api.get(`${config.apiBaseUrl}/as/getApiTrace/${user.customer_name}/${app}?depth=100&collection=${historyCollectionId}&apiPath=${apiPath}&runId=${runId}`)
            .then((res) => {
                res.response.sort((a, b) => {
                    return b.res.length - a.res.length;
                });
                const apiTrace = res.response[0];
                const selectedApp = app, reqIdArray = [];
                apiTrace && apiTrace.res.map((eachApiTraceEvent) => {
                    reqIdArray.push(eachApiTraceEvent.requestEventId);
                });
                
                if(reqIdArray && reqIdArray.length > 0) {
                    const eventTypes = [];
                    cubeService.fetchAPIEventData(selectedApp, reqIdArray, eventTypes).then((result) => {
                        if(result && result.numResults > 0) {
                            const ingressReqResPair = result.objects.filter(eachReq => eachReq.apiPath === apiPath);
                            let ingressReqObj;
                            if(ingressReqResPair.length > 0) {
                                ingressReqObj = this.formatHttpEventToReqResObject(reqId, ingressReqResPair);
                            }
                            for(let eachReqId of reqIdArray) {
                                const reqResPair = result.objects.filter(eachReq => {
                                    return (eachReq.reqId === eachReqId && eachReq.apiPath !== apiPath);
                                });
                                if(reqResPair.length > 0 && eachReqId !== reqId) {
                                    let reqObject = this.formatHttpEventToReqResObject(eachReqId, reqResPair);
                                    ingressReqObj.outgoingRequests.push(reqObject);
                                }
                            }
                            this.setState({
                                tabs: this.state.tabs.map(eachTab => {
                                    if (eachTab.id === tabId) {
                                        eachTab["recordedHistory"] = ingressReqObj;
                                        if(reqIdArray.length > 1 && eachTab["clearIntervalHandle"]) clearInterval(eachTab["clearIntervalHandle"]); // need interval for runid?
                                    }
                                    return eachTab; 
                                })
                            });
                        }
                    });
                }
            }, (err) => {
                console.error("err: ", err);
            })
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
                    res.response.sort((a, b) => {
                        return b.res[0].reqTimestamp - a.res[0].reqTimestamp;
                    });
                    res.response.map(eachApiTrace => {
                        eachApiTrace.res.map((eachApiTraceEvent) => {
                            eachApiTraceEvent["name"] = eachApiTraceEvent["apiPath"];
                            eachApiTraceEvent["id"] = eachApiTraceEvent["requestEventId"];
                            eachApiTraceEvent["toggled"] = false;
                            eachApiTraceEvent["recordingIdAddedFromClient"] = selectedCollection.id;
                            eachApiTraceEvent["traceIdAddedFromClient"] = eachApiTrace.traceId;
                            eachApiTraceEvent["collectionIdAddedFromClient"] = eachApiTrace.collection;
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
                    console.error("err: ", err);
                });
            }
            
        } catch(error) {
            console.error("Error ", error);
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
                httpURLShowOnly:"",
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
                responseBodyType: "",
                requestId: "",
                outgoingRequestIds: [],
                eventData: null,
                showOutgoingRequestsBtn: false,
                showSaveBtn: false,
                outgoingRequests: [],
                showCompleteDiff: false,
                isOutgoingRequest: false,
                service: ""
            };
        }
        this.setState({
            tabs: [...this.state["tabs"], {
                id: tabId,
                tabName: reqObject.httpURLShowOnly ? reqObject.httpURLShowOnly : "New",
                ...reqObject
            }],
            selectedTabKey: tabId,
            app: appAvailable
        });
        return tabId;
    }

    getRequestIds() {
        const {apiCatalog: {httpClientRequestIds}} = this.props;
        return httpClientRequestIds;
    }

    formatHttpEventToTabObject(reqId, requestIdsObj, httpEventReqResPair) {
        const httpRequestEventTypeIndex = httpEventReqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
        const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
        const httpRequestEvent = httpEventReqResPair[httpRequestEventTypeIndex];
        const httpResponseEvent = httpEventReqResPair[httpResponseEventTypeIndex];
        let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
        for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
            headers.push({
                id: uuidv4(),
                name: eachHeader,
                value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                description: "",
                selected: true,
            });
        }
        for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
            queryParams.push({
                id: uuidv4(),
                name: eachQueryParam,
                value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                description: "",
                selected: true,
            });
        }
        for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
            formData.push({
                id: uuidv4(),
                name: eachFormParam,
                value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                description: "",
                selected: true,
            });
            rawDataType = "";
        }
        if(httpRequestEvent.payload[1].body) {
            if(!_.isString(httpRequestEvent.payload[1].body)) {
                try {
                    rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                    rawDataType = "json";
                } catch (err) {
                    console.error(err);
                }
            } else {
                rawData = httpRequestEvent.payload[1].body;
                rawDataType = "text";
            }
        }
        let reqObject = {
            httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
            httpURL: httpRequestEvent.apiPath,
            httpURLShowOnly:httpRequestEvent.apiPath,
            headers: headers,
            queryStringParams: queryParams,
            bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
            formData: formData,
            rawData: rawData,
            rawDataType: rawDataType,
            responseStatus: "NA",
            responseStatusText: "",
            responseHeaders: "",
            responseBody: "",
            recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
            recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
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
            traceIdAddedFromClient: httpRequestEvent.traceId
        };
        return reqObject;
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
        this.loadFromHistory();
        this.loadUserCollections();
        let urlParameters = new URLSearchParams(window.location.search);
        
        const requestIds = this.getRequestIds(), selectedApp = urlParameters.get("app"), reqIdArray = Object.keys(requestIds);
        /* Object.keys(requestIds).map((objKey) => {
            reqIdArray.push(...requestIds[objKey]);
        }); */
        if(reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(selectedApp, reqIdArray, eventTypes).then((result) => {
                if(result && result.numResults > 0) {
                    for(let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if(reqResPair.length > 0) {
                            let reqObject = this.formatHttpEventToTabObject(eachReqId, requestIds, reqResPair);
                            const mockEvent = {};
                            const savedTabId = this.addTab(mockEvent, reqObject, selectedApp);
                            this.showOutgoingRequests(savedTabId, reqObject.traceIdAddedFromClient, reqObject.collectionIdAddedFromClient, reqObject.recordingIdAddedFromClient);
                        }
                    }
                }
            });
        }

        dispatch(apiCatalogActions.fetchEnvironments())
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        const { tabs } = this.state;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
        tabs.map((eachTab) => {
            if(eachTab.clearIntervalHandle) {
                clearInterval(eachTab.clearIntervalHandle);
            }
        });
    }

    handleTreeNodeClick(node) {
        this.openTab(node);
    }

    openTab(node) {
        let urlParameters = new URLSearchParams(window.location.search);
        const {app} = this.state;
        const selectedApp = urlParameters.get("app"), reqIdArray = [node.requestEventId];
        if(reqIdArray && reqIdArray.length > 0) {
            const user = JSON.parse(localStorage.getItem('user'));
            const apiEventURL = `${config.recordBaseUrl}/getEvents`;
            let body = {
                "customerId": user.customer_name,
                "app": selectedApp,
                "eventTypes": [],
                "services": [node.service],
                "traceIds": [node.traceIdAddedFromClient],
                "reqIds": reqIdArray,
                "paths": [node.apiPath],
                "collection": node.collectionIdAddedFromClient
            }
            api.post(apiEventURL, body).then((result) => {
                if(result && result.numResults > 0) {
                    for(let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if(reqResPair.length === 1) {
                            reqResPair.push(result.objects.find(eachReq => eachReq.eventType === "HTTPResponse"));
                        }
                        if(reqResPair.length > 0) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            let headers = [], queryParams = [], formData = [], rawData = "", rawDataType = "";
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: uuidv4(),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: "",
                                    selected: true,
                                });
                            }
                            for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: uuidv4(),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam][0],
                                    description: "",
                                    selected: true,
                                });
                            }
                            for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: uuidv4(),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: "",
                                    selected: true,
                                });
                                rawDataType = "";
                            }
                            if(httpRequestEvent.payload[1].body) {
                                if(!_.isString(httpRequestEvent.payload[1].body)) {
                                    try {
                                        rawData = JSON.stringify(httpRequestEvent.payload[1].body, undefined, 4)
                                        rawDataType = "json";
                                    } catch (err) {
                                        console.error(err);
                                    }
                                } else {
                                    rawData = httpRequestEvent.payload[1].body;
                                    rawDataType = "text";
                                }
                            }

                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: httpRequestEvent.apiPath,
                                httpURLShowOnly: httpRequestEvent.apiPath,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: formData && formData.length > 0 ? "formData" : rawData && rawData.length > 0 ? "rawData" : "formData",
                                formData: formData,
                                rawData: rawData,
                                rawDataType: rawDataType,
                                responseStatus: "NA",
                                responseStatusText: "",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: httpResponseEvent ? JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4) : "",
                                recordedResponseBody: httpResponseEvent ? httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "" : "",
                                responseBodyType: "json",
                                requestId: httpRequestEvent.reqId,
                                outgoingRequestIds: node.children ? node.children.map(eachChild =>  eachChild.requestEventId) : [],
                                eventData: reqResPair,
                                showOutgoingRequestsBtn: node.children && node.children.length > 0,
                                showSaveBtn: true,
                                recordingIdAddedFromClient: node.recordingIdAddedFromClient,
                                collectionIdAddedFromClient: node.collectionIdAddedFromClient,
                                traceIdAddedFromClient: node.traceIdAddedFromClient,
                                outgoingRequests: [],
                                showCompleteDiff: false,
                                isOutgoingRequest: false,
                                service: httpRequestEvent.service
                            };
                            const mockEvent = {};
                            const savedTabId = this.addTab(mockEvent, reqObject, selectedApp);
                            this.showOutgoingRequests(savedTabId, node.traceIdAddedFromClient, node.collectionIdAddedFromClient, node.recordingIdAddedFromClient);
                        }
                    }
                }
            });
        }
    }

    getSelectedTabKey(givenTabs, type) {
        const {selectedTabKey} = this.state;
        if(type && type === "outgoingRequests") {
            const currentTab = givenTabs.find((eachTab) => eachTab.id === selectedTabKey);
            const tabsToRender = currentTab[type];
            return tabsToRender.length > 0 ? tabsToRender[0].id : ""
        }
        return "";
    }

    getTabs(givenTabs) {
        let tabsToRender = givenTabs;
        const {selectedTabKey, cubeRunHistory} = this.state;
        return tabsToRender.map((eachTab, index) => ({
            title: (
                <div className="tab-container">
                  <div className="tab-name">{eachTab.tabName ? eachTab.tabName : eachTab.httpURLShowOnly ? eachTab.httpURLShowOnly : "New"}</div>
                </div>
            ),
            getContent: () => {
                return (
                    <div className="tab-container">
                        <HttpClient currentSelectedTab={eachTab}
                        
                        /* tabId={eachTab.id}
                        requestId={eachTab.requestId}
                        httpMethod={eachTab.httpMethod}
                        httpURL={eachTab.httpURL}
                        headers={eachTab.headers} 
                        queryStringParams={eachTab.queryStringParams}
                        bodyType={eachTab.bodyType}
                        formData={eachTab.formData} 
                        rawData={eachTab.rawData}
                        rawDataType={eachTab.rawDataType}
                        responseStatus={eachTab.responseStatus}
                        responseStatusText={eachTab.responseStatusText}
                        responseHeaders={eachTab.responseHeaders}
                        responseBody={eachTab.responseBody}
                        recordedResponseHeaders={eachTab.recordedResponseHeaders}
                        recordedResponseBody={eachTab.recordedResponseBody}
                        responseBodyType={eachTab.responseBodyType}
                        outgoingRequests={eachTab.outgoingRequests}
                        showSaveBtn={eachTab.showSaveBtn}
                        showCompleteDiff={eachTab.showCompleteDiff}
                        service={eachTab.service}
                        diffLayoutData={eachTab.diffLayoutData} */

                        addOrRemoveParam={this.addOrRemoveParam} 
                        updateParam={this.updateParam}
                        updateAllParams={this.updateAllParams}
                        updateBodyOrRawDataType={this.updateBodyOrRawDataType}
                        driveRequest={this.driveRequest}
                        showSaveModal={this.showSaveModal}
                        handleRowClick={this.handleRowClick}
                        cubeRunHistory={cubeRunHistory} >
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
                        <div style={{flex: "1", wordBreak: "break-word", verticalAlign: "top", fontSize: "12px"}} onClick={() => this.handleTreeNodeClick(props.node)}>
                            <span style={{paddingLeft: "5px", marginLeft: "5px", borderLeft: "2px solid #fc6c0a"}} >{props.node.name + " " + moment(props.node.reqTimestamp * 1000).format("hh:mm:ss")}</span>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    renderTreeNodeContainer(props) {
        return (
            <TreeNodeContainer {...props} />
        );
    }

    renderTreeNodeToggle(props) {
        return (
            <TreeNodeToggle {...props} />
        );
    }

    getCurrentEnvirnoment = () => {
        const {apiCatalog: {environmentList}} = this.props;
        const {selectedEnvironment} = this.state;
        return _.find(environmentList, {name: selectedEnvironment})
    }
    
    renderEnvListDD = () => {
        const {apiCatalog: {environmentList}} = this.props;
        return (
        <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
            <FormControl componentClass="select" placeholder="Environment" style={{fontSize: "12px"}} value={this.state.selectedEnvironment} onChange={this.handleEnvChange} className="btn-sm">
                <option value="">No Environment</option>
                {environmentList.map((env) => (<option key={env.name} value={env.name}>{env.name}</option>))}
            </FormControl>
        </FormGroup>)
    }

    handleEnvChange = (e) => {
        this.setState({selectedEnvironment: e.target.value})
    }

    renderEnvPopoverBtn = () => {
        const currentEnvironment = this.getCurrentEnvirnoment();
        const envPopover = (<Popover title={this.state.selectedEnvironment || "No Environment"}>
                      <div style={{margin: "5px", width: "200px"}}>
                        {currentEnvironment && !_.isEmpty(currentEnvironment.vars) ? <table className="table table-bordered table-hover">
                              <thead>
                                  <tr>
                                      <th style={{width: "20%"}}>
                                          Variable
                                      </th>
                                      <th>
                                          Value
                                      </th>
                                  </tr>
                              </thead>
                              <tbody>
                                {
                                    currentEnvironment.vars.map((varEntry) => (<tr><td>{varEntry.key}</td><td>{varEntry.value}</td></tr>))
                                }
                              </tbody>
                          </table> : "No Variables"}
                      </div>
                    </Popover>)
        return (
            <OverlayTrigger trigger="click" placement="bottom" overlay={envPopover} rootClose>
                <span title="Environment quick look" className="btn btn-sm cube-btn text-center" onClick={() => {}}>
                    <i className="fas fa-eye"/>
                </span>
            </OverlayTrigger>)
    }

    hideEnvModal = () => {
        this.setState({showEnvVarModal: false})
    }

    render() {
        const { cube } = this.props;
        const { app, cubeRunHistory, userCollections, collectionName, collectionLabel, modalErroSaveMessage, modalErroCreateCollectionMessage, showEnvVarModal } = this.state;

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
                                {Object.keys(cubeRunHistory).map((k, i) => {
                                    return (
                                        <Panel key={k + "_" + i} id="collapsible-panel-example-2" defaultExpanded>
                                            <Panel.Heading style={{paddingLeft: "9px"}}>
                                                <Panel.Title toggle style={{fontSize: "13px"}}>
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
                                                                decorators={{...decorators, Header: this.renderTreeNodeHeader, Container: this.renderTreeNodeContainer, Toggle: this.renderTreeNodeToggle}}
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
                                                <Panel.Title toggle style={{fontSize: "13px"}}>
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
                                                                decorators={{...decorators, Header: this.renderTreeNodeHeader, Container: this.renderTreeNodeContainer, Toggle: this.renderTreeNodeToggle}}
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

                    {/* <div>
                        <FormGroup>
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                type="text"
                                placeholder="Search"
                            />
                        </FormGroup>
                    </div> */}
                    <div style={{marginRight: "7px"}}>
                        <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}></div>
                        <div style={{display: "inline-block", width: "80%", textAlign: "right"}}>
                                <div style={{display: "inline-block", padding: 0}} className="btn">{this.renderEnvListDD()}</div>
                                {this.renderEnvPopoverBtn()}
                                <span className="btn btn-sm cube-btn text-center" onClick={() => {this.setState({showEnvVarModal: true})}} title="Configure environments"><i className="fas fa-cog"/> </span>
                            {/* <div style={{display: "inline-block", margin: "10px" }}>
                            </div> */}
                        </div>
                    </div>
                    <div style={{marginTop: "10px", display: ""}}>
                        <ResponsiveTabs 
                            showMore={true} 
                            allowRemove={true} 
                            onAddClick={this.addTab}
                            removeActiveOnly={false} 
                            onChange={this.handleTabChange} 
                            onRemove={this.handleRemoveTab}
                            items={this.getTabs(this.state.tabs)} 
                            tabsWrapperClass={"md-hc-tabs-wrapper"} 
                            selectedTabKey={this.state.selectedTabKey} 
                        />
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
                                <p style={{fontWeight: 500}}>{modalErroCreateCollectionMessage}</p>
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
                                <p style={{marginTop: "10px", fontWeight: 500}}>{modalErroSaveMessage}</p>
                            </Modal.Body>
                            <Modal.Footer>
                                <Button onClick={this.handleSave}>Save</Button>
                                <Button onClick={this.handleCloseModal}>Close</Button>
                            </Modal.Footer>
                        </Modal>
                        <Modal show={showEnvVarModal} onHide={this.hideEnvModal}>
                            <EnvVar hideModal={this.hideEnvModal}/>
                        </Modal>
                    </div>
                </main>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const {cube, apiCatalog} = state;
    return {
        cube,
        apiCatalog,
    }
}

const connectedHttpClientTabs = connect(mapStateToProps)(HttpClientTabs);

export default connectedHttpClientTabs
