import  React , { Component, Fragment, createContext } from "react";

import HttpRequestMessage from "./HttpRequestMessage";
import HttpResponseMessage from "./HttpResponseMessage";

class HttpClient extends Component {

    constructor(props) {
        super(props);
        this.state = { 
            
        };
    }

    render() {
        return (
            <div>
                <HttpRequestMessage tabId={this.props.tabId}
                        requestId={this.props.requestId}
                        httpMethod={this.props.httpMethod}
                        httpURL={this.props.httpURL}
                        headers={this.props.headers} 
                        queryStringParams={this.props.queryStringParams}
                        bodyType={this.props.bodyType}
                        formData={this.props.formData} 
                        rawData={this.props.rawData}
                        rawDataType={this.props.rawDataType}
                        addOrRemoveParam={this.props.addOrRemoveParam} 
                        updateParam={this.props.updateParam}
                        updateBodyOrRawDataType={this.props.updateBodyOrRawDataType}
                        driveRequest={this.props.driveRequest}
                        showOutgoingRequests={this.props.showOutgoingRequests} >
                  </HttpRequestMessage>
                    <HttpResponseMessage tabId={this.props.tabId}
                        updateParam={this.props.updateParam}
                        responseStatus={this.props.responseStatus}
                        responseHeaders={this.props.responseHeaders}
                        responseBody={this.props.responseBody}
                        recordedResponseHeaders={this.props.recordedResponseHeaders}
                        recordedResponseBody={this.props.recordedResponseBody}
                        updateParam={this.props.updateParam} >
                    </HttpResponseMessage>
            </div>
        );
    }
}

export default HttpClient;
