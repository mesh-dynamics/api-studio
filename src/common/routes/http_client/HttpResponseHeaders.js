import React, { Component } from 'react';
// import "./styles_here.css";

import { diff as DiffEditor } from "react-ace";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-min-noconflict/ext-searchbox";
import "ace-builds/src-min-noconflict/ext-language_tools";
import "ace-builds/src-noconflict/theme-github";

import "./diff.css";

class HttpResponseHeaders extends Component {
    constructor(props) {
        super(props);
        const leftValue = this.props.recordedResponseHeaders;
        const rightValue = this.props.responseHeaders;
        this.state = {
            leftValue: leftValue,
            rightValue: rightValue,
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(value) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "recordedResponseHeaders", "recordedResponseHeaders", value[0]);
        this.props.updateParam(isOutgoingRequest, tabId, "responseHeaders", "responseHeaders", value[0]);
        this.setState({
            leftValue: value[0],
            rightValue: value[1]
        }); 
    }


    render() {
        const showHeaders = this.props.showHeaders;
        return showHeaders ? (
            <div style={{display: this.props.showHeaders === true ? "" : "none"}}>
                <DiffEditor
                    name="responseHeaders"
                    value={[this.props.recordedResponseHeaders, this.props.responseHeaders]}
                    width="100%"
                    mode="json"
                    setOptions={{
                        useWorker: false
                    }}
                    onChange={this.handleChange}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpResponseHeaders;