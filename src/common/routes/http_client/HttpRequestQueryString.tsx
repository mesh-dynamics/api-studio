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
    readOnly: boolean;
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
            <div style={{display: this.props.showQueryParams === true ? "" : "none"}} className="params-input">
                {this.props.queryStringParams.length > 0 && (
                    <div className="header">
                        <div className="cell cell1"> 
                            <FormGroup bsSize="small">
                                <input type="checkbox" checked={this.allSelected()}
                                disabled={this.props.readOnly}  onChange={this.handleAllCheckChange}/>
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
                {this.props.queryStringParams.map(eachParam => {return (
                    <div className="row" key={eachParam.id}>
                        <div className="cell cell1"> 
                            <FormGroup>
                                <input type="checkbox" checked={eachParam.selected} 
                                disabled={this.props.readOnly} 
                                onChange={() => this.handleCheckChange(eachParam.id, eachParam.selected)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell2"> 
                            <FormGroup>
                                <FormControl type="text" placeholder="" 
                                readOnly={this.props.readOnly} 
                                value={eachParam.name} name="name" onChange={this.handleChange.bind(this, eachParam.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell3">
                            <FormGroup>
                                <FormControl type="text"
                                readOnly={this.props.readOnly}  placeholder="" value={eachParam.value} name="value" onChange={this.handleChange.bind(this, eachParam.id)} />
                            </FormGroup>
                        </div>
                        <div className="cell cell4" onClick={this.handleDelete.bind(this, eachParam.id)} > 
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
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add query parameter</span>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestQueryString;