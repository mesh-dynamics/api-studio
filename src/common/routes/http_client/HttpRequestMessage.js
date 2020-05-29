import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
import _ from 'lodash';

// import "./styles_here.css";

import HttpRequestHeaders from "./HttpRequestHeaders";
import HttpRequestQueryString from "./HttpRequestQueryString";
import HttpRequestBody from "./HttpRequestBody";
import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestRawData from "./HttpRequestRawData";

class HttpRequestMessage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showHeaders: true,
            showQueryParams: false,
            showBody: false,
            showFormData: this.props.bodyType === "formData",
            showRawData: this.props.bodyType === "rawData"
        };
        this.onChangeValue = this.onChangeValue.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleBodyOrRawDataType = this.handleBodyOrRawDataType.bind(this);
    }

    handleChange(evt) {
        this.props.updateParam(evt.target.name, evt.target.name, evt.target.value);
    }

    onChangeValue(event) {
        this.setState({
            showHeaders: event.target.value === "showHeaders",
            showQueryParams: event.target.value === "showQueryParams",
            showBody: event.target.value === "showBody"
        });
    }

    handleBodyOrRawDataType(event) {
        const typeToUpdate = event.target.name === "bodyType" ? "bodyType" : "rawDataType";
        this.props.updateBodyOrRawDataType(typeToUpdate === "bodyType" ? "bodyType" : "rawDataType", event.target.value);
        if(typeToUpdate === "bodyType") {
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
                    <div style={{display: "inline-block", width: "80%", textAlign: "right"}}>
                        <div className="btn btn-sm cube-btn text-center" style={{ padding: "2px 10px"}} onClick={this.props.driveRequest}>
                        <Glyphicon glyph="play" /> RUN
                        </div>
                    </div>
                </div>
                
                <div style={{marginBottom: "0px"}}>
                    <div style={{display: "inline-block", width: "10%", paddingRight: "15px"}}> 
                        <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} name="httpMethod" value={this.props.httpMethod} onChange={this.handleChange}>
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
                    <div style={{display: "inline-block", width: "89%"}}>
                        <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                            <FormControl type="text" placeholder="https://...." style={{fontSize: "12px"}} name="httpURL" value={this.props.httpURL} onChange={this.handleChange} />
                        </FormGroup>
                    </div>
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "12px"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        VIEW
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showHeaders" name="paramsType" checked={this.state.showHeaders} onChange={this.onChangeValue}/>
                            Headers
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" checked style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showQueryParams" name="paramsType" checked={this.state.showQueryParams} onChange={this.onChangeValue}/>
                            Query Params
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" checked style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showBody" name="paramsType" checked={this.state.showBody} onChange={this.onChangeValue}/>
                            Body
                    </div>
                    
                    {/* <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} />Binary Data
                    </div> */}
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "12px", display: this.state.showBody ? "" : "none"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        BODY
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="formData" name="bodyType" checked={this.state.showFormData} onChange={this.handleBodyOrRawDataType}/>
                            x-www-form-urlencoded
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="rawData" name="bodyType" checked={this.state.showRawData} onChange={this.handleBodyOrRawDataType}/>
                            Raw Data
                    </div>
                    <div className="" style={{display: this.state.showRawData ? "inline-block" : "none", paddingRight: "25px", fontSize: "12px"}}>
                        <FormGroup bsSize="small">
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} name="rawDataType" value={this.props.rawDataType} onChange={this.handleBodyOrRawDataType}>
                                <option value="txt">Text</option>
                                <option value="js">JavaScript</option>
                                <option value="json">JSON</option>
                                <option value="html">HTML</option>
                                <option value="xml">XML</option>
                            </FormControl>
                        </FormGroup>
                    </div>
                </div>
                <HttpRequestHeaders showHeaders={this.state.showHeaders} 
                    headers={this.props.headers} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam} >

                </HttpRequestHeaders>
                <HttpRequestQueryString showQueryParams={this.state.showQueryParams} 
                    queryStringParams={this.props.queryStringParams} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam} >

                </HttpRequestQueryString>
                <HttpRequestBody showBody={this.state.showBody}
                    showFormData={this.state.showFormData}
                    showRawData={this.state.showRawData}
                    formData={this.props.formData} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    rawData={this.props.rawData} >

                </HttpRequestBody>
            </div>
        );
    }
}

export default HttpRequestMessage;