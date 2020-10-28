import React, { Component } from "react";
import MonacoEditor from "react-monaco-editor";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
import "./diff.css";
import _ from "lodash";

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
  private lhsEditor: monacoEditor.editor.IStandaloneCodeEditor;
  constructor(props: IHttpResponseBodyProps) {
    super(props);
    this.formatHandler = this.formatHandler.bind(this);
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
    if (this.props.responseBodyType === "json") {
      //In case of json action.run() doesn't work, hence needs to format json manually. This is mostly due to jsonworker error.
      try {
        this.lhsEditor.setValue(
          JSON.stringify(JSON.parse(this.lhsEditor.getValue()), null, "\t")
        );
      } catch (error) {}
    }else{

      this.lhsEditor
      .getAction("editor.action.formatDocument")
      ?.run()
      .then(() => {});
    }
  }

  compareAndSetOptions(editor: monacoEditor.editor.IStandaloneCodeEditor) {
    const currentoptions = editor.getRawOptions();
    if (currentoptions.wordWrap != "bounded") {
      editor.updateOptions({
        wordWrap: "bounded",
        wordWrapMinified: true,
      });
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
      <div style={{ width: "100%", display: "flex" }}>
        <div style={{ width: "50%", paddingRight: "10px" }}>
          <MonacoEditor
            value={recordedResponseBody}
            language={language}
            key="rawLhsData"
            height={
              this.props.maximizeEditorHeight ? "calc(100vh - 30px)" : "600"
            }
            onChange={this.handleChange}
            options={{
              wordWrap: "bounded",
              wordWrapMinified: true,
              colorDecorators: true,

              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              scrollbar: {
                alwaysConsumeMouseWheel: false,
              },
              automaticLayout: true,
              contextmenu: false,
            }}
            editorDidMount={(editor) => {
              this.lhsEditor = editor;
            }}
          />
        </div>
        <div style={{ width: "50%" }}>
          <MonacoEditor
            value={responseBody}
            language={language}
            key="rawRhsData"
            height={
              this.props.maximizeEditorHeight ? "calc(100vh - 30px)" : "600"
            }
            options={{
              wordWrap: "bounded",
              wordWrapMinified: true,
              colorDecorators: true,

              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              scrollbar: {
                alwaysConsumeMouseWheel: false,
              },
              automaticLayout: true,
              contextmenu: false,
              readOnly: true,
            }}
          />
        </div>
      </div>
    ) : (
      <div></div>
    );
  }
}

export default HttpResponseBody;
