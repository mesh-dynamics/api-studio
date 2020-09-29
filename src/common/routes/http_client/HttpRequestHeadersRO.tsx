import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";

export interface IHttpRequestHeadersROProps{
    showHeaders: boolean;
    headers: any[]; //TODO
}

class HttpRequestHeadersRO extends Component<IHttpRequestHeadersROProps> {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div style={{display: this.props.showHeaders === true ? "" : "none"}}>
                {this.props.headers.length > 0 && (
                    <div style={{marginBottom: "1px"}}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup bsSize="small" style={{marginBottom: "0px", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} disabled />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "505", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.headers.map(eachHeader => {return (
                    <div style={{marginBottom: "1px"}} key={eachHeader.id}>
                        <div style={{display: "inline-block", width: "3%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "none", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} disabled checked={eachHeader.selected} />                            
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "35%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" 
                                value={eachHeader.name} disabled name="name" />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "55%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" value={eachHeader.value} disabled name="value" />
                            </FormGroup>
                        </div>
                    </div>
                )})}
            </div>
        );
    }
}

export default HttpRequestHeadersRO;