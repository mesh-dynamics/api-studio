import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";
import {UpdateParamHandler, AddOrRemoveHandler, IFormData} from './HttpResponseHeaders';

export interface IHttpRequestFormDataProps{
    tabId: string;
     isOutgoingRequest :boolean;
     showFormData :boolean;
     updateParam: UpdateParamHandler;
     addOrRemoveParam: AddOrRemoveHandler;
     updateAllParams: UpdateParamHandler;
     formData: IFormData[]
}

class HttpRequestFormData extends Component<IHttpRequestFormDataProps> {
    constructor(props: IHttpRequestFormDataProps) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "formData", "add");
    }

    handleDelete(id) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "formData", "delete", id);
    }

    handleChange(id, evt) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "formData", evt.target.name, evt.target.value, id);
    }

    handleCheckChange = (id, currentChecked) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "formData", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.formData.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "formData", "selected", e.target.checked);
    }

    render() {
        return (
            <div style={{display: this.props.showFormData === true ? "" : "none"}}>
                {this.props.formData.length > 0 && (
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
                {this.props.formData.map(eachParam => {return (
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
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add form data</span>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestFormData;