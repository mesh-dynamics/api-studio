import React, { Component } from "react";
import { FormGroup, FormControl, Grid, Row, Col } from "react-bootstrap";
import _ from "lodash";
// import "./styles_here.css";

import HttpResponseHeaders, {
  IHttpResponseHeadersProps,
} from "./HttpResponseHeaders";
import HttpResponseBody, { IHttpResponseBodyProps } from "./HttpResponseBody";
import ResponseStatusEditable from "./ResponseStatusEditable";

export interface IHttpResponseMessageProps
  extends IHttpResponseHeadersProps,
    IHttpResponseBodyProps {
  recordedResponseStatus: string;
  responseStatus: string;
  requestRunning: boolean;
  isGrpcRHS: boolean;
  isGrpcLHS: boolean;
}
export interface IHttpResponseMessageState {
  status: string;
  showHeaders: boolean;
  showBody: boolean;
  responseBodyType: string;
  maximizeEditorHeight: boolean;
}

class HttpResponseMessage extends Component<
  IHttpResponseMessageProps,
  IHttpResponseMessageState
> {
  private childRefHttpResponseBody: React.RefObject<HttpResponseBody>;
  private isResponseBodyTypeManuallySet: boolean = false;
  constructor(props: IHttpResponseMessageProps) {
    super(props);
    this.state = {
      status: "",
      showHeaders: false,
      showBody: true,
      responseBodyType: this.getInitialResponseBodyType(props),
      maximizeEditorHeight: false,
    };
    this.childRefHttpResponseBody = React.createRef<HttpResponseBody>();
    this.onChangeValue = this.onChangeValue.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.onMaxHeightIconClick = this.onMaxHeightIconClick.bind(this);
  }

  onChangeValue(event) {
    this.setState({
      showHeaders: event.target.value === "showHeaders",
      showBody: event.target.value === "showBody",
    });
  }

  onFormatIconClick = () => {
    if (!this.state.showHeaders) {
      this.childRefHttpResponseBody.current &&
        this.childRefHttpResponseBody.current.formatHandler();
    }
  };
  //Move this to utilities

  getContentTypeToLanguage(headers: string) {
    try {
      const contentType = JSON.parse(headers)["content-type"];
      if (_.isArray(contentType)) {
        return contentType.find((header) => header.indexOf("text/html") !== -1)
          ? "html"
          : contentType.find(
              (header) =>
                header.indexOf("application/json") !== -1 ||
                header.indexOf("application/grpc") !== -1
            )
          ? "json"
          : "text";
      } else if (_.isString(contentType)) {
        return contentType.indexOf("text/html") !== -1
          ? "html"
          : contentType.indexOf("application/json") !== -1 ||
            contentType.indexOf("application/grpc") !== -1
          ? "json"
          : "text";
      }
    } catch (error) {
      //Silent error handling. Occurs due to headers is null or empty or not valid JSON object
    }
    return "json";
  }

  getInitialResponseBodyType(props: IHttpResponseMessageProps) {
    const responseLanguage = this.getContentTypeToLanguage(
      this.props.responseHeaders
    );
    const recordedResponseLanguage = this.getContentTypeToLanguage(
      this.props.recordedResponseHeaders
    );
    if (responseLanguage !== "json") {
      return responseLanguage;
    } else if (recordedResponseLanguage !== "json") {
      return recordedResponseLanguage;
    } else {
      return "json";
    }
  }

  //Get language type headers from contentType header on props change
  componentWillReceiveProps(nextProps: IHttpResponseMessageProps) {
    const responseLanguage = this.getContentTypeToLanguage(
      this.props.responseHeaders
    );
    const recordedResponseLanguage = this.getContentTypeToLanguage(
      this.props.recordedResponseHeaders
    );
    const responseLanguageNext = this.getContentTypeToLanguage(
      nextProps.responseHeaders
    );
    const recordedResponseLanguageNext = this.getContentTypeToLanguage(
      nextProps.recordedResponseHeaders
    );
    if (responseLanguage !== responseLanguageNext) {
      this.setState({ responseBodyType: responseLanguageNext });
    } else if (recordedResponseLanguage !== recordedResponseLanguageNext) {
      this.setState({ responseBodyType: recordedResponseLanguageNext });
    } else if (!this.isResponseBodyTypeManuallySet) {
      if (responseLanguageNext !== "json" || nextProps.responseBody) {
        responseLanguageNext !== this.state.responseBodyType &&
          this.setState({ responseBodyType: responseLanguageNext });
      } else if (recordedResponseLanguageNext !== "json") {
        recordedResponseLanguageNext !== this.state.responseBodyType &&
          this.setState({ responseBodyType: recordedResponseLanguageNext });
      } else {
        this.setState({ responseBodyType: "json" });
      }
    }
  }

  onMaxHeightIconClick() {
    this.setState(
      { maximizeEditorHeight: !this.state.maximizeEditorHeight },
      () => {
        if (this.state.maximizeEditorHeight) {
          const contentWrapper = document.querySelector(".content-wrapper");
          const editorDiv = document.querySelector(
            ".diffEditors"
          ) as HTMLDivElement;
          if (contentWrapper && editorDiv) {
            contentWrapper.scroll({
              behavior: "smooth",
              top: contentWrapper.scrollHeight - editorDiv.offsetHeight - 85,
            });
          }
        }
      }
    );
  }

  handleChange(event) {
    this.isResponseBodyTypeManuallySet = true;
    this.setState({
      responseBodyType: event.target.value,
    });
  }

  render() {
    const {
      recordedResponseStatus,
      responseStatus,
      //   responseStatusText,
      requestRunning,
      isGrpcRHS, isGrpcLHS,
    } = this.props;
    return (
      <div style={{ marginTop: "18px" }}>
        <div style={{ fontSize: "11px" }}>RESPONSE</div>
        <div style={{ marginTop: "7px", marginBottom: "0px" }}>
          <div
            className=""
            style={{
              display: "inline-block",
              paddingRight: "18px",
              opacity: "0.7",
              fontSize: "12px",
              width: "50px",
            }}
          >
            VIEW
          </div>
          <div
            className=""
            style={{
              display: "inline-block",
              paddingRight: "10px",
              fontSize: "12px",
            }}
          >
            <input
              type="radio"
              style={{ marginTop: "0px", marginRight: "9px" }}
              value="showHeaders"
              name="fieldType"
              checked={this.state.showHeaders}
              onChange={this.onChangeValue}
            />
            Headers
          </div>
          <div
            className=""
            style={{ display: "inline-block", fontSize: "12px" }}
          >
            <input
              type="radio"
              style={{ marginTop: "0px", marginRight: "9px" }}
              value="showBody"
              name="fieldType"
              checked={this.state.showBody}
              onChange={this.onChangeValue}
            />
            Body
          </div>
          <div
            className=""
            style={{
              display: this.state.showBody ? "inline-block" : "none",
              fontSize: "12px",
              marginLeft: "9px",
            }}
          >
            <FormGroup bsSize="small">
              <FormControl
                componentClass="select"
                placeholder="Method"
                style={{ fontSize: "12px" }}
                name="responseBodyType"
                value={this.state.responseBodyType}
                onChange={this.handleChange}
              >
                <option value="json">JSON</option>
                <option value="txt">Text</option>
                <option value="html">HTML</option>
                <option value="xml">XML</option>
                <option value="js">JavaScript</option>
                <option value="auto">Auto</option>
              </FormControl>
            </FormGroup>
          </div>
        </div>
        <Grid
          className="margin-top-15"
          style={{ fontSize: "12px", marginBottom: "12px" }}
        >
          <Row className="show-grid">
            <Col xs={6}>
              <span style={{ opacity: "0.7" }}>
                {isGrpcLHS ? "gRPC" : "HTTP"} RESPONSE STATUS:
                <ResponseStatusEditable
                  tabId={this.props.tabId}
                  clientTabId={this.props.clientTabId}
                  status={recordedResponseStatus}
                  isRecordingStatus={true}
                  requestRunning={false}
                  isGrpc={isGrpcLHS}
                />
              </span>
            </Col>
            <Col xs={6}>
              <span style={{ opacity: "0.7" }}>
              {isGrpcRHS ? "gRPC" : "HTTP"} RESPONSE STATUS:
                <ResponseStatusEditable
                  tabId={this.props.tabId}
                  clientTabId={this.props.clientTabId}
                  status={responseStatus}
                  isRecordingStatus={false}
                  requestRunning={requestRunning}
                  isGrpc={isGrpcRHS}
                />
              </span>

              <div style={{ float: "right" }}>
                <span
                  className="btn btn-sm cube-btn text-center"
                  style={{ padding: "2px 10px", display: "inline-block" }}
                  title={
                    this.state.maximizeEditorHeight
                      ? "Shrink editor height"
                      : "Fit to window height"
                  }
                  onClick={this.onMaxHeightIconClick}
                >
                  {this.state.maximizeEditorHeight ? (
                    <i className="fa fa-compress" aria-hidden="true"></i>
                  ) : (
                    <i className="fa fa-expand" aria-hidden="true"></i>
                  )}
                </span>
              </div>
              {this.state.showBody && (
                <div style={{ float: "right" }}>
                  <span
                    className="btn btn-sm cube-btn text-center"
                    style={{ padding: "2px 10px", display: "inline-block" }}
                    title="Format document"
                    onClick={this.onFormatIconClick}
                  >
                    <i className="fa fa-align-center" aria-hidden="true"></i>{" "}
                    Format
                  </span>
                </div>
              )}
            </Col>
          </Row>
        </Grid>
        <div className="diffEditors">
          <HttpResponseHeaders
            tabId={this.props.tabId}
            clientTabId={this.props.clientTabId}
            showHeaders={this.state.showHeaders}
            responseHeaders={this.props.responseHeaders}
            recordedResponseHeaders={this.props.recordedResponseHeaders}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
          ></HttpResponseHeaders>
          <HttpResponseBody
            ref={this.childRefHttpResponseBody}
            tabId={this.props.tabId}
            showBody={this.state.showBody}
            responseBody={this.props.responseBody}
            recordedResponseBody={this.props.recordedResponseBody}
            updateParam={this.props.updateParam}
            responseBodyType={this.state.responseBodyType}
            isOutgoingRequest={this.props.isOutgoingRequest}
            maximizeEditorHeight={this.state.maximizeEditorHeight}
          ></HttpResponseBody>
        </div>
      </div>
    );
  }
}

export default HttpResponseMessage;
