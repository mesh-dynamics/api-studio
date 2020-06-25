import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';

// import "./styles_here.css";

import HttpResponseHeaders from "./HttpResponseHeaders";
import HttpResponseBody from "./HttpResponseBody";

class HttpResponseMessage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            status: "",
            showHeaders: false,
            showBody: true,
            responseBodyType: "json"
        };
        this.onChangeValue = this.onChangeValue.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    onChangeValue(event) {
        this.setState({
            showHeaders: event.target.value === "showHeaders",
            showBody: event.target.value === "showBody"
        });
    }

    handleChange(event) {
        this.setState({
            responseBodyType: event.target.value
        })
    }


    render() {
        return (
            <div style={{marginTop: "18px"}}>
                <div style={{fontSize: "11px"}}>RESPONSE</div>
                <div style={{marginTop: "7px", marginBottom: "0px"}}>
                    <div className="" style={{display: "inline-block", paddingRight: "18px", opacity: "0.7", fontSize: "12px", width: "50px"}}>
                        VIEW
                    </div>
                    <div className="" style={{display: "inline-block", paddingRight: "10px", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "9px"}} value="showHeaders"
                            name="fieldType" checked={this.state.showHeaders} onChange={this.onChangeValue} />
                            Headers
                    </div>
                    <div className="" style={{display: "inline-block", fontSize: "12px"}}>
                        <input type="radio" style={{marginTop: "0px", marginRight: "9px"}} value="showBody"
                            name="fieldType" checked={this.state.showBody} onChange={this.onChangeValue} />
                            Body
                    </div>
                    <div className="" style={{display: this.state.showBody ? "inline-block" : "none", fontSize: "12px", marginLeft: "9px"}}>
                        <FormGroup bsSize="small">
                            <FormControl componentClass="select" placeholder="Method" style={{fontSize: "12px"}} name="responseBodyType" value={this.props.responseBodyType} onChange={this.handleChange}>
                                <option value="json">JSON</option>
                                <option value="txt">Text</option>
                                <option value="html">HTML</option>
                                <option value="xml">XML</option>
                                <option value="js">JavaScript</option>
                                <option value="auto">Auto</option>
                            </FormControl>
                        </FormGroup>
                    </div>
                </div>
                <div style={{opacity: "0.7", fontSize: "12px", marginBottom: "12px"}}>HTTP RESPONSE STATUS: <b>{this.props.responseStatus + " " + this.props.responseStatusText}</b></div>
                <div>
                    <HttpResponseHeaders tabId={this.props.tabId}
                        showHeaders={this.state.showHeaders} 
                        responseHeaders={this.props.responseHeaders}
                        recordedResponseHeaders={this.props.recordedResponseHeaders}
                        updateParam={this.props.updateParam} >
                        </HttpResponseHeaders>
                    <HttpResponseBody tabId={this.props.tabId}
                        showBody={this.state.showBody}
                        responseBody={this.props.responseBody}
                        recordedResponseBody={this.props.recordedResponseBody}
                        updateParam={this.props.updateParam}
                        responseBodyType={this.state.responseBodyType} >
                        </HttpResponseBody>
                </div>
            </div>
        );
    }
}

export default HttpResponseMessage;