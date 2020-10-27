import React, { Component } from "react";
import MonacoEditor from "react-monaco-editor";
import { UpdateParamHandler } from "./HttpResponseHeaders";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
// import "./styles_here.css";

export interface IHttpRequestRawDataProps {
  showRawData: boolean;
  rawData: string;
  tabId: string;
  isOutgoingRequest: boolean;
  updateParam: UpdateParamHandler;
}
export interface IHttpRequestRawDataState{
  showError: boolean;     
}

class HttpRequestRawData extends Component<IHttpRequestRawDataProps,IHttpRequestRawDataState> {
  private editor: monacoEditor.editor.IStandaloneCodeEditor;
  constructor(props: IHttpRequestRawDataProps) {
    super(props);
    this.state={
      showError: false
    }
    this.handleChange = this.handleChange.bind(this);
    this.formatHandler = this.formatHandler.bind(this);
  }

  handleChange(value: string) {
    const { tabId, isOutgoingRequest, rawData } = this.props;
    if (value !== rawData) {
      this.props.updateParam(
        isOutgoingRequest,
        tabId,
        "rawData",
        "rawData",
        value
      );
    }
  }
  formatHandler = ()=> {
    //Ideally following statement should work. Need to check later if this bug is resolved in any updates.
    // this.editor.getAction('editor.action.formatDocument').run().then(()=>{});
    this.setState({showError: false});
    try{
      this.editor.setValue(
        JSON.stringify(JSON.parse(this.editor.getValue()), null, "\t")
      );
    }catch(error){
      this.setState({showError: true});
    }    
  }

  editorDidMount = (editor: monacoEditor.editor.IStandaloneCodeEditor) => {
    this.editor = editor;
  };

  render() {
    const showRawData = this.props.showRawData;
    return showRawData ? (
      <div style={{ height: "100%", minHeight: "100px" }}>
        <div style={{ width: "100%", height: "30px" }}>
        {this.state.showError? <span style={{color: 'red'}}>Couldn't format. Please validate the document for any errors.</span>: <></>}
          <div style={{ float: "right" }}>
            <span
              className="btn btn-sm cube-btn text-center"
              style={{ padding: "2px 10px", display: "inline-block" }}
              title="Format document"
              onClick={this.formatHandler}
            >
              <i className="fa fa-align-center" aria-hidden="true"></i> Format
            </span>
          </div>
        </div>
        <MonacoEditor
          value={this.props.rawData}
          language="json"
          key="rawData"
          width="100%"
          onChange={this.handleChange}
          options={{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            scrollbar: {
              alwaysConsumeMouseWheel: false,
            },
            automaticLayout: true,
            formatOnPaste: true,
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

export default HttpRequestRawData;
