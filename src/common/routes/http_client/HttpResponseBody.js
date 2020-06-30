import React, { Component } from 'react';
import { diff as DiffEditor } from "react-ace";

import "ace-builds/webpack-resolver";

import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-xml";
import "ace-builds/src-noconflict/mode-html";
import "ace-builds/src-noconflict/mode-text";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-min-noconflict/ext-searchbox";
import "ace-builds/src-min-noconflict/ext-language_tools";
import "ace-builds/src-noconflict/theme-github";

import "./diff.css";

class HttpResponseBody extends Component {
    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(value) {
        const { tabId, isOutgoingRequest } = this.props;
        this.props.updateParam(isOutgoingRequest, tabId, "recordedResponseBody", "recordedResponseBody", value[0]);
        this.props.updateParam(isOutgoingRequest, tabId, "responseBody", "responseBody", value[1]);
    }

    render() {
        const { showBody, tabId } = this.props;
        return showBody ? (
            <div>
                <DiffEditor
                    name={"responseBody" + tabId}
                    value={[this.props.recordedResponseBody, this.props.responseBody]}
                    width="100%"
                    mode={this.props.responseBodyType}
                    theme="github"
                    setOptions={{
                        useWorker: false
                    }}
                    onChange={this.handleChange}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpResponseBody;