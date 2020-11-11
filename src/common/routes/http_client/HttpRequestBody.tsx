import React, { ChangeEvent, Component } from "react";
import { Glyphicon } from "react-bootstrap";

import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestRawData from "./HttpRequestRawData";
import HttpRequestBinaryData from "./HttpRequestBinaryData";

import { FormGroup, FormControl} from 'react-bootstrap';

import classNames from 'classnames';

import {
  UpdateParamHandler,
  AddOrRemoveHandler,
  IFormData,
} from "./HttpResponseHeaders";
import { UpdateBodyOrRawDataTypeHandler } from './HttpResponseHeaders';

//TODO: These params can be reduced from HttpClientTabs. Example: showRawData and rawData can be combined
export interface IHttpRequestBodyProps {
  showBody: boolean;
  tabId: string;
  rawData: string;
  isOutgoingRequest: boolean;
  readOnly: boolean;
  updateParam: UpdateParamHandler;
  addOrRemoveParam: AddOrRemoveHandler;
  updateAllParams: UpdateParamHandler;
  formData: IFormData[];
  id: string;
  rawDataType: string;
  updateBodyOrRawDataType: UpdateBodyOrRawDataTypeHandler;
}

export interface IHttpRequestBodyState{
  showFormData: boolean;
  rawDataRef : HttpRequestRawData | null
}
class HttpRequestBody extends Component<IHttpRequestBodyProps, IHttpRequestBodyState> {
  constructor(props: IHttpRequestBodyProps){
    super(props);
    this.state = {
      showFormData: true,
      rawDataRef : null
    }
  }
   
   handleBodyOrRawDataType = (event: ChangeEvent<HTMLInputElement>) => {
    const { tabId, isOutgoingRequest } = this.props;
    const typeToUpdate = event.target.name === "bodyType"+this.props.id.trim() ? "bodyType" : "rawDataType";
    this.props.updateBodyOrRawDataType(isOutgoingRequest, tabId, typeToUpdate === "bodyType" ? "bodyType" : "rawDataType", event.target.value);
    if(typeToUpdate === "bodyType") {
        this.setState({
            showFormData: event.target.value === "formData"
        });
    }
}
 formatHandler = ()=>{
      this.state.rawDataRef && this.state.rawDataRef.formatHandler();
}  
  render() {

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
        
     

    return (
      <>
      <div className="" style={{ marginBottom: "12px", display: this.props.showBody ? "" : "none"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        BODY
                    </div>
                    <div className={formDataLabelClass}>
                        <input type="radio"
                            value="formData" name={"bodyType"+this.props.id.trim()} checked={this.state.showFormData} onChange={this.handleBodyOrRawDataType}/>
                            x-www-form-urlencoded
                    </div>
                    <div className={rawDataLabelClass}>
                        <input type="radio"
                            value="rawData" name={"bodyType"+this.props.id.trim()} checked={!this.state.showFormData} onChange={this.handleBodyOrRawDataType}/>
                            Raw Data
                    </div>
                    <div className="" style={{display: this.state.showFormData ? "none":  "inline-block", paddingRight: "25px", fontSize: "12px"}}>
                        <FormGroup bsSize="small">
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} readOnly={this.props.readOnly} name="rawDataType" value={this.props.rawDataType} onChange={this.handleBodyOrRawDataType}>
                                <option value="txt">Text</option>
                                <option value="js">JavaScript</option>
                                <option value="json">JSON</option>
                                <option value="html">HTML</option>
                                <option value="xml">XML</option>
                            </FormControl>
                        </FormGroup>
                    </div>
                    <div style={{ float: "right", display: this.state.showFormData || this.props.readOnly ? "none":  "inline-block", }}>
                      <span
                        className="btn btn-sm cube-btn text-center"
                        style={{ padding: "2px 10px", display: "inline-block" }}
                        title="Format document"
                        onClick={this.formatHandler}
                      >
                        <i className="fa fa-align-center" aria-hidden="true"></i> Format
                      </span>
                    </div>
                </div>
      <div
        style={{
          display: this.props.showBody === true ? "" : "none",
          height: "calc(100% - 70px)",
          minHeight: "100px",
        }}
      >
        <HttpRequestFormData
          tabId={this.props.tabId}
          showFormData={this.state.showFormData}
          formData={this.props.formData}
          addOrRemoveParam={this.props.addOrRemoveParam}
          updateParam={this.props.updateParam}
          isOutgoingRequest={this.props.isOutgoingRequest}
          updateAllParams={this.props.updateAllParams}
          readOnly={this.props.readOnly} 
        ></HttpRequestFormData>
        <HttpRequestRawData
          tabId={this.props.tabId}
          showRawData={!this.state.showFormData}
          rawData={this.props.rawData}
          updateParam={this.props.updateParam}
          isOutgoingRequest={this.props.isOutgoingRequest}
          readOnly={this.props.readOnly} 
          ref={item => !this.state.rawDataRef && this.setState({rawDataRef: item})}
        ></HttpRequestRawData>
        {/* <HttpRequestBinaryData></HttpRequestBinaryData> */}
      </div>
      </>
    );
  }
}

export default HttpRequestBody;
