import React, { Component } from "react";
import { Glyphicon } from "react-bootstrap";

import HttpRequestFormDataRO, {
  IHttpRequestFormDataROProps,
} from "./HttpRequestFormDataRO";
import HttpRequestRawDataRO, {
  IHttpRequestRawDataROProps,
} from "./HttpRequestRawDataRO";

//TODO: If showBody is false then this component itself not required loaded from Parent

export interface IHttpRequestBodyROProps
  extends IHttpRequestRawDataROProps,
    IHttpRequestFormDataROProps {
  showBody: boolean;
}

class HttpRequestBodyRO extends Component<IHttpRequestBodyROProps> {
  render() {
    return (
      <div
        style={{
          display: this.props.showBody === true ? "" : "none",
          height: "calc(100% - 210px)",
          minHeight: "50px",
        }}
      >
        <HttpRequestFormDataRO
          showFormData={this.props.showFormData}
          formData={this.props.formData}
        ></HttpRequestFormDataRO>
        <HttpRequestRawDataRO
          showRawData={this.props.showRawData}
          rawData={this.props.rawData}
        ></HttpRequestRawDataRO>
      </div>
    );
  }
}

export default HttpRequestBodyRO;
