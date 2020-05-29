import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
// import "./styles_here.css";

import { diff as DiffEditor } from "react-ace";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-min-noconflict/ext-searchbox";
import "ace-builds/src-min-noconflict/ext-language_tools";
import "ace-builds/src-noconflict/theme-github";

import "./diff.css";

const defaultValue = ``;

class HttpResponseHeaders extends Component {
    constructor(props) {
        super(props);
        this.state = { 
            //value: [defaultValue, this.props.responseHeaders]
            value: defaultValue
        };
        this.onChange = this.onChange.bind(this);
    }

    onChange(newValue) {
        this.setState({
            value: newValue[0]
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
                    onChange={this.onChange}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpResponseHeaders;