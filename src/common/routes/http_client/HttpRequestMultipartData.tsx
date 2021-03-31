import React, { Component } from 'react';
import { Glyphicon, FormGroup, FormControl, ControlLabel } from 'react-bootstrap';
import { convertFileToString, tryJsonParse } from '../../utils/http_client/utils';
import AutoCompleteBox from './components/AutoCompleteBox';
// import "./styles_here.css";
import {UpdateParamHandler, AddOrRemoveHandler, IFormData} from './HttpResponseHeaders';

export interface IMultipartData extends IFormData{
  isFile: boolean;
}

export interface IMultipartFileJSON{
    value: string;
    filename:string;
    type: string;
}

export interface IHttpRequestMultipartDataProps{
    tabId: string;
     isOutgoingRequest :boolean;
     showMultipartData :boolean;
     readOnly :boolean;
     updateParam: UpdateParamHandler;
     addOrRemoveParam: AddOrRemoveHandler;
     updateAllParams: UpdateParamHandler;
     multipartData: IMultipartData[]
}

class HttpRequestMultipartData extends Component<IHttpRequestMultipartDataProps> {
    constructor(props: IHttpRequestMultipartDataProps) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
    }

    handleAdd() {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "multipartData", "add");
    }
    handleAddFile = () => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "multipartDataFile", "add");
    }

    handleDelete(id: string) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.addOrRemoveParam(isOutgoingRequest, tabId, "multipartData", "delete", id);
    }

    handleChange(id: string, evt: React.ChangeEvent<HTMLInputElement>) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "multipartData", evt.target.name, evt.target.value, id);
    }

    async handleFileChange(id: string, evt: React.ChangeEvent<HTMLInputElement>) {
        const { tabId, isOutgoingRequest } = this.props;
        let fileJSON = {};
        const name = evt.target.name;
        if(evt.target.files?.length)
        {
            fileJSON = { 
                filename: evt.target.files[0].name,
                type: evt.target.files[0].type,
                value: await convertFileToString(evt.target.files[0]),
            } as IMultipartFileJSON;
        }
        const fileData = JSON.stringify(fileJSON);
        this.props.updateParam(isOutgoingRequest, tabId, "multipartData",name , fileData, id);
    }

    clearFileSelection(id:string, evt: React.MouseEvent<HTMLElement>){
        if(!this.props.readOnly){
            const { tabId, isOutgoingRequest } = this.props;
            const fileData = JSON.stringify({});
            this.props.updateParam(isOutgoingRequest, tabId, "multipartData", (evt.target as HTMLElement).getAttribute("data-name")!, fileData, id);
        }
    }

    handleCheckChange = (id: string, currentChecked: boolean) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "multipartData", "selected", !currentChecked, id);
    }

    allSelected = () => {
        return this.props.multipartData.reduce((acc, param) => (acc = acc && param.selected), true)
    }

    handleAllCheckChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateAllParams(isOutgoingRequest, tabId, "multipartData", "selected", e.target.checked);
    }

    render() {
        
        return (
            <div style={{display: this.props.showMultipartData === true ? "" : "none"}} className="params-input">
                {this.props.multipartData.length > 0 && (
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
                {this.props.multipartData.map(eachParam => {
                    let fileData : IMultipartFileJSON | null = null;
                    if(eachParam.isFile){
                        fileData = tryJsonParse(eachParam.value);
                    }
                    
                    return (
                    <div className="row" key={eachParam.id}>
                        <div className="cell cell-1"> 
                            <FormGroup>
                            <input type="checkbox" checked={eachParam.selected} 
                            disabled={this.props.readOnly} 
                                onChange={() => this.handleCheckChange(eachParam.id, eachParam.selected)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-2"> 
                            <FormGroup className="autocomplete">
                                    <AutoCompleteBox readOnly={this.props.readOnly} id={"name"+ eachParam.id}
                                        value={eachParam.name} name="name" onChange={this.handleChange.bind(this, eachParam.id)}/>
                            </FormGroup>
                        </div>
                        <div className="cell cell-3">
                            {!eachParam.isFile ? <FormGroup className="autocomplete">
                                <AutoCompleteBox readOnly={this.props.readOnly} id={"value" + eachParam.id}
                                    value={eachParam.value} name="value" onChange={this.handleChange.bind(this, eachParam.id)}/>
                            </FormGroup> :
                            <FormGroup>
                                {fileData && fileData.filename ?
                                <div className={this.props.readOnly ? "": "form-control"}>
                                    <div className="fileListParam">
                                    <span>{fileData.filename}</span>
                                    <i className="fas fa-times pointer" onClick={this.clearFileSelection.bind(this, eachParam.id)} data-name="value" />
                                </div></div>
                                :<FormControl type="file" placeholder="" disabled={this.props.readOnly}
                                readOnly={this.props.readOnly} name="value" onChange={this.handleFileChange.bind(this, eachParam.id)} />
                                }
                            </FormGroup>}
                        </div>
                        <div className="cell cell-4" onClick={this.handleDelete.bind(this, eachParam.id)} > 
                            <FormGroup>
                                <Glyphicon glyph="remove-sign" title="Remove" /> 
                            </FormGroup>
                        </div>
                    </div>
                )})}
                <div style={{ marginTop: "5px", marginRight: "7px"}}>
                    <div style={{display: this.props.readOnly? "none" : "flex", width: "100%", whiteSpace: "nowrap"}}>
                        <button className="add-request-options-button" onClick={this.handleAdd} >
                            <span style={{ fontSize: "20px" }}>+</span>
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add form data</span>
                        </button>
                        <button className="add-request-options-button" onClick={this.handleAddFile} >
                            <span style={{ fontSize: "20px" }}>+</span>
                            <span style={{ marginLeft: "5px", fontWeight: 400 }}>Add file field</span>
                        </button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestMultipartData;