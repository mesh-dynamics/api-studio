import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import MonacoEditor from "react-monaco-editor";
import * as monacoEditor from "monaco-editor/esm/vs/editor/editor.api";
// import "./styles_here.css";

export interface IHttpRequestRawDataROProps{
    showRawData: boolean;
    rawData: string;
}
export interface IHttpRequestRawDataROState{
    showError: boolean;     
}

class HttpRequestRawDataRO extends Component<IHttpRequestRawDataROProps, IHttpRequestRawDataROState> {
    getFormattedValue = (value)=>{
      try{        
          return JSON.stringify(JSON.parse(value), null, "\t");
      }catch(error){
        //Error appears when document can not be formatted and having some syntax errors. No need to show any message.
      } 
      return value;
    }
    render() {
        const showRawData = this.props.showRawData;
        return showRawData ? (
            <div style={{ height: "100%", minHeight: "100px", marginTop:'42px' }}>
            
            <MonacoEditor
                    value={this.getFormattedValue(this.props.rawData)}
                    language="json"
                    key="rawData"
                    width="100%"
                    options={{
                        minimap: {enabled: false},
                        scrollBeyondLastLine: false,
                        scrollbar:{
                            alwaysConsumeMouseWheel: false
                        },
                        readOnly: true,
                        contextmenu: false,
                        automaticLayout: true,
                    }}                   
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpRequestRawDataRO;