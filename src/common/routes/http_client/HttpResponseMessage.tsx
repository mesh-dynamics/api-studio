import React, { Component } from "react";
import { FormGroup, FormControl, Grid, Row, Col } from "react-bootstrap";
import { getStatusColor } from "../../utils/http_client/utils";
import { getHttpStatus } from "../../status-code-list";
// import "./styles_here.css";

import HttpResponseHeaders, {
  IHttpResponseHeadersProps,
} from "./HttpResponseHeaders";
import HttpResponseBody, { IHttpResponseBodyProps } from "./HttpResponseBody";

export interface IHttpResponseMessageProps
  extends IHttpResponseHeadersProps,
    IHttpResponseBodyProps {
  recordedResponseStatus: string;
  responseStatus: string;
  requestRunning: boolean;
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
  private childRefHttpResponseHeaders: React.RefObject<HttpResponseHeaders>;
  constructor(props: IHttpResponseMessageProps) {
    super(props);
    this.state = {
      status: "",
      showHeaders: false,
      showBody: true,
      responseBodyType: "json",
      maximizeEditorHeight: false,
    };
    this.childRefHttpResponseBody = React.createRef<HttpResponseBody>();
    this.childRefHttpResponseHeaders = React.createRef<HttpResponseHeaders>();
    this.onChangeValue = this.onChangeValue.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.onFormatIconClick = this.onFormatIconClick.bind(this);
    this.onMaxHeightIconClick = this.onMaxHeightIconClick.bind(this);
  }

  onChangeValue(event) {
    this.setState({
      showHeaders: event.target.value === "showHeaders",
      showBody: event.target.value === "showBody",
    });
  }

  onFormatIconClick() {
    if (this.state.showHeaders) {
      this.childRefHttpResponseHeaders.current &&
        this.childRefHttpResponseHeaders.current.formatHandler();
    } else {
      this.childRefHttpResponseBody.current &&
        this.childRefHttpResponseBody.current.formatHandler();
    }
  }

  onMaxHeightIconClick() {
    this.setState(
      { maximizeEditorHeight: !this.state.maximizeEditorHeight },
      () => {
        if (this.state.maximizeEditorHeight) {
          const contentWrapper = document.querySelector(".content-wrapper");
          const monacoDiffEditor = document.querySelector(
            ".diffEditors .react-monaco-editor-container"
          ) as HTMLDivElement;
          if (contentWrapper && monacoDiffEditor) {
            contentWrapper.scroll({
              behavior: "smooth",
              top:
                contentWrapper.scrollHeight -
                monacoDiffEditor.offsetHeight -
                85,
            });
          }
        }
      }
    );
  }

  handleChange(event) {
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
                value={this.props.responseBodyType}
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
                HTTP RESPONSE STATUS:
                <b
                  style={{
                    color:
                      recordedResponseStatus &&
                      getStatusColor(recordedResponseStatus),
                  }}
                >
                  {" "}
                  {recordedResponseStatus
                    ? getHttpStatus(recordedResponseStatus)
                    : "NA"}
                </b>
              </span>
              
            </Col>
            <Col xs={6}>
              <span style={{ opacity: "0.7" }}>
                HTTP RESPONSE STATUS:
                <b
                  style={{
                    color: responseStatus && getStatusColor(responseStatus),
                  }}
                >
                  {" "}
                  {requestRunning
                    ? "WAITING..."
                    : responseStatus
                    ? getHttpStatus(responseStatus)
                    : "NA"}
                </b>
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
            </Col>
          </Row>
        </Grid>
        <div className="diffEditors">
          <HttpResponseHeaders
            ref={this.childRefHttpResponseHeaders}
            tabId={this.props.tabId}
            showHeaders={this.state.showHeaders}
            responseHeaders={this.props.responseHeaders}
            recordedResponseHeaders={this.props.recordedResponseHeaders}
            updateParam={this.props.updateParam}
            isOutgoingRequest={this.props.isOutgoingRequest}
            maximizeEditorHeight={this.state.maximizeEditorHeight}
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
