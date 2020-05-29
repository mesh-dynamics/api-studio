import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';

// import "./styles_here.css";
import HttpRequestFormData from "./HttpRequestFormData";
import HttpRequestRawData from "./HttpRequestRawData";
import HttpRequestBinaryData from "./HttpRequestBinaryData";

class HttpRequestBody extends Component {
    constructor(props) {
        super(props);
        this.state = { 
        };
    }


    render() {
        return (
            <div style={{display: this.props.showBody === true ? "" : "none"}}>
                <HttpRequestFormData showFormData={this.props.showFormData} 
                    formData={this.props.formData} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam} >

                </HttpRequestFormData>
                <HttpRequestRawData showRawData={this.props.showRawData} 
                    rawData={this.props.rawData}
                    updateParam={this.props.updateParam} >

                </HttpRequestRawData>
                {/* <HttpRequestBinaryData></HttpRequestBinaryData> */}
            </div>
        );
    }
}

export default HttpRequestBody;