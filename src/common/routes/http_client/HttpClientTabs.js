import  React , { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Glyphicon } from 'react-bootstrap';

import _ from 'lodash';
import { stringify } from 'query-string';

import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";
import { cubeService } from "../../services";

import HttpClient from "./HttpClient";
import Tabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";

class HttpClientTabs extends Component {

    constructor(props) {
        super(props);
        const tabId = _.uniqueId('key_');
        const requestId = _.uniqueId('request_');
        this.state = { 
            tabs: [{ 
                id: tabId,
                requestId: requestId,
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
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                responseBodyType: "json"
            }],
            selectedTabKey: tabId
        };
        this.addTab = this.addTab.bind(this);
        this.handleTabChange = this.handleTabChange.bind(this);
        this.handleRemoveTab = this.handleRemoveTab.bind(this);

        this.addOrRemoveParam = this.addOrRemoveParam.bind(this);
        this.updateParam = this.updateParam.bind(this);
        this.updateBodyOrRawDataType = this.updateBodyOrRawDataType.bind(this);

        this.driveRequest = this.driveRequest.bind(this);
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
                            id: _.uniqueId('key_'),
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

    extractHeaders(httpReqestHeaders) {
        let headers = new Headers();
        headers.delete('Content-Type');
        httpReqestHeaders.forEach(each => {
            if(each.name && each.value && each.name.indexOf(":") < 0) headers.append(each.name, each.value);
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

        const httpRequestQueryStringParams = stringify(this.extractQueryStringParams(queryStringParams));
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
        let fetchURL = httpRequestURL + (httpRequestQueryStringParams ? "?" + httpRequestQueryStringParams : "");
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
                        eachTab["responseStatus"] = responseStatus + " " + responseStatusText;
                    }
                    return eachTab; 
                })
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
    };

    addTab(evt, reqObject) {
        const tabId = _.uniqueId('key_');
        const requestId = _.uniqueId('request_');
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
                responseHeaders: "",
                responseBody: "",
                recordedResponseHeaders: "",
                recordedResponseBody: "",
                responseBodyType: ""
            };
        }
        this.setState({
            tabs: [...this.state["tabs"], {
                id: tabId,
                requestId: requestId,
                tabName: reqObject.httpURL ? reqObject.httpURL : "New",
                ...reqObject
            }],
            selectedTabKey: tabId
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

        let urlParameters = new URLSearchParams(window.location.search);
        
        const requestIds = this.getRequestIds(urlParameters), selectedApp = urlParameters.get("app"), reqIdArray = Object.keys(requestIds);
        if(reqIdArray && reqIdArray.length > 0) {
            const eventTypes = [];
            cubeService.fetchAPIEventData(selectedApp, reqIdArray, eventTypes).then((result) => {
                if(result && result.numResults > 0) {
                    for(let eachReqId of reqIdArray) {
                        const reqResPair = result.objects.filter(eachReq => eachReq.reqId === eachReqId);
                        if(reqResPair.length === 2) {
                            const httpRequestEventTypeIndex = reqResPair[0].eventType === "HTTPRequest" ? 0 : 1;
                            const httpResponseEventTypeIndex = httpRequestEventTypeIndex === 0 ? 1 : 0;
                            const httpRequestEvent = reqResPair[httpRequestEventTypeIndex];
                            const httpResponseEvent = reqResPair[httpResponseEventTypeIndex];
                            let headers = [], queryParams = [], formData = [];
                            for(let eachHeader in httpRequestEvent.payload[1].hdrs) {
                                headers.push({
                                    id: _.uniqueId('key_'),
                                    name: eachHeader,
                                    value: httpRequestEvent.payload[1].hdrs[eachHeader].join(","),
                                    description: ""
                                });
                            }
                            for(let eachQueryParam in httpRequestEvent.payload[1].queryParams) {
                                queryParams.push({
                                    id: _.uniqueId('key_'),
                                    name: eachQueryParam,
                                    value: httpRequestEvent.payload[1].queryParams[eachQueryParam].join(","),
                                    description: ""
                                });
                            }
                            for(let eachFormParam in httpRequestEvent.payload[1].formParams) {
                                formData.push({
                                    id: _.uniqueId('key_'),
                                    name: eachFormParam,
                                    value: httpRequestEvent.payload[1].formParams[eachFormParam].join(","),
                                    description: ""
                                });
                            }
                            let reqObject = {
                                httpMethod: httpRequestEvent.payload[1].method.toLowerCase(),
                                httpURL: httpRequestEvent.payload[1].path,
                                headers: headers,
                                queryStringParams: queryParams,
                                bodyType: "formData",
                                formData: formData,
                                rawData: "",
                                rawDataType: "json",
                                responseStatus: "NA",
                                responseHeaders: "",
                                responseBody: "",
                                recordedResponseHeaders: JSON.stringify(httpResponseEvent.payload[1].hdrs, undefined, 4),
                                recordedResponseBody: httpResponseEvent.payload[1].body ? JSON.stringify(httpResponseEvent.payload[1].body, undefined, 4) : "",
                                responseBodyType: ""
                            };
                            const mockEvent = {};
                            this.addTab(mockEvent, reqObject);
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

    getTabs() {
        return this.state.tabs.map((eachTab, index) => ({
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
                        responseHeaders={eachTab.responseHeaders}
                        responseBody={eachTab.responseBody}
                        recordedResponseHeaders={eachTab.recordedResponseHeaders}
                        recordedResponseBody={eachTab.recordedResponseBody}
                        responseBodyType={eachTab.responseBodyType} >

                        </HttpClient>
                    </div>
              )},
            /* Optional parameters */
            key: eachTab.id,
            tabClassName: 'md-hc-tab',
            panelClassName: 'md-hc-tab-panel'
        }));
    }

    render() {
        const { cube } = this.props;
        return (
            <div className="content-wrapper">
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
                        <div className="btn btn-sm cube-btn text-center" style={{}} onClick={this.addTab}>
                        <Glyphicon glyph="plus" /> ADD TAB
                        </div>
                    </div>
                </div>
                <div style={{marginTop: "10px"}}>
                    <Tabs items={this.getTabs()} tabsWrapperClass={"md-hc-tabs-wrapper"} allowRemove={true} removeActiveOnly={false} showMore={true} selectedTabKey={this.state.selectedTabKey} onChange={this.handleTabChange} onRemove={this.handleRemoveTab} />
                </div>
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
