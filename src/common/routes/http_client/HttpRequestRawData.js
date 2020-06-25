import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
// import "./styles_here.css";

import AceEditor from "react-ace";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-min-noconflict/ext-searchbox";
import "ace-builds/src-min-noconflict/ext-language_tools";
import "ace-builds/src-noconflict/theme-github";

class HttpRequestRawData extends Component {
    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange(value) {
        const { tabId } = this.props;
        this.props.updateParam(tabId, "rawData", "rawData", value);
    }

    render() {
        const showRawData = this.props.showRawData;
        return showRawData ? (
            <div>
                <AceEditor
                    value={this.props.rawData}
                    mode="json"
                    theme="github"
                    name="rawData"
                    editorProps={{ $blockScrolling: true }}
                    height="250px"
                    width="100%"
                    onChange={this.handleChange}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpRequestRawData;