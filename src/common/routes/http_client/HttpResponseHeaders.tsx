import React, { Component } from "react";
import { IRequestParamData } from "../../reducers/state.types";
import "./diff.css";
import HttpRequestHeaders from "./HttpRequestHeaders";
import _ from "lodash";
import { v4 as uuidv4 } from "uuid";
import { extractHeadersToCubeFormat } from "../../utils/http_client/utils";

// Start: Move from below declaration to HttpClientTabs (originating file).
// We may need to change param names in HttpClientTabs from where these are passed.
export declare type UpdateParamHandler = (
  isOutgoingRequest: boolean,
  tabId: string,
  type: string,
  key: string,
  value: string | boolean| FileList | null,
  id?: any
) => void;

export declare type AddOrRemoveHandler = (
  isOutgoingRequest: boolean,
  tabId: string,
  type: string,
  operation: string,
  id?: any
) => void;

export declare type UpdateBodyOrRawDataTypeHandler = (
  isOutgoingRequest: boolean,
  tabId: string,
  type: string,
  value: string
) => void;

export declare type ReplaceAllParamsHandler = (
  isOutgoingRequest: boolean,
  tabId: string,
  type: string,
  params: any,
) => void;

export interface IFormData {
  id: string;
  name: string;
  value: string;
  description: string;
  selected: boolean;
}
// End
export interface IHttpResponseHeadersState {
  recordedResponseHeaders: IRequestParamData[];
  responseHeaders: IRequestParamData[];
  recordedResponseHeadersSaved: string;
  responseHeadersSaved: string;
}
export interface IHttpResponseHeadersProps {
  tabId: string;
  isOutgoingRequest: boolean;
  showHeaders: boolean;
  updateParam: UpdateParamHandler;
  recordedResponseHeaders: string;
  responseHeaders: string;
}

class HttpResponseHeaders extends Component<
  IHttpResponseHeadersProps,
  IHttpResponseHeadersState
> {
  private updatePropsCallback: ()=>void;
  constructor(props: IHttpResponseHeadersProps) {
    super(props);
    this.state = {
      recordedResponseHeaders: HttpResponseHeaders.headersToKeyValurPair(
        this.props.recordedResponseHeaders
      ),
      recordedResponseHeadersSaved: this.props.recordedResponseHeaders,
      responseHeaders: HttpResponseHeaders.headersToKeyValurPair(
        this.props.responseHeaders
      ),
      responseHeadersSaved: this.props.responseHeaders,
    };
    this.updatePropsCallback = _.debounce(this.updatePropsParam, 400);
  }

  static headersToKeyValurPair(headerString: string) {
    const headers: IRequestParamData[] = [];
    if (headerString) {
      Object.entries(JSON.parse(headerString)).forEach((entry) => {
        const key = entry[0] as string;
        let value = entry[1] as string;
        if (_.isArray(value)) {
          value = value.join(";");
        }
        headers.push({
          id: key,
          name: key,
          value: value,
          description: "",
          selected: true,
        });
      });
    }
    return headers;
  }

  static getDerivedStateFromProps(
    props: IHttpResponseHeadersProps,
    state: IHttpResponseHeadersState
  ) {
    let updatedState: Partial<IHttpResponseHeadersState> = {};
    if (props.recordedResponseHeaders != state.recordedResponseHeadersSaved) {
      updatedState = {
        recordedResponseHeadersSaved: props.recordedResponseHeaders,
        recordedResponseHeaders: HttpResponseHeaders.headersToKeyValurPair(
          props.recordedResponseHeaders
        ),
      };
    }
    if (props.responseHeaders != state.responseHeadersSaved) {
      updatedState = {
        ...updatedState,
        responseHeadersSaved: props.responseHeaders,
        responseHeaders: HttpResponseHeaders.headersToKeyValurPair(
          props.responseHeaders
        ),
      };
    }
    return updatedState;
  }

  addOrRemoveParam = (
    isOutgoingRequest: boolean,
    tabId: string,
    type: string,
    op: string,
    id: string
  ) => {
    if(op === "delete"){
      const currentHeaderIndex = _.findIndex(this.state.recordedResponseHeaders, {
        id: id,
      })!;
      
      const changedResponseHeaders = [
        ...this.state.recordedResponseHeaders.slice(0, currentHeaderIndex),
        ...this.state.recordedResponseHeaders.slice(currentHeaderIndex + 1),
      ];
      this.setState(
        {
          recordedResponseHeaders: changedResponseHeaders
        },
        this.updatePropsCallback
      );
    }else{
      this.setState(
        {
          recordedResponseHeaders: [...this.state.recordedResponseHeaders, {
            id: uuidv4(),
            name: "",
            description: "",
            value: "",
            selected: true,
          }]
        },
        this.updatePropsCallback
      ); 
  }
  }

  updatePropsParam = () => {
    const headers = extractHeadersToCubeFormat(this.state.recordedResponseHeaders);
    const responseHeadersSaved = JSON.stringify(headers, null, 4);

    this.props.updateParam(
      this.props.isOutgoingRequest,
      this.props.tabId,
      "recordedResponseHeaders",
      "recordedResponseHeaders",
      responseHeadersSaved
    );
  };

  updateParam = (
    isOutgoingRequest: boolean,
    tabId: string,
    type: string,
    key: string,
    value: string,
    id: string
  ) => {
    const currentHeaderIndex = _.findIndex(this.state.recordedResponseHeaders, {
      id: id,
    })!;
    const currentValue = this.state.recordedResponseHeaders[currentHeaderIndex];
    if (key == "name") {
      currentValue["name"] = value;
    }
    if (key == "value") {
      currentValue["value"] = value;
    }
    const changedResponseHeaders = [
      ...this.state.recordedResponseHeaders.slice(0, currentHeaderIndex),
      currentValue,
      ...this.state.recordedResponseHeaders.slice(currentHeaderIndex + 1),
    ];
    const headers = extractHeadersToCubeFormat(changedResponseHeaders);
    const responseHeadersSaved = JSON.stringify(headers);
    this.setState(
      {
        recordedResponseHeaders: changedResponseHeaders
      },
      this.updatePropsCallback
    );
  };

  updateAllParams = (
    isOutgoingRequest: boolean,
    tabId: string,
    type: string,
    key: string,
    value: string
  ) => {};
  render() {
    const { showHeaders, tabId } = this.props;
    return showHeaders ? (
      <div style={{ width: "100%", display: "flex" }}>
        <div style={{ width: "50%", paddingRight: "10px" }}>
          <HttpRequestHeaders
            tabId={this.props.tabId}
            showHeaders={true}
            headers={this.state.recordedResponseHeaders}
            addOrRemoveParam={this.addOrRemoveParam}
            updateParam={this.updateParam}
            updateAllParams={this.updateAllParams}
            isOutgoingRequest={this.props.isOutgoingRequest}
            readOnly={false}
            isResponse={true}
          ></HttpRequestHeaders>
        </div>
        <div style={{ width: "50%" }}>
          <HttpRequestHeaders
            tabId={this.props.tabId}
            showHeaders={true}
            headers={this.state.responseHeaders}
            addOrRemoveParam={this.addOrRemoveParam}
            updateParam={this.updateParam}
            updateAllParams={this.updateAllParams}
            isOutgoingRequest={this.props.isOutgoingRequest}
            readOnly={true}
            isResponse={true}
          ></HttpRequestHeaders>
        </div>
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseHeaders;
