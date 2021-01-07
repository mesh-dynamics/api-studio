import * as React from "react";
import HttpRequestHeaders, {
  IHttpRequestHeadersProps,
} from "./HttpRequestHeaders";
import HttpRequestQueryString, {
  IHttpRequestQueryStringProps,
} from "./HttpRequestQueryString";
import HttpRequestBody, { IHttpRequestBodyProps } from "./HttpRequestBody";

export interface IHttpRequestFieldsProps
  extends IHttpRequestHeadersProps,
    IHttpRequestQueryStringProps,
    IHttpRequestBodyProps {
        paramsType: string
}

export class HttpRequestFields extends React.Component<
  IHttpRequestFieldsProps
> {
  render() {
    return (
      <>
        <HttpRequestHeaders
          tabId={this.props.tabId}
          showHeaders={this.props.paramsType === "showHeaders"}
          headers={this.props.headers}
          addOrRemoveParam={this.props.addOrRemoveParam}
          updateParam={this.props.updateParam}
          updateAllParams={this.props.updateAllParams}
          isOutgoingRequest={this.props.isOutgoingRequest}
          readOnly={this.props.readOnly}
          isResponse={false}
        ></HttpRequestHeaders>
        <HttpRequestQueryString
          tabId={this.props.tabId}
          showQueryParams={this.props.paramsType === "showQueryParams"}
          queryStringParams={this.props.queryStringParams}
          addOrRemoveParam={this.props.addOrRemoveParam}
          updateParam={this.props.updateParam}
          updateAllParams={this.props.updateAllParams}
          readOnly={this.props.readOnly}
          isOutgoingRequest={this.props.isOutgoingRequest}
        ></HttpRequestQueryString>
        <HttpRequestBody
          tabId={this.props.tabId}
          showBody={this.props.paramsType === "showBody"}
          formData={this.props.formData}
          addOrRemoveParam={this.props.addOrRemoveParam}
          updateParam={this.props.updateParam}
          updateAllParams={this.props.updateAllParams}
          rawData={this.props.rawData}
          readOnly={this.props.readOnly}
          isOutgoingRequest={this.props.isOutgoingRequest}
          rawDataType={this.props.rawDataType}
          updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
          id={this.props.id}
          grpcData = {this.props.grpcData}
          bodyType={this.props.bodyType}
        ></HttpRequestBody>
      </>
    );
  }
}
