import React, { Component } from "react";
import { MonacoDiffEditor } from "react-monaco-editor";
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

  componentDidUpdate() {
    this.resetOptions();
  }

  formatHandler() {
    this.editor
      .getOriginalEditor()
      .getAction("editor.action.formatDocument")
      ?.run()
      .then(() => {});
    this.editor
      .getModifiedEditor()
      .getAction("editor.action.formatDocument")
      ?.run()
      .then(() => {});
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

  resetOptions = () => {
    if (this.editor) {
      this.compareAndSetOptions(this.editor.getOriginalEditor());
      this.compareAndSetOptions(this.editor.getModifiedEditor());
    }
  };
  
  editorDidMount = (editor: monacoEditor.editor.IStandaloneDiffEditor) => {
    this.editor = editor;
    const { original, modified } = editor.getModel()!;
    this.resetOptions();
    // Following function is cached and editorDidMount is called only once at the time of first page load, not when props changing.
    // So any param from props should be taken fresh from props.
    modified.onDidChangeContent((event) => {
      const value =
        this.props.responseBodyType !== "json"
          ? JSON.stringify(modified.getValue())
          : modified.getValue(); //
      this.props.updateParam(
        this.props.isOutgoingRequest,
        this.props.tabId,
        "responseBody",
        "responseBody",
        value
      );
    });
    original.onDidChangeContent((event) => {
      const value =
        this.props.responseBodyType !== "json"
          ? JSON.stringify(original.getValue())
          : original.getValue(); //
      this.props.updateParam(
        this.props.isOutgoingRequest,
        this.props.tabId,
        "recordedResponseBody",
        "recordedResponseBody",
        value
      );
      this.resetOptions();
    });
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
      <MonacoDiffEditor
        width="100%"
        height={this.props.maximizeEditorHeight ? "calc(100vh - 30px)" : "600"}
        language={language}
        original={recordedResponseBody}
        value={responseBody}
        options={{
          wordWrap: "bounded",
          wordWrapMinified: true,
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
