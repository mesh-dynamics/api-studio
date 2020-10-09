import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
import "./HttpClient.css";
// import "./styles_here.css";

export interface IHttpRequestQueryStringROProps{
    showQueryParams: boolean;
    queryStringParams: any[];//TODO
}

class HttpRequestQueryStringRO extends Component<IHttpRequestQueryStringROProps> {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div style={{display: this.props.showQueryParams === true ? "" : "none"}}>
                {this.props.queryStringParams.length > 0 && (
                    <div style={{marginBottom: "1px"}}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup bsSize="small" style={{marginBottom: "0px", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} disabled/>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "55%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.queryStringParams.map(eachParam => {return (
                    <div style={{marginBottom: "1px"}} key={eachParam.id}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "none", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} checked={eachParam.selected} disabled />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" 
                                value={eachParam.name} name="name" disabled />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "55%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" value={eachParam.value} name="value" disabled />
                            </FormGroup>
                        </div>
                    </div>
                )})}
            </div>
        );
    }
}

export default HttpRequestQueryStringRO;