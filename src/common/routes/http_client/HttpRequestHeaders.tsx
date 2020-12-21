import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";
import { AddOrRemoveHandler, UpdateParamHandler } from './HttpResponseHeaders';

export interface IHttpRequestHeadersProps{
    tabId: string;
    isOutgoingRequest : boolean;
    showHeaders : boolean;
    addOrRemoveParam: AddOrRemoveHandler;
    updateParam: UpdateParamHandler;
    updateAllParams: UpdateParamHandler;
    readOnly: boolean;
    headers: any[]; //TODO: Get proper interface from HttpClientTabs
}

class HttpRequestHeaders extends Component<IHttpRequestHeadersProps> {
    constructor(props) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "headers", "add");
    }

    handleDelete(id) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "headers", "delete", id);
    }

    handleChange(id, evt) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "headers", evt.target.name, evt.target.value, id);
    }

    handleCheckChange = (id, currentChecked) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "headers", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.headers.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "headers", "selected", e.target.checked);
    }

    render() {
        return (
            <div style={{display: this.props.showHeaders === true ? "" : "none"}} className="params-input">
                {this.props.headers.length > 0 && (
                    <div className="header">
                        <div className="cell cell1"> 
                            <FormGroup bsSize="small">
                                <input type="checkbox" disabled={this.props.readOnly} checked={this.allSelected()} onChange={this.handleAllCheckChange}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell2"> 
                            <FormGroup>
                                <ControlLabel>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div className="cell cell3">
                            <FormGroup bsSize="small">
                                <ControlLabel>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                        <div className="cell cell4">
                            <FormGroup bsSize="small">
                                <ControlLabel></ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.headers.map(eachHeader => {return (
                    <div  className="row" key={eachHeader.id}>
                        <div className="cell cell1"> 
                            <FormGroup>
                                <input type="checkbox" checked={eachHeader.selected} 
                                disabled={this.props.readOnly} 
                                onChange={() => this.handleCheckChange(eachHeader.id, eachHeader.selected)}/>                            
                            </FormGroup>
                        </div>
                        <div className="cell cell2"> 
                            <FormGroup>
                                <FormControl type="text" placeholder="" 
                                readOnly={this.props.readOnly} 
                                value={eachHeader.name} name="name" onChange={this.handleChange.bind(this, eachHeader.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell3">
                            <FormGroup>
                                <FormControl
                                readOnly={this.props.readOnly}  type="text" placeholder="" value={eachHeader.value} name="value" onChange={this.handleChange.bind(this, eachHeader.id)} />
                            </FormGroup>
                        </div>
                        <div className="cell cell4"
                                onClick={this.handleDelete.bind(this, eachHeader.id)} > 
                            <FormGroup>
                                <Glyphicon glyph="remove-sign" title="Remove" /> 
                            </FormGroup>
                        </div>
                    </div>
                )})}
                <div style={{ marginTop: "5px", marginRight: "7px"}}>
                    <div style={{display:  this.props.readOnly? "none" : "inline-block", width: "100%"}}>
                        <button className="add-request-options-button" onClick={this.handleAdd}>
                            <span style={{ fontSize: "20px" }}>+</span>
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add header</span>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestHeaders;