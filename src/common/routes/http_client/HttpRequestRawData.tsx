import React, { Component } from "react";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import Editor from "../../components/Editor/Editor";
export interface IHttpRequestRawDataProps {
  showRawData: boolean;
  rawData: string;
  tabId: string;
  isOutgoingRequest: boolean;
  updateParam: UpdateParamHandler;
  readOnly: boolean;
  paramName: string;
}
export interface IHttpRequestRawDataState {
  showError: boolean;
}

class HttpRequestRawData extends Component<
  IHttpRequestRawDataProps,
  IHttpRequestRawDataState
> {
  private editorRef : CodeMirror.Editor;
  constructor(props: IHttpRequestRawDataProps) {
    super(props);
    this.state = {
      showError: false,
    };
    this.handleChange = this.handleChange.bind(this);
    this.formatHandler = this.formatHandler.bind(this);
  }

  handleChange(value: string) {
    const { tabId, isOutgoingRequest, rawData } = this.props;
    if (value !== rawData) {
      this.props.updateParam(
        isOutgoingRequest,
        tabId,
        this.props.paramName,
        this.props.paramName,
        value
      );
    }
  }
  formatHandler = () => {
    this.setState({ showError: false });
    try {
      this.editorRef &&  (this.editorRef as any).format();
    } catch (error) {
      this.setState({ showError: true });
    }
  };

  hideError = () => {
    this.setState({ showError: false });
  };

  render() {
    const showRawData = this.props.showRawData;
    return showRawData ? (
      <div style={{ height: "100%", minHeight: "100px" }}>
        <div
          style={{
            width: "100%",
            display: this.props.readOnly ? "none" : "block",
          }}
        >
          {this.state.showError ? (
            <>
              <span style={{ color: "red", marginRight: "10px" }}>
                Couldn't format. Please validate the document for any errors.
              </span>
              <i
                className="fa fa-times"
                title="Hide error"
                aria-hidden="true"
                onClick={this.hideError}
              ></i>
            </>
          ) : (
            <></>
          )}
        </div>
        <div style={{ height: "100%" }}>
        <Editor
            value={this.props.rawData}
            onChange={this.handleChange}
            language="json"
            getEditorRef = {(editor)=> this.editorRef = editor}
            readonly={ this.props.readOnly}
          />
        </div>
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpRequestRawData;
