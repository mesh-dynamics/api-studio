import React, { Component } from "react";
import { Glyphicon } from "react-bootstrap";

import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestRawData from "./HttpRequestRawData";
import HttpRequestBinaryData from "./HttpRequestBinaryData";

import {
  UpdateParamHandler,
  AddOrRemoveHandler,
  IFormData,
} from "./HttpResponseHeaders";

//TODO: These params can be reduced from HttpClientTabs. Example: showRawData and rawData can be combined
export interface IHttpRequestBodyProps {
  showBody: boolean;
  showRawData: boolean;
  tabId: string;
  rawData: string;
  isOutgoingRequest: boolean;
  showFormData: boolean;
  readOnly: boolean;
  updateParam: UpdateParamHandler;
  addOrRemoveParam: AddOrRemoveHandler;
  updateAllParams: UpdateParamHandler;
  formData: IFormData[];
}
class HttpRequestBody extends Component<IHttpRequestBodyProps> {
  render() {
    return (
      <div
        style={{
          display: this.props.showBody === true ? "" : "none",
          height: "calc(100% - 210px)",
          minHeight: "50px",
        }}
      >
        <HttpRequestFormData
          tabId={this.props.tabId}
          showFormData={this.props.showFormData}
          formData={this.props.formData}
          addOrRemoveParam={this.props.addOrRemoveParam}
          updateParam={this.props.updateParam}
          isOutgoingRequest={this.props.isOutgoingRequest}
          updateAllParams={this.props.updateAllParams}
          readOnly={this.props.readOnly} 
        ></HttpRequestFormData>
        <HttpRequestRawData
          tabId={this.props.tabId}
          showRawData={this.props.showRawData}
          rawData={this.props.rawData}
          updateParam={this.props.updateParam}
          isOutgoingRequest={this.props.isOutgoingRequest}
          readOnly={this.props.readOnly} 
        ></HttpRequestRawData>
        {/* <HttpRequestBinaryData></HttpRequestBinaryData> */}
      </div>
    );
  }
}

export default HttpRequestBody;
