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
        const defaultValue = this.props.recordedResponseHeaders;
        this.state = {
            value: defaultValue
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(value) {
        const { tabId } = this.props;
        this.props.updateParam(tabId, "recordedResponseHeaders", "recordedResponseHeaders", value[0]);
        this.setState({
            value: value[0]
        });
    }


    render() {
        const showHeaders = this.props.showHeaders;
        return showHeaders ? (
            <div style={{display: this.props.showHeaders === true ? "" : "none"}}>
                <DiffEditor
                    name="responseHeaders"
                    value={[this.state.value, this.props.responseHeaders]}
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