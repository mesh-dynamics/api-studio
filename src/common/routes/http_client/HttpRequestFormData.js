import React, { Component } from 'react';
import { Glyphicon, FormGroup, Button, FormControl, Radio, ControlLabel, Checkbox } from 'react-bootstrap';
// import "./styles_here.css";

class HttpRequestFormData extends Component {
    constructor(props) {
        super(props);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleChange = this.handleChange.bind(this);
    }

    handleAdd() {
        const { tabId } = this.props;
        this.props.addOrRemoveParam(tabId, "formData", "add");
    }

    handleDelete(id) {
        const { tabId } = this.props;
        this.props.addOrRemoveParam(tabId, "formData", "delete", id);
    }

    handleChange(id, evt) {
        const { tabId } = this.props;
        this.props.updateParam(tabId, "formData", evt.target.name, evt.target.value, id);
    }

    render() {
        return (
            <div style={{display: this.props.showFormData === true ? "" : "none"}}>
                {this.props.formData.length > 0 && (
                    <div style={{marginBottom: "1px"}}>
                        <div style={{display: "inline-block", width: "2%", paddingRight: "9px"}}> 
                            <FormGroup bsSize="small" style={{marginBottom: "0px", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "20%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>NAME</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "20%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>VALUE</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "54%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}>DESCRIPTION</ControlLabel>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "4%", paddingRight: "9px"}}>
                            <FormGroup bsSize="small" style={{marginBottom: "0px"}}>
                                <ControlLabel style={{fontWeight: "normal", fontSize: "11px"}}></ControlLabel>
                            </FormGroup>
                        </div>
                    </div>
                )}
                {this.props.formData.map(eachHeader => {return (
                    <div style={{marginBottom: "1px"}} key={eachHeader.id}>
                        <div style={{display: "inline-block", width: "2%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "none", textAlign: "center"}}>
                                <input type="checkbox" style={{marginTop: "0px", padding: "5px"}} />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "20%", paddingRight: "9px"}}> 
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" 
                                value={eachHeader.name} name="name" onChange={this.handleChange.bind(this, eachHeader.id)}/>
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "20%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="" value={eachHeader.value} name="value" onChange={this.handleChange.bind(this, eachHeader.id)} />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "54%", paddingRight: "9px"}}>
                            <FormGroup style={{marginBottom: "0px", fontSize: "12px"}}>
                                <FormControl style={{fontSize: "12px", border: "0px", borderTop: "1px solid #ccc"}} type="text" placeholder="optional" 
                                value={eachHeader.description} name="description" onChange={this.handleChange.bind(this, eachHeader.id)} />
                            </FormGroup>
                        </div>
                        <div style={{display: "inline-block", width: "4%", paddingRight: "9px"}} 
                                onClick={this.handleDelete.bind(this, eachHeader.id)} > 
                            <FormGroup style={{marginBottom: "0px", backgroundColor: "#ffffff", textAlign: "center", padding: "5px"}}>
                                <Glyphicon style={{fontSize: "16px", top: "5px"}} glyph="remove-sign" /> 
                            </FormGroup>
                        </div>
                    </div>
                )})}
                <div style={{ marginTop: "5px", marginRight: "7px"}}>
                    <div style={{display: "inline-block", width: "100%"}}> 
                        <Button block style={{paddingLeft: "6px", textAlign: "left"}} onClick={this.handleAdd}>
                            <span style={{fontSize: "14px", paddingRight: "18px", opacity: "0.7"}}> + </span>
                            <span style={{paddingRight: "14px", opacity: "0.7", fontSize: "11px"}}>Add form data</span>
                        </Button>
                    </div>
                </div>
            </div>
        );
    }
}

export default HttpRequestFormData;