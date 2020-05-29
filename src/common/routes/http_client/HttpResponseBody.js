import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import { diff as DiffEditor } from "react-ace";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-min-noconflict/ext-searchbox";
import "ace-builds/src-min-noconflict/ext-language_tools";
import "ace-builds/src-noconflict/theme-github";

import "./diff.css";

const defaultValue = ``;

class HttpResponseBody extends Component {
    constructor(props) {
        super(props);
        this.state = {
            value: defaultValue,
            editor: null
        };
        this.onChange = this.onChange.bind(this);
        this.onLoad = this.onLoad.bind(this);
    }

    onChange(newValue) {
        this.setState({
            value: newValue[0]
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
                    mode="json"
                    setOptions={{
                        useWorker: false
                    }}
                    onChange={this.onChange}
                    onLoad={this.onLoad}
                />
            </div>
        ) : (<div></div>);
    }
}

export default HttpResponseBody;