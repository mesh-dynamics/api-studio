import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
import "./HttpClient.css";
// import "./styles_here.css";

import { AddOrRemoveHandler, UpdateParamHandler} from './HttpResponseHeaders';

export interface IHttpRequestQueryStringProps{
    tabId: string, isOutgoingRequest: boolean;
    addOrRemoveParam: AddOrRemoveHandler;
    updateParam: UpdateParamHandler;
    updateAllParams: UpdateParamHandler;
    showQueryParams: boolean;
    queryStringParams: any[]; //TODO: Get proper interface from HttpClientTabs

}

class HttpRequestQueryString extends Component<IHttpRequestQueryStringProps> {
    constructor(props) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "queryStringParams", "add");
    }

    handleDelete(id) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "queryStringParams", "delete", id);
    }

    handleChange(id, evt) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "queryStringParams", evt.target.name, evt.target.value, id);
    }

    handleCheckChange = (id, currentChecked) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "queryStringParams", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.queryStringParams.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "queryStringParams", "selected", e.target.checked);
    }

    render() {
        return (
            <div style={{display: this.props.showQueryParams === true ? "" : "none"}}>
                {this.props.queryStringParams.length > 0 && (
                    <div style={{marginBottom: "1px"}}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup bsSize="small" style={{marginBottom: "0px", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} checked={this.allSelected()} onChange={this.handleAllCheckChange}/>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "55%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                        {/* <div style={{display: "inline-block", width: "54%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>DESCRIPTION</ControlLabel>
                            </FormGroup>
                        </div> */}
                        <div style={{display: "inline-block", width: "7%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}></ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.queryStringParams.map(eachParam => {return (
                    <div style={{marginBottom: "1px"}} key={eachParam.id}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "none", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} checked={eachParam.selected} 
                                onChange={() => this.handleCheckChange(eachParam.id, eachParam.selected)}/>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" 
                                value={eachParam.name} name="name" onChange={this.handleChange.bind(this, eachParam.id)}/>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "55%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" value={eachParam.value} name="value" onChange={this.handleChange.bind(this, eachParam.id)} />
                            </FormGroup>
                        </div>
                        {/* <div style={{display: "inline-block", width: "54%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="optional" 
                                value={eachParam.description} name="description" onChange={this.handleChange.bind(this, eachParam.id)} />
                            </FormGroup>
                        </div> */}
                        <div style={{display: "inline-block", width: "7%", paddingRight: "9px"}} 
                                onClick={this.handleDelete.bind(this, eachParam.id)} > 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "#ffffff", textAlign: "center", padding: "5px"}}>
                                <Glyphicon style={{fontSize: "16px", top: "5px"}} glyph="remove-sign" /> 
                            </FormGroup>
                        </div>
                    </div>
                )})}
                <div style={{ marginTop: "5px", marginRight: "7px"}}>
                    <div style={{display: "inline-block", width: "100%"}}> 
                        <button className="add-request-options-button" onClick={this.handleAdd}>
                            <span style={{ fontSize: "20px" }}>+</span>
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add query parameter</span>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestQueryString;