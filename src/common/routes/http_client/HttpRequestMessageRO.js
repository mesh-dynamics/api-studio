import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
import _ from 'lodash';

// import "./styles_here.css";

import HttpRequestHeadersRO from "./HttpRequestHeadersRO";
import HttpRequestQueryStringRO from "./HttpRequestQueryStringRO";
import HttpRequestBodyRO from "./HttpRequestBodyRO";
import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestRawData from "./HttpRequestRawData";

class HttpRequestMessageRO extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showFormData: this.props.bodyType === "formData",
            showRawData: this.props.bodyType === "rawData"
        };
        this.handleBodyOrRawDataType = this.handleBodyOrRawDataType.bind(this);
    }

    handleBodyOrRawDataType(event) {
        const { tabId, isOutgoingRequest } = this.props;
        const typeToUpdate = event.target.name === "bodyTypeRO" ? "bodyTypeRO" : "rawDataTypeRO";
        if(typeToUpdate === "bodyTypeRO") {
            this.setState({
                showFormData: event.target.value === "formData",
                showRawData: event.target.value === "rawData"
            });
        }
    }
 

    render() {
        return (
            <div>
                <div style={{marginRight: "7px"}}>
                    <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}>REQUEST</div>
                </div>
                
                <div style={{marginBottom: "0px"}}>
                    <div style={{display: "inline-block", width: "18%", paddingRight: "15px"}}> 
                        <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} disabled name="httpMethod" value={this.props.httpMethod} >
                                <option value="get">GET</option>
                                <option value="post">POST</option>
                                <option value="put">PUT</option>
                                <option value="patch">PATCH</option>
                                <option value="delete">DELETE</option>
                                <option value="copy">COPY</option>
                                <option value="head">HEAD</option>
                                <option value="options">OPTIONS</option>
                                <option value="link">LINK</option>
                                <option value="unlink">UNLINK</option>
                                <option value="purge">PURGE</option>
                                <option value="lock">LOCK</option>
                                <option value="unlock">UNLOCK</option>
                                <option value="propfind">PROPFIND</option>
                                <option value="view">VIEW</option>
                            </FormControl>
                        </FormGroup>
                    </div>
                    <div style={{display: "inline-block", width: "82%"}}>
                        <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                            <FormControl type="text" placeholder="https://...." style={{fontSize: "12px"}} disabled name="httpURL" value={this.props.httpURL} />
                        </FormGroup>
                    </div>
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "12px"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        VIEW
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} disabled 
                            value="showHeaders" name="paramsTypeRO" checked={this.props.paramsType === "showHeaders"} />
                            Headers
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" checked style={{marginTop: "0px", marginRight: "7px"}} disabled 
                            value="showQueryParams" name="paramsTypeRO" checked={this.props.paramsType === "showQueryParams"} />
                            Query Params
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" checked style={{marginTop: "0px", marginRight: "7px"}} disabled 
                            value="showBody" name="paramsTypeRO" checked={this.props.paramsType === "showBody"} />
                            Body
                    </div>
                    
                    {/* <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} />Binary Data
                    </div> */}
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "12px", display: this.props.paramsType === "showBody" ? "" : "none"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        BODY
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="formData" name="bodyTypeRO" checked={this.state.showFormData} onChange={this.handleBodyOrRawDataType}/>
                            x-www-form-urlencoded
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="rawData" name="bodyTypeRO" checked={this.state.showRawData} onChange={this.handleBodyOrRawDataType}/>
                            Raw Data
                    </div>
                    <div className="" style={{display: this.state.showRawData ? "inline-block" : "none", paddingRight: "25px", fontSize: "12px"}}>
                        <FormGroup bsSize="small">
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} name="rawDataTypeRO" value={this.props.rawDataType} onChange={this.handleBodyOrRawDataType}>
                                <option value="txt">Text</option>
                                <option value="js">JavaScript</option>
                                <option value="json">JSON</option>
                                <option value="html">HTML</option>
                                <option value="xml">XML</option>
                            </FormControl>
                        </FormGroup>
                    </div>
                </div>
                <HttpRequestHeadersRO tabId={this.props.tabId}
                    showHeaders={this.props.paramsType === "showHeaders"} 
                    headers={this.props.headers} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestHeadersRO>
                <HttpRequestQueryStringRO tabId={this.props.tabId}
                    showQueryParams={this.props.paramsType === "showQueryParams"} 
                    queryStringParams={this.props.queryStringParams} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestQueryStringRO>
                <HttpRequestBodyRO tabId={this.props.tabId}
                    showBody={this.props.paramsType === "showBody"}
                    showFormData={this.state.showFormData}
                    showRawData={this.state.showRawData}
                    formData={this.props.formData} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    rawData={this.props.rawData}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestBodyRO>
            </div>
        );
    }
}

export default HttpRequestMessageRO;