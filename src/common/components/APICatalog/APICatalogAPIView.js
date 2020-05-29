import React, { Component } from 'react'
import { APIListTable } from './APIListTable'
import { APIRequestsTable } from './APIRequestsTable'

class APICatalogAPIView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("api");
    }

    render() {
        const {selectedService, selectedApiPath, apiCount, apiTrace} = this.props;
        
        return (
            <div style={{display: "flex", flexDirection: "column" }}>
                <div style={{margin: "10px"}}>
                    <p>API</p>
                    <p><b>{selectedApiPath}</b></p>
                </div>
                <div style={{margin: "10px"}}>
                    Count: {apiCount}
                </div>
                <label style={{marginLeft:"10px"}}>REQUESTS</label>
                <APIRequestsTable selectedService={selectedService} selectedApiPath={selectedApiPath} apiTrace={apiTrace}/>
            </div>
        )
    }
}

export default APICatalogAPIView;