import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";
import {UpdateParamHandler, AddOrRemoveHandler, IFormData} from './HttpResponseHeaders';

export interface IHttpRequestFormDataProps{
    tabId: string;
     isOutgoingRequest :boolean;
     showFormData :boolean;
     readOnly :boolean;
     updateParam: UpdateParamHandler;
     addOrRemoveParam: AddOrRemoveHandler;
     updateAllParams: UpdateParamHandler;
     formData: IFormData[]
}

class HttpRequestFormData extends Component<IHttpRequestFormDataProps> {
    constructor(props: IHttpRequestFormDataProps) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "formData", "add");
    }

    handleDelete(id: string) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "formData", "delete", id);
    }

    handleChange(id: string, evt: React.ChangeEvent<HTMLInputElement>) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "formData", evt.target.name, evt.target.value, id);
    }

    handleCheckChange = (id: string, currentChecked: boolean) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "formData", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.formData.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "formData", "selected", e.target.checked);
    }

    render() {
        return (
            <div style={{display: this.props.showFormData === true ? "" : "none"}} className="params-input">
                {this.props.formData.length > 0 && (
                    <div className="header">
                        <div className="cell cell-1"> 
                            <FormGroup bsSize="small">
                                <input type="checkbox" checked={this.allSelected()} 
                                disabled={this.props.readOnly} onChange={this.handleAllCheckChange}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-2"> 
                            <FormGroup>
                                <ControlLabel>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div className="cell cell-3">
                            <FormGroup bsSize="small">
                                <ControlLabel>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                        <div className="cell cell-4">
                            <FormGroup bsSize="small">
                                <ControlLabel></ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.formData.map(eachParam => {return (
                    <div className="row" key={eachParam.id}>
                        <div className="cell cell-1"> 
                            <FormGroup>
                            <input type="checkbox" checked={eachParam.selected} 
                            disabled={this.props.readOnly} 
                                onChange={() => this.handleCheckChange(eachParam.id, eachParam.selected)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-2"> 
                            <FormGroup>
                                <FormControl type="text" placeholder="" 
                                readOnly={this.props.readOnly} 
                                value={eachParam.name} name="name" onChange={this.handleChange.bind(this, eachParam.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-3">
                            <FormGroup>
                                <FormControl type="text" placeholder="" 
                                readOnly={this.props.readOnly} value={eachParam.value} name="value" onChange={this.handleChange.bind(this, eachParam.id)} />
                            </FormGroup>
                        </div>
                        <div className="cell cell-4" onClick={this.handleDelete.bind(this, eachParam.id)} > 
                            <FormGroup>
                                <Glyphicon glyph="remove-sign" title="Remove" /> 
                            </FormGroup>
                        </div>
                    </div>
                )})}
                <div style={{ marginTop: "5px", marginRight: "7px"}}>
                    <div style={{display: this.props.readOnly? "none" : "inline-block", width: "100%"}}>
                        <button className="add-request-options-button" onClick={this.handleAdd} >
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