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
        const defaultValue = this.props.recordedResponseBody;
        this.state = {
            value: defaultValue,
            editor: null
        };
        this.handleChange = this.handleChange.bind(this);
        this.onLoad = this.onLoad.bind(this);
    }

    handleChange(value) {
        const { tabId } = this.props;
        this.props.updateParam(tabId, "recordedResponseBody", "recordedResponseBody", value[0]);
        this.setState({
            value: value[0]
        });
    }

    onLoad(editor) {
        this.setState({
            editor: editor
        });
    }

    render() {
        const showBody = this.props.showBody;
        return showBody ? (
            <div>
                <DiffEditor
                    name="responseBody"
                    value={[this.state.value, this.props.responseBody]}
                    width="100%"
                    mode={this.props.responseBodyType}
                    theme="github"
                    setOptions={{
                        useWorker: false
                    }}
                    onChange={this.handleChange}
                    onLoad={this.onLoad}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpResponseBody;