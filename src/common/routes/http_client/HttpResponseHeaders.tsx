import React, { Component } from "react";
import { DiffEditorDidMount, MonacoDiffEditor } from "react-monaco-editor";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
// import "./styles_here.css";

import "./diff.css";

// Start: Move from below declaration to HttpClientTabs (originating file).
// We may need to change param names in HttpClientTabs from where these are passed.
export declare type UpdateParamHandler = (
  isOutgoingRequest: boolean,
  tabId: string,
  type: string,
  key: string,
  value: string | boolean,
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
export interface IFormData {
  id: string;
  name: string;
  value: string;
  description: string;
  selected: boolean;
}
// End

export interface IHttpResponseHeadersProps {
  tabId: string;
  isOutgoingRequest: boolean;
  showHeaders: boolean;
  updateParam: UpdateParamHandler;
  recordedResponseHeaders: string;
  responseHeaders: string;
  maximizeEditorHeight: boolean;
}

class HttpResponseHeaders extends Component<IHttpResponseHeadersProps> {
  private editor: monacoEditor.editor.IStandaloneDiffEditor;
  constructor(props: IHttpResponseHeadersProps) {
    super(props);
  }


  formatHandler(){
    this.editor.getOriginalEditor().getAction('editor.action.formatDocument').run().then(()=>{});
    this.editor.getModifiedEditor().getAction('editor.action.formatDocument').run().then(()=>{});
  }

  editorDidMount: DiffEditorDidMount = (editor) => {
    this.editor = editor;
    const model = editor.getModel();
    if (model) {
      const { original, modified } = model;

      // const { original, modified } = editor.getModel();
      const { tabId, isOutgoingRequest } = this.props;

      modified.onDidChangeContent((event) => {
        this.props.updateParam(
          isOutgoingRequest,
          tabId,
          "responseHeaders",
          "responseHeaders",
          modified.getValue()
        );
      });

      original.onDidChangeContent((event) => {
        this.props.updateParam(
          isOutgoingRequest,
          tabId,
          "recordedResponseHeaders",
          "recordedResponseHeaders",
          original.getValue()
        );
      });
    }
  };

  render() {
    const { showHeaders, tabId } = this.props;
    return showHeaders ? (
      <div>
        <MonacoDiffEditor
          key={"responseHeaders" + tabId}
          width="100%"
          height={this.props.maximizeEditorHeight? "calc(100vh - 30px)": "600"}
          language="json"
          original={this.props.recordedResponseHeaders}
          value={this.props.responseHeaders}
          options={{
            wordWrap: "on",
            originalEditable: true,
            colorDecorators: true,
            readOnly: true,
            scrollbar: {
              alwaysConsumeMouseWheel: false,
            },
            enableSplitViewResizing: true,            
            automaticLayout: true,
            scrollBeyondLastLine: false,
            contextmenu: false,
          }}
          editorDidMount={this.editorDidMount}
        />
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseHeaders;
