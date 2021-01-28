import React, { Component } from 'react';
import { FormGroup, FormControl, OverlayTrigger, Tooltip } from 'react-bootstrap';
import _ from 'lodash';
import classNames from 'classnames';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';
import { applyEnvVarsToUrl } from "../../utils/http_client/envvar";
import { UpdateBodyOrRawDataTypeHandler, UpdateParamHandler, ReplaceAllParamsHandler } from './HttpResponseHeaders';
import {generateUrlWithQueryParams, extractURLQueryParams} from "./../../utils/http_client/utils"
import { IRequestParamData } from '../../reducers/state.types';
export interface IHttpRequestMessageProps {
    bodyType: string;
    httpMethod: string;
    httpURL: string;
    paramsType: string;
    rawDataType: string;
    updateBodyOrRawDataType: UpdateBodyOrRawDataTypeHandler;
    id: string;
    rawData: string;
    grpcData: any;
    readOnly: boolean;
    tabId: string, 
    isOutgoingRequest : boolean;
    headers: IRequestParamData[]; 
    formData: IRequestParamData[];
    multipartData: IRequestParamData[];
    updateParam: UpdateParamHandler;
    replaceAllParams: ReplaceAllParamsHandler;
    disabled: boolean;
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
        const typeToUpdate = event.target.name === "bodyType"+this.props.id.trim() ? "bodyType" : "rawDataType";
        this.props.updateBodyOrRawDataType(isOutgoingRequest, tabId, typeToUpdate === "bodyType" ? "bodyType" : "rawDataType", event.target.value);
        if(typeToUpdate === "bodyType") {
            this.setState({
                showFormData: event.target.value === "formData",
                showRawData: event.target.value === "rawData"
            });
        }
    }
 

    generateUrlTooltip = (url: string) => {
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
            : <></>;
    
    }

    onTippyShow =(instance: any) => {  
        if(instance.props.content == null || instance.props.content.innerText == ""){ 
            return false 
        } 
        return;
    };

    handleURLChange = (evt) => {
        const { tabId, isOutgoingRequest, queryStringParams } = this.props;
        const {httpURL, queryParamsFromUrl} = extractURLQueryParams(evt.target.value)
        const queryStringParamsUnselected = _.filter(queryStringParams, {selected: false})
        const queryParams = queryStringParamsUnselected.concat(queryParamsFromUrl)
        this.props.updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", httpURL);
        this.props.replaceAllParams(isOutgoingRequest, tabId, "queryStringParams", queryParams)
    }

    render() {
        const {httpURL, queryStringParams} = this.props;
        const urlWithQueryParams = generateUrlWithQueryParams(httpURL, queryStringParams)
        const urlRendered = this.generateUrlTooltip(urlWithQueryParams);
        
        const urlTextBox = <div style={{display: "inline-block", width: "82%"}}>
            <FormGroup bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                <FormControl type="text" placeholder="https://...." style={{fontSize: "12px"}} readOnly={this.props.readOnly} disabled={this.props.disabled} name="httpURL" value={urlWithQueryParams} onChange={this.handleURLChange}/>
            </FormGroup>
        </div>

        const headerLabelClass = classNames({
            "request-data-label": true,
            "filled": this.props.headers.findIndex( header => header.name !== '') > -1
        });
        const queryParamLabelClass = classNames({
            "request-data-label": true,
            "filled": this.props.queryStringParams.findIndex( queryString => queryString.name !== '') > -1
        });
        const isRawDataHighlighted = this.props.rawData && this.props.rawData.trim();
        const isFormDataExists =this.props.formData.findIndex( header => header.name !== '') > -1;
        const isMultipartDataExists =this.props.multipartData?.findIndex( header => header.name !== '') > -1;
        
        const bodyLabelClass = classNames({
            "request-data-label": true,
            "filled": isFormDataExists || isRawDataHighlighted || isMultipartDataExists
        }); 
        const isgRPCData = this.props.bodyType == "grpcData" && this.props.paramsType == "showBody";
        const httpMethod = isgRPCData ? "post" : this.props.httpMethod;
        

        return (
            <>
                <div style={{marginRight: "7px"}}>
                    <div style={{marginBottom: "9px", display: "inline-block", width: "20%", fontSize: "11px"}}>REQUEST</div>
                </div>
                
                <div style={{marginBottom: "0px"}}>
                    <div style={{display: "inline-block", width: "18%", paddingRight: "15px"}}> 
                        <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} name="httpMethod" 
                                readOnly={this.props.readOnly || isgRPCData} value={httpMethod} onChange={this.handleChange}>
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
                    <Tippy content={urlRendered} arrow={false} arrowType="round" enabled={!this.props.disabled} interactive={true} theme={"google"} size="large" placement="bottom-start" onShow={this.onTippyShow}>
                        {urlTextBox}
                    </Tippy>
                </div>
                <div className="" style={{marginTop: "18px", marginBottom: "5px"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        VIEW
                    </div>
                    <div className={headerLabelClass}>
                        <input type="radio" disabled={this.props.disabled} 
                            value="showHeaders" name={"paramsType"+this.props.id.trim()} checked={this.props.paramsType === "showHeaders"} onChange={this.onChangeValue}/>
                            Headers
                    </div>
                    <div className={queryParamLabelClass}>
                        <input type="radio" disabled={this.props.disabled} 
                            value="showQueryParams" name={"paramsType"+this.props.id.trim()}  checked={this.props.paramsType === "showQueryParams"} onChange={this.onChangeValue}/>
                            Query Params
                    </div>
                    <div className={bodyLabelClass}>
                        <input type="radio" disabled={this.props.disabled} 
                            value="showBody" name={"paramsType"+this.props.id.trim()}  checked={this.props.paramsType === "showBody"} onChange={this.onChangeValue}/>
                            Body
                    </div>
                </div>
            </>
        );
    }
}

export default HttpRequestMessage;