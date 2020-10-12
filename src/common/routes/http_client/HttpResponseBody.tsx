import React, { Component } from "react";
import { MonacoDiffEditor } from "react-monaco-editor";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
import "./diff.css";

export interface IHttpResponseBodyProps {
  responseBody: string;
  responseBodyType: string;
  recordedResponseBody: string;
  showBody: boolean;
  isOutgoingRequest: boolean;
  tabId: string;
  updateParam: UpdateParamHandler;
}

class HttpResponseBody extends Component<IHttpResponseBodyProps> {

  private editor: monacoEditor.editor.IStandaloneDiffEditor;
  constructor(props: IHttpResponseBodyProps) {
    super(props);
  }

  updateDimensions() {
    this.editor && this.editor.layout();
  }

  componentDidMount() {
    window.addEventListener("resize", this.updateDimensions.bind(this));
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.updateDimensions.bind(this));
  }

  shouldComponentUpdate(nextProps: IHttpResponseBodyProps) {
    if (
      this.props.responseBody != nextProps.responseBody ||
      this.props.responseBodyType != nextProps.responseBodyType ||
      this.props.recordedResponseBody != nextProps.recordedResponseBody ||
      this.props.showBody != nextProps.showBody ||
      this.props.isOutgoingRequest != nextProps.isOutgoingRequest ||
      this.props.tabId != nextProps.tabId
    ) {
      return true;
    }
    return false;
  }
  editorDidMount = (editor) => {
    this.editor = editor;
    const { original, modified } = editor.getModel()!;
    
    // Following function is cached and editorDidMount is called only once at the time of first page load, not when props changing. 
    // So any param from props should be taken fresh from props.
    modified.onDidChangeContent((event) => {
      this.props.updateParam(
        this.props.isOutgoingRequest,
        this.props.tabId,
        "responseBody",
        "responseBody",
        modified.getValue()
      );
    });
    original.onDidChangeContent((event) => {
      this.props.updateParam(
        this.props.isOutgoingRequest,
        this.props.tabId,
        "recordedResponseBody",
        "recordedResponseBody",
        original.getValue()
      );
    });
  };

  render() {
    const { showBody, tabId } = this.props;
    const language =
      this.props.responseBodyType == "js"
        ? "javascript"
        : this.props.responseBodyType;

    return showBody ? (
      <div>
        <MonacoDiffEditor
          width="100%"
          height="600"
          language={language}
          original={this.props.recordedResponseBody}
          value={this.props.responseBody}
          options={{
            wordWrap: "on",
            originalEditable: true,
            colorDecorators: true,
            readOnly: true,
            scrollbar: {
              alwaysConsumeMouseWheel: false,
            },
          }}
          editorDidMount={this.editorDidMount}
        />
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseBody;
