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
  maximizeEditorHeight: boolean;
}

class HttpResponseBody extends Component<IHttpResponseBodyProps> {
  private editor: monacoEditor.editor.IStandaloneDiffEditor;
  constructor(props: IHttpResponseBodyProps) {
    super(props);
  }

  shouldComponentUpdate(nextProps: IHttpResponseBodyProps) {
    if (
      this.props.responseBody != nextProps.responseBody ||
      this.props.responseBodyType != nextProps.responseBodyType ||
      this.props.recordedResponseBody != nextProps.recordedResponseBody ||
      this.props.showBody != nextProps.showBody ||
      this.props.isOutgoingRequest != nextProps.isOutgoingRequest ||
      this.props.tabId != nextProps.tabId ||
      this.props.maximizeEditorHeight != nextProps.maximizeEditorHeight
    ) {
      return true;
    }
    return false;
  }
  formatHandler() {
    this.editor
      .getOriginalEditor()
      .getAction("editor.action.formatDocument")
      .run()
      .then(() => {});
    this.editor
      .getModifiedEditor()
      .getAction("editor.action.formatDocument")
      .run()
      .then(() => {});
  }
  editorDidMount = (editor: monacoEditor.editor.IStandaloneDiffEditor) => {
    this.editor = editor;

    const { original, modified } = editor.getModel()!;
    const { tabId, isOutgoingRequest } = this.props;
    modified.onDidChangeContent((event) => {
      this.props.updateParam(
        isOutgoingRequest,
        tabId,
        "responseBody",
        "responseBody",
        modified.getValue()
      );
    });
    original.onDidChangeContent((event) => {
      this.props.updateParam(
        isOutgoingRequest,
        tabId,
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
      <MonacoDiffEditor
        width="100%"
        height={this.props.maximizeEditorHeight ? "calc(100vh - 30px)" : "600"}
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
          automaticLayout: true,
          scrollBeyondLastLine: false,
          contextmenu: false,
        }}
        editorDidMount={this.editorDidMount}
      />
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseBody;
