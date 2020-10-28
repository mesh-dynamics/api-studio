import React, { ChangeEvent, Component, FormEvent } from 'react';
import { FormGroup, FormControl } from 'react-bootstrap';
import _ from 'lodash';
import classNames from 'classnames';

// import "./styles_here.css";

import HttpRequestHeadersRO, {IHttpRequestHeadersROProps} from "./HttpRequestHeadersRO";
import HttpRequestQueryStringRO, {IHttpRequestQueryStringROProps} from "./HttpRequestQueryStringRO";
import HttpRequestBodyRO, {IHttpRequestBodyROProps} from "./HttpRequestBodyRO";

export interface IHttpRequestMessageROProps extends IHttpRequestBodyROProps, IHttpRequestHeadersROProps, IHttpRequestQueryStringROProps{
    bodyType: string;
    httpMethod: string;
    httpURL: string;
    paramsType: string;
    rawDataType: string;
}
export interface IHttpRequestMessageROState{
    showFormData: boolean;
    showRawData: boolean;
}

class HttpRequestMessageRO extends Component<IHttpRequestMessageROProps, IHttpRequestMessageROState> {
    constructor(props: IHttpRequestMessageROProps) {
        super(props);
        this.state = {
            showFormData: this.props.bodyType === "formData",
            showRawData: this.props.bodyType === "rawData"
        };
        this.handleBodyOrRawDataType = this.handleBodyOrRawDataType.bind(this);
    }

    handleBodyOrRawDataType(event: ChangeEvent<HTMLInputElement>) {
        const typeToUpdate = event.target.name === "bodyTypeRO" ? "bodyTypeRO" : "rawDataTypeRO";
        if(typeToUpdate === "bodyTypeRO") {
            this.setState({
                showFormData: event.target.value === "formData",
                showRawData: event.target.value === "rawData"
            });
        }
    }
 

    render() {

        const headerLabelClass = classNames({
            "request-data-label": true,
            "filled": this.props.headers.findIndex( header => header.name !== '') > -1
        });
        const queryParamLabelClass = classNames({
            "request-data-label": true,
            "filled": this.props.queryStringParams.findIndex( queryString => queryString.name !== '') > -1
        });
        const isRawDataHighlighted = this.props.rawData && this.props.rawData.trim();
        const rawDataLabelClass = classNames({
            "request-data-label": true,
            "filled": isRawDataHighlighted
        });
        const isFormDataExists =this.props.formData.findIndex( header => header.name !== '') > -1;
        const formDataLabelClass = classNames({
            "request-data-label": true,
            "filled": isFormDataExists
        });
        
        const bodyLabelClass = classNames({
            "request-data-label": true,
            "filled": isFormDataExists || isRawDataHighlighted
        });
        


        return (
            <>
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
                    <div className={headerLabelClass}>
                        <input type="radio" disabled 
                            value="showHeaders" name="paramsTypeRO" checked={this.props.paramsType === "showHeaders"} />
                            Headers
                    </div>
                    <div className={queryParamLabelClass}>
                        <input type="radio" disabled 
                            value="showQueryParams" name="paramsTypeRO" checked={this.props.paramsType === "showQueryParams"} />
                            Query Params
                    </div>
                    <div className={bodyLabelClass}>
                        <input type="radio" disabled 
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
                    <div className={formDataLabelClass}>
                        <input type="radio"
                            value="formData" name="bodyTypeRO" checked={this.state.showFormData} onChange={this.handleBodyOrRawDataType}/>
                            x-www-form-urlencoded
                    </div>
                    <div className={rawDataLabelClass}>
                        <input type="radio"
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
                <HttpRequestHeadersRO 
                    showHeaders={this.props.paramsType === "showHeaders"} 
                    headers={this.props.headers}  >

                </HttpRequestHeadersRO>
                <HttpRequestQueryStringRO 
                    showQueryParams={this.props.paramsType === "showQueryParams"} 
                    queryStringParams={this.props.queryStringParams} 
                     >

                </HttpRequestQueryStringRO>
                <HttpRequestBodyRO 
                    showBody={this.props.paramsType === "showBody"}
                    showFormData={this.state.showFormData}
                    showRawData={this.state.showRawData}
                    formData={this.props.formData} 
                     
                    rawData={this.props.rawData}  >

                </HttpRequestBodyRO>
            </>
        );
    }
}

export default HttpRequestMessageRO;