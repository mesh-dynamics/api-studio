import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import MonacoEditor from "react-monaco-editor";
// import "./styles_here.css";

export interface IHttpRequestRawDataROProps{
    showRawData: boolean;
    rawData: string;
}

class HttpRequestRawDataRO extends Component<IHttpRequestRawDataROProps> {
    
    render() {
        const showRawData = this.props.showRawData;
        return showRawData ? (
            <div>
                 <MonacoEditor
                    value={this.props.rawData}
                    language="json"
                    key="rawData"
                    height="250px"
                    width="100%"
                    options={{
                        minimap: {enabled: false},
                        scrollBeyondLastLine: false,
                        scrollbar:{
                            alwaysConsumeMouseWheel: false
                        },
                        readOnly: true
                    }}                    
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpRequestRawDataRO;