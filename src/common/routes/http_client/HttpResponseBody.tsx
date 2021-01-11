import React, { Component } from "react";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import "./diff.css";
import _ from "lodash";
import Editor from "../../components/Editor/Editor";

export interface IHttpResponseBodyState {
  isFormattingError: boolean;
}
export interface IHttpResponseBodyProps {
  responseBody: string;
  responseBodyType: string;
  recordedResponseBody: string;
  showBody: boolean;
  isOutgoingRequest: boolean;
  tabId: string;
  updateParam: UpdateParamHandler;
  maximizeEditorHeight: boolean;
}

class HttpResponseBody extends Component<
  IHttpResponseBodyProps,
  IHttpResponseBodyState
> {
  private editorRef: CodeMirror.Editor;
  constructor(props: IHttpResponseBodyProps) {
    super(props);
    this.formatHandler = this.formatHandler.bind(this);
    this.state = {
      isFormattingError: false,
    };
  }

  shouldComponentUpdate(
    nextProps: IHttpResponseBodyProps,
    nextState: IHttpResponseBodyState
  ) {
    if (
      this.props.responseBody != nextProps.responseBody ||
      this.props.responseBodyType != nextProps.responseBodyType ||
      this.props.recordedResponseBody != nextProps.recordedResponseBody ||
      this.props.showBody != nextProps.showBody ||
      this.props.isOutgoingRequest != nextProps.isOutgoingRequest ||
      this.props.tabId != nextProps.tabId ||
      this.props.maximizeEditorHeight != nextProps.maximizeEditorHeight ||
      this.state.isFormattingError != nextState.isFormattingError
    ) {
      return true;
    }
    return false;
  }

  formatHandler() {
    this.setState({ isFormattingError: false });
    try {
      this.editorRef &&  (this.editorRef as any).format();
    } catch (error) {
      this.setState({ isFormattingError: true });
      console.error("Error in formatting", error);
    }
  }

  handleChange = (editorValue: string) => {
    const value =
      this.props.responseBodyType !== "json"
        ? JSON.stringify(editorValue)
        : editorValue;
    if (value !== this.props.recordedResponseBody) {
      this.props.updateParam(
        this.props.isOutgoingRequest,
        this.props.tabId,
        "recordedResponseBody",
        "recordedResponseBody",
        value
      );
      this.setState({ isFormattingError: false });
    }
  };

  tryParseStringToHTML(jsonString: string) {
    try {
      const htmlString = JSON.parse(jsonString);
      if (_.isString(htmlString)) {
        return htmlString;
      }
    } catch (error) {
      //Silently absorb error, happens in case of empty or null string or invalid json
    }
    return jsonString;
  }
  hideError = () => {
    this.setState({ isFormattingError: false });
  };

  render() {
    const { showBody, tabId } = this.props;
    const language =
      this.props.responseBodyType == "js"
        ? "javascript"
        : this.props.responseBodyType;
    const recordedResponseBody =
      language !== "json"
        ? this.tryParseStringToHTML(this.props.recordedResponseBody)
        : this.props.recordedResponseBody;
    const responseBody =
      language !== "json"
        ? this.tryParseStringToHTML(this.props.responseBody)
        : this.props.responseBody;

    return showBody ? (
      <div style={{ width: "100%", display: "flex", height: this.props.maximizeEditorHeight ? "calc(100vh - 90px)" : "600px" }}>
        <div style={{ width: "50%", paddingRight: "10px", height:'100%' }}>
          {this.state.isFormattingError && (
            <div style={{ color: "red" }}>
              Couldn't format. Please validate the document for any errors.
              <i
                className="fa fa-times"
                title="Hide error"
                aria-hidden="true"
                onClick={this.hideError}
              ></i>
            </div>
          )}
          <Editor 
            value={recordedResponseBody}
            onChange={this.handleChange}
            language={language}
            getEditorRef = {(editor)=> this.editorRef = editor}
          />
        </div>
        <div style={{ width: "50%", height:'100%'  }}>
          <Editor value={responseBody} language={language} readonly={true} />
        </div>
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseBody;
