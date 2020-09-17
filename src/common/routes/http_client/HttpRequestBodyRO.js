import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';

import HttpRequestFormDataRO from "./HttpRequestFormDataRO";
import HttpRequestRawDataRO from "./HttpRequestRawDataRO";

class HttpRequestBodyRO extends Component {

    render() {
        return (
            <div style={{display: this.props.showBody === true ? "" : "none"}}>
                <HttpRequestFormDataRO tabId={this.props.tabId} showFormData={this.props.showFormData} 
                    formData={this.props.formData} 
                    addOrRemoveParam={this.props.addOrRemoveParam} 
                    updateParam={this.props.updateParam}
                    isOutgoingRequest={this.props.isOutgoingRequest} 
                    updateAllParams={this.props.updateAllParams}
                    >
                </HttpRequestFormDataRO>
                <HttpRequestRawDataRO tabId={this.props.tabId} showRawData={this.props.showRawData} 
                    rawData={this.props.rawData}
                    updateParam={this.props.updateParam}
                    isOutgoingRequest={this.props.isOutgoingRequest} >

                </HttpRequestRawDataRO>
            </div>
        );
    }
}

export default HttpRequestBodyRO;