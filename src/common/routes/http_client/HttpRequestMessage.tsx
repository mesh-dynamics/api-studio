import React, { Component } from 'react';
import { FormGroup, FormControl, OverlayTrigger, Tooltip } from 'react-bootstrap';
import _ from 'lodash';

// import "./styles_here.css";

import HttpRequestHeaders, {IHttpRequestHeadersProps} from "./HttpRequestHeaders";
import HttpRequestQueryString, {IHttpRequestQueryStringProps} from "./HttpRequestQueryString";
import HttpRequestBody, {IHttpRequestBody} from "./HttpRequestBody";
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';
import { applyEnvVarsToUrl } from "../../utils/http_client/envvar";
import { UpdateBodyOrRawDataTypeHandler } from './HttpResponseHeaders';

export interface IHttpRequestMessageProps extends IHttpRequestHeadersProps, IHttpRequestQueryStringProps, IHttpRequestBody{
    bodyType: string;
    httpMethod: string;
    httpURL: string;
    paramsType: string;
    rawDataType: string;
    updateBodyOrRawDataType: UpdateBodyOrRawDataTypeHandler
}


export interface IHttpRequestMessageState{
    showFormData: boolean;
    showRawData: boolean;
}

class HttpRequestMessage extends Component<IHttpRequestMessageProps, IHttpRequestMessageState> {
    constructor(props: IHttpRequestMessageProps) {
        super(props);
        this.state = {
            showFormData: this.props.bodyType === "formData",
            showRawData: this.props.bodyType === "rawData"
        };
        this.onChangeValue = this.onChangeValue.bind(this);
        this.handleChange = this.handleChange.bind(this);
        this.handleBodyOrRawDataType = this.handleBodyOrRawDataType.bind(this);
    }

    handleChange(evt) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, evt.target.name, evt.target.name, evt.target.value);
    }

    onChangeValue(evt) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, evt.target.name, evt.target.name, evt.target.value);
    }

    handleBodyOrRawDataType(event) {
        const { tabId, isOutgoingRequest } = this.props;
        const typeToUpdate = event.target.name === "bodyType" ? "bodyType" : "rawDataType";
        this.props.updateBodyOrRawDataType(isOutgoingRequest, tabId, typeToUpdate === "bodyType" ? "bodyType" : "rawDataType", event.target.value);
        if(typeToUpdate === "bodyType") {
            this.setState({
                showFormData: event.target.value === "formData",
                showRawData: event.target.value === "rawData"
            });
        }
    }
 

    generateUrlTooltip = (url) => {
        let urlRendered = url;
        let err = "";
        try {
            urlRendered = applyEnvVarsToUrl(url)
        } catch (e) {
            err = e.toString()
        }

        return urlRendered ? <div>
                    <p style={{fontSize:12}}>{urlRendered}</p>
                        {err && <p style={{fontSize: 9, color: "red"}}>{err}</p>}
                </div>
            : null;
    
    }

    render() {
        const urlRendered = this.generateUrlTooltip(this.props.httpURL);
        
        const urlTextBox = <div style={{display: "inline-block", width: "82%"}}>
            <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                <FormControl type="text" placeholder="https://...." style={{fontSize: "12px"}} name="httpURL" value={this.props.httpURL} onChange={this.handleChange}/>
            </FormGroup>
        </div>

        return (
            <>
                <div style={{marginRight: "7px"}}>
                    <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}>REQUEST</div>
                </div>
                
                <div style={{marginBottom: "0px"}}>
                    <div style={{display: "inline-block", width: "18%", paddingRight: "15px"}}> 
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
                    {urlRendered ? <Tippy content={urlRendered} arrow={false} arrowType="round" interactive={true} theme={"google"} size="large" placement="bottom-start">
                        {urlTextBox}
                    </Tippy> : urlTextBox}
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "12px"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        VIEW
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showHeaders" name="paramsType" checked={this.props.paramsType === "showHeaders"} onChange={this.onChangeValue}/>
                            Headers
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio"  style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showQueryParams" name="paramsType" checked={this.props.paramsType === "showQueryParams"} onChange={this.onChangeValue}/>
                            Query Params
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <input type="radio"  style={{marginTop: "0px", marginRight: "7px"}} 
                            value="showBody" name="paramsType" checked={this.props.paramsType === "showBody"} onChange={this.onChangeValue}/>
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
                <HttpRequestHeaders tabId={this.props.tabId}
                    showHeaders={this.props.paramsType === "showHeaders"} 
                    headers={this.props.headers} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestHeaders>
                <HttpRequestQueryString tabId={this.props.tabId}
                    showQueryParams={this.props.paramsType === "showQueryParams"} 
                    queryStringParams={this.props.queryStringParams} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestQueryString>
                <HttpRequestBody tabId={this.props.tabId}
                    showBody={this.props.paramsType === "showBody"}
                    showFormData={this.state.showFormData}
                    showRawData={this.state.showRawData}
                    formData={this.props.formData} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    updateAllParams={this.props.updateAllParams}
                    rawData={this.props.rawData}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestBody>
            </>
        );
    }
}

export default HttpRequestMessage;