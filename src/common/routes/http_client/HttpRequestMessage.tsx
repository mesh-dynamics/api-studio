import React, { Component } from 'react';
import { FormGroup, FormControl, InputGroup, Button } from 'react-bootstrap';
import _ from 'lodash';
import classNames from 'classnames';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';
import { applyEnvVarsToUrl } from "../../utils/http_client/envvar";
import { UpdateBodyOrRawDataTypeHandler, UpdateParamHandler, ReplaceAllParamsHandler } from './HttpResponseHeaders';
import {generateUrlWithQueryParams, extractURLQueryParams} from "./../../utils/http_client/utils"
import { IMockConfig, IRequestParamData, IStoreState } from '../../reducers/state.types';
import HideInternalHeadersButton from './HideInternalHeadersButton';
import { connect } from 'react-redux';
import ServiceSelector from './components/ServiceSelector';
import { getDefaultServiceName, joinPaths } from '../../utils/http_client/httpClientUtils';
import { getApiPathAndServiceFromUrl } from '../../utils/http_client/httpClientTabs.utils';
import MockConfigUtils from '../../utils/http_client/mockConfigs.utils';
import AutoCompleteBox from './components/AutoCompleteBox';
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
    clientTabId: string;
    queryStringParams: IRequestParamData[];
    service: string;
    mockConfigList: IMockConfig[];
    selectedMockConfig: string;
    requestPathURL: string;
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

    componentDidUpdate(prevProps: IHttpRequestMessageProps) {
        const mockConfigUtils = new MockConfigUtils({
            selectedMockConfig: this.props.selectedMockConfig,
            mockConfigList: this.props.mockConfigList,
        });
        const currentService = mockConfigUtils.getCurrentService(this.props.service);
        const domain = currentService?.url || this.props.service;
        if (!this.props.readOnly && (prevProps.selectedMockConfig !== this.props.selectedMockConfig || (this.props.httpURL.indexOf(domain) != 0 && !this.props.isOutgoingRequest))) {
          this.handleServiceChange(this.props.service);
        }
    }

    componentDidMount() {
        this.handleServiceChange(this.props.service);
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
        this.props.updateParam(isOutgoingRequest, tabId, "httpURL", "httpURL", httpURL); //It will again replace apiPath
        this.props.replaceAllParams(isOutgoingRequest, tabId, "queryStringParams", queryParams)
    }

    showServiceSelectionSuggestion(){
        const serviceIsSelected = this.isServiceSelected();
        const { httpURL, readOnly } = this.props;
        if(!serviceIsSelected && !readOnly){
            const {isServiceMatchedFromConfig, service, targetUrl} =  getApiPathAndServiceFromUrl(httpURL);
            if(isServiceMatchedFromConfig){
                return <div key="serviceSuggestion" className="font-12">URL `{targetUrl}` matches with service <b>{service}</b>. 
                    <Button className="btn btn-sm cube-btn left margin-top-5" style={{padding: "2px 10px", fontSize : "10px"}} 
                     onClick={()=> this.handleServiceChange(service)} title={`Update service to '${service}'`}>
                         <i className="fa fa-list-alt"></i> Update</Button> 
                </div>
            }
        }
        return null;
    }

    handleServiceChange = (value:string)=> {
        this.props.updateParam(this.props.isOutgoingRequest, this.props.tabId, "service", "service",value);
    }
    handleApiPathChange = (evt)=> {
        const {httpURL: requestPathURL, queryParamsFromUrl} = extractURLQueryParams(evt.target.value);
        this.props.updateParam(this.props.isOutgoingRequest, this.props.tabId, "requestPathURL", "requestPathURL", requestPathURL);

        const { tabId, isOutgoingRequest, queryStringParams } = this.props;
        const queryStringParamsUnselected = _.filter(queryStringParams, {selected: false})
        const queryParams = queryStringParamsUnselected.concat(queryParamsFromUrl)
        this.props.replaceAllParams(isOutgoingRequest, tabId, "queryStringParams", queryParams)
    }

    isServiceSelected(){
        const defaultService = getDefaultServiceName();
        let selectedService = this.props.service || defaultService;
        return selectedService != defaultService;
    }

    renderURLBox = ()=> {
        
        const {httpURL, queryStringParams, requestPathURL} = this.props;
        const serviceIsSelected = this.isServiceSelected();
        if(!serviceIsSelected || this.props.readOnly){ 
            const urlWithQueryParams = generateUrlWithQueryParams(httpURL, queryStringParams);
            const urlRendered = this.generateUrlTooltip(urlWithQueryParams);
            const urlTextBox = (<div style={{display: "inline-block", width: "82%"}}>
                <FormGroup className="autocomplete" bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                    <AutoCompleteBox id="urlTextBox" placeholder="https://...." style={{fontSize: "12px"}} readOnly={this.props.readOnly} disabled={this.props.disabled} name="httpURL" value={urlWithQueryParams} onChange={this.handleURLChange}/>
                </FormGroup>
            </div>);

            return (<Tippy content={urlRendered} arrow={false} arrowType="round" enabled={!this.props.disabled} interactive={true} theme={"google"} size="large" placement="bottom-start" onShow={this.onTippyShow}>
            {urlTextBox}
            </Tippy>)
        }
        else{

        const pathWithQueryParams = generateUrlWithQueryParams(requestPathURL, queryStringParams); 
        const mockConfigUtils = new MockConfigUtils({
            selectedMockConfig: this.props.selectedMockConfig,
            mockConfigList: this.props.mockConfigList,
        });
        const currentService = mockConfigUtils.getCurrentService(this.props.service);
        let domain = currentService?.url || this.props.service;
        if(!domain.endsWith("/")){
            domain = domain + "/";
        }
        
        const urlRendered = this.generateUrlTooltip(joinPaths(domain, pathWithQueryParams));
              
             const urlTextBox = (<div style={{display: "inline-block", width: "82%"}}>
                <FormGroup className="autocomplete" bsSize="small" style={{marginBottom: "0px", fontSize: "12px"}}>
                <InputGroup>
                <InputGroup.Addon style={{fontSize: "11px"}}>{domain}</InputGroup.Addon>
                    <AutoCompleteBox id="apiPathURLTextBox" placeholder="/request/path?with=query" style={{fontSize: "12px"}} readOnly={this.props.readOnly} disabled={this.props.disabled} name="requestPathURL" value={pathWithQueryParams} onChange={this.handleApiPathChange}/>
                </InputGroup>
                </FormGroup>
                </div>);
            
            return (<Tippy content={urlRendered} arrow={false} arrowType="round" enabled={!this.props.disabled} interactive={true} theme={"google"} size="large" placement="bottom-start" onShow={this.onTippyShow}>
                {urlTextBox}
            </Tippy>)
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

                <div className="margin-bottom-10">
                    <div style={{display: "inline-block",  paddingRight: "15px"}}> 
                        <FormGroup bsSize="small" style={{marginBottom: "0px", display: "flex"}}>
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
                            <ServiceSelector readOnly={this.props.readOnly} selectedService={this.props.service} 
                            onChange={this.handleServiceChange} serviceSelectionSuggestion={this.showServiceSelectionSuggestion()}/>
                        </FormGroup>
                    </div>
                </div>
                  
                {this.renderURLBox()}

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
                    { this.props.paramsType === "showHeaders" && this.props.tabId && !this.props.disabled && 
                        <HideInternalHeadersButton clientTabId={this.props.clientTabId} headers={this.props.headers}/>
                    }
                </div>
            </>
        );
    }
}

const mapStateToProps = (state: IStoreState) =>  {
    const {httpClient: {selectedMockConfig, mockConfigList}} = state;
    //Progressively, get as much possible props from redux state by tabId, rather then passing as props between components
    return {
        selectedMockConfig,
        mockConfigList
    }
};

export default connect(mapStateToProps)(HttpRequestMessage);
