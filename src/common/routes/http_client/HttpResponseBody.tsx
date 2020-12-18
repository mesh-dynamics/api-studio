import React, { Component } from "react";
import MonacoEditor from "react-monaco-editor";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
import "./diff.css";
import _ from "lodash";
import * as monaco from "monaco-editor/esm/vs/editor/editor.api";

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
  isFormattingError: boolean;
}

class HttpResponseBody extends Component<
  IHttpResponseBodyProps,
  IHttpResponseBodyState
> {
  private lhsEditor: monacoEditor.editor.IStandaloneCodeEditor;
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
    if (this.props.responseBodyType === "json") {
      //In case of json action.run() doesn't work, hence needs to format json manually. This is mostly due to jsonworker error.
      try {
        this.lhsEditor.setValue(
          JSON.stringify(JSON.parse(this.lhsEditor.getValue()), null, "\t")
        );
      } catch (error) {
        this.setState({ isFormattingError: true });
        console.error("Error in formatting", error);
      }
    } else {
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
    if (this.lhsEditor) {
      const model = this.lhsEditor.getModel();
      if (model) {
        monaco.editor.setModelLanguage(model, language);
      }
    }
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
              folding:true,
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
