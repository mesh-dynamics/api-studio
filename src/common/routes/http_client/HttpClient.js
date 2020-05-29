import  React , { Component, Fragment, createContext } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { FormControl, FormGroup, Glyphicon } from 'react-bootstrap';

import _ from 'lodash';
import { stringify } from 'query-string';

import Tabs from '../../components/Tabs';
// IMPORTANT you need to include the default styles
import '../../components/Tabs/styles.css';
// import "./HttpClient.css";
import "./Tabs.css";

import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";

import HttpRequestMessage from "./HttpRequestMessage";
import HttpResponseMessage from "./HttpResponseMessage";

class HttpClient extends Component {

    constructor(props) {
        super(props);
        this.state = { 
            httpMethod: "get",
            httpURL: "https://www.mocky.io/v2/5185415ba171ea3a00704eed",
            headers: [],
            queryStringParams: [],
            bodyType: "formData",
            formData: [],
            rawData: "",
            rawDataType: "json",
            responseStatus: "",
            responseHeaders: "",
            responseBody: "",
            tabs: [{ requestId: 'https://www.mocky.io/v2/5185415ba171ea3a00704eed' }]
        };
        this.addTab = this.addTab.bind(this);
        this.addOrRemoveParam = this.addOrRemoveParam.bind(this);
        this.updateParam = this.updateParam.bind(this);
        this.updateBodyOrRawDataType = this.updateBodyOrRawDataType.bind(this);
        this.driveRequest = this.driveRequest.bind(this);
    }

    addTab() {
        this.setState({
            tabs: [...this.state["tabs"], {
                id: _.uniqueId('key_'),
                requestId: "New"
            }]
        });
    }

    addOrRemoveParam(type, op, id) {
        if(op === "delete") {
            this.setState({
                [type]: this.state[type].filter((e) => e.id !== id)
            });
        } else {
            this.setState({
                [type]: [...this.state[type], {
                    id: _.uniqueId('key_'),
                    name: "",
                    value: "",
                    description: ""
                }]
            });

        }
    }

    updateParam(type, key, value, id) {
        let params = this.state[type];
        if(_.isArray(params)) {
            let specificParamArr = params.filter((e) => e.id === id);
            if(specificParamArr.length > 0) {
                specificParamArr[0][key] = value;
            }
        } else {
            params = value;
        }
        this.setState({[type]: params})
    }

    updateBodyOrRawDataType(type, value) {
        this.setState({[type]: value});
    }

    extractHeaders(httpReqestHeaders) {
        let headers = new Headers();
        headers.delete('Content-Type');
        httpReqestHeaders.forEach(each => {
            if(each.name && each.value) headers.append(each.name, each.value);
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

    driveRequest() {
        // make the request and update response status, headers & body
        // extract headers
        // extract body
        const { headers, queryStringParams, bodyType, rawDataType } = this.state;
        const httpReqestHeaders = this.extractHeaders(headers);

        const httpRequestQueryStringParams = stringify(this.extractQueryStringParams(queryStringParams));
        let httpRequestBody;
        if(bodyType === "formData") {
            const { formData } = this.state;
            httpRequestBody = this.extractBody(formData);
        }
        if(bodyType === "rawData") {
            const { rawData } = this.state;
            httpRequestBody = this.extractBody(rawData);
        }
        const httpMethod = this.state.httpMethod;
        const httpRequestURL = this.state.httpURL;

        let fetchConfig = {
            method: httpMethod,
            headers: httpReqestHeaders
        }
        if(httpMethod !== "GET".toLowerCase() && httpMethod !== "HEAD".toLowerCase()) {
            fetchConfig["body"] = httpRequestBody;
        }
        let fetchURL = httpRequestURL + (httpRequestQueryStringParams ? "?" + httpRequestQueryStringParams : "");
        
        // Make request
        // https://www.mocky.io/v2/5185415ba171ea3a00704eed
        let fetchedResponseHeaders = {};
        return fetch(fetchURL, fetchConfig).then((response) => {
                for(const header of response.headers){
                    fetchedResponseHeaders[header[0]] = header[1];
                }
                if (response.headers.get("content-type").indexOf("application/json") !== -1) {// checking response header
                    return response.json();
                } else {
                    throw new TypeError('Response from has unexpected "content-type"');
                }
            })
            .then((data) => {
                // handle success
                this.setState({
                    responseHeaders: JSON.stringify(fetchedResponseHeaders, undefined, 4),
                    responseBody: JSON.stringify(data, undefined, 4)
                });
            })
            .catch((error) => {
                console.error(error.message);
            });
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(false));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(true));
    }

    getTabs() {
        return this.state.tabs.map((eachRun, index) => ({
            title: (
                <div className="tab-container">
                  <div className="tab-name">{eachRun.requestId}</div>
                </div>
              ),
            getContent: () => (
                <div className="tab-container">
                  <HttpRequestMessage httpMethod={this.state.httpMethod} httpURL={this.state.httpURL}
                    headers={this.state.headers} 
                    queryStringParams={this.state.queryStringParams}
                    bodyType={this.state.bodyType}
                    formData={this.state.formData} 
                    rawData={this.state.rawData}
                    rawDataType={this.state.rawDataType}
                    addOrRemoveParam={this.addOrRemoveParam} 
                    updateParam={this.updateParam}
                    updateBodyOrRawDataType={this.updateBodyOrRawDataType}
                    driveRequest={this.driveRequest} >
                      {eachRun.requestId}
                  </HttpRequestMessage>
                  <HttpResponseMessage responseStatus={this.state.responseStatus}
                    responseHeaders={this.state.responseHeaders}
                    responseBody={this.state.responseBody}>
                        {eachRun.requestId}
                </HttpResponseMessage>
                </div>
              ),
            /* Optional parameters */
            key: index,
            tabClassName: 'md-hc-tab',
            panelClassName: 'md-hc-tab-panel',
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
                    <Tabs items={this.getTabs()} tabsWrapperClass={"md-hc-tabs-wrapper"} allowRemove={true} removeActiveOnly={false} showMore={true} />
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

const connectedHttpClient = connect(mapStateToProps)(HttpClient);

export default connectedHttpClient
