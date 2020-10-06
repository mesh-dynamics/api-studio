import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import MonacoEditor from "react-monaco-editor";
import {UpdateParamHandler} from './HttpResponseHeaders';
// import "./styles_here.css";


export interface IHttpRequestRawDataProps{
    showRawData: boolean;
    rawData: string;
    tabId: string;
    isOutgoingRequest : boolean,
    updateParam: UpdateParamHandler
}

class HttpRequestRawData extends Component<IHttpRequestRawDataProps> {
    constructor(props: IHttpRequestRawDataProps) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(value) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "rawData", "rawData", value);
    }

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
                    onChange={this.handleChange}
                    options={{
                        minimap: {enabled: false},
                        scrollBeyondLastLine: false,
                        scrollbar:{
                            alwaysConsumeMouseWheel: false
                        }
                    }}
                    
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpRequestRawData;