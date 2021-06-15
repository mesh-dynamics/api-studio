/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";
import { AddOrRemoveHandler, UpdateParamHandler } from './HttpResponseHeaders';
import {filterInternalHeaders} from '../../utils/http_client/utils'
import { connect } from "react-redux";
import { IRequestParamData, IStoreState } from '../../reducers/state.types';
import AutoCompleteBox from './components/AutoCompleteBox';

export interface IHttpRequestHeadersProps{
    tabId: string;
    isOutgoingRequest : boolean;
    showHeaders : boolean;
    addOrRemoveParam: AddOrRemoveHandler;
    updateParam: UpdateParamHandler;
    updateAllParams: UpdateParamHandler;
    readOnly: boolean;
    isResponse: boolean;
    headers: IRequestParamData[];
    hideInternalHeaders: boolean;
    clientTabId: string;
}

class HttpRequestHeaders extends Component<IHttpRequestHeadersProps> {
    
    constructor(props: IHttpRequestHeadersProps) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "headers", "add");
    }

    handleDelete(id: string) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "headers", "delete", id);
    }

    handleChange(id: string, evt: React.ChangeEvent<HTMLInputElement>) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "headers", evt.target.name, evt.target.value, id);
    }

    handleCheckChange = (id: string, currentChecked: boolean) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "headers", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.headers.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "headers", "selected", e.target.checked);
    }

    render() {
        const displayHeaders: IRequestParamData[] = filterInternalHeaders(this.props.headers, this.props.hideInternalHeaders);
        return (
            <div style={{display: this.props.showHeaders === true ? "" : "none"}} className="params-input">
                {this.props.headers.length > 0 && (
                    <div className="header">
                        <div className="cell cell-1"> 
                            {!this.props.isResponse && <FormGroup bsSize="small">
                                <input type="checkbox" disabled={this.props.readOnly} checked={this.allSelected()} onChange={this.handleAllCheckChange}/>
                            </FormGroup>}
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
                {displayHeaders.map(eachHeader => {return (
                    <div  className="row" key={eachHeader.id}>
                        <div className="cell cell-1"> 
                          {!this.props.isResponse && <FormGroup>
                                <input type="checkbox" checked={eachHeader.selected} 
                                disabled={this.props.readOnly} 
                                onChange={() => this.handleCheckChange(eachHeader.id, eachHeader.selected)}/>                            
                            </FormGroup>}
                        </div>
                        <div className="cell cell-2"> 
                            <FormGroup className="autocomplete">
                                <AutoCompleteBox readOnly={this.props.readOnly} id={"name"+ eachHeader.id} headerList={true}
                                value={eachHeader.name} name="name" onChange={this.handleChange.bind(this, eachHeader.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-3">
                            <FormGroup className="autocomplete">
                            <AutoCompleteBox readOnly={this.props.readOnly} id={"value"+ eachHeader.id}
                                value={eachHeader.value} name="value" onChange={this.handleChange.bind(this, eachHeader.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-4"
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


const mapStateToProps = (state: IStoreState, props: IHttpRequestHeadersProps) =>  {
    const hideInternalHeaders = !!(state.httpClient.tabs.find((tab => tab.id == props.clientTabId))?.hideInternalHeaders);
    return{
        hideInternalHeaders
    }
}

export default connect(mapStateToProps)(HttpRequestHeaders);
