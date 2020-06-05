import React, { Component } from 'react'
import { APICountTable } from './APICountTable'
import { APIRequestsTable } from './APIRequestsTable'
import './APICatalog.css';

class APICatalogAPIView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("api");
    }

    render() {
        const {selectedService, selectedApiPath, apiCount, apiTrace,app} = this.props;
        
        return (
            <div style={{display: "flex", flexDirection: "column" }}>
                <div className="margin-top-10">
                    <span style={{fontWeight: 300}}>API</span>
                    <p><b>{selectedApiPath}</b></p>
                </div>
                <div className="api-catalog-bordered-box width-50" style={{minHeight: "250px"}}>
                    <p className="api-catalog-box-title">FROM SERVICE</p>
                    <APICountTable apiCount={apiCount}/>
                </div>
                <div className="api-catalog-bordered-box">
                    <p className="api-catalog-box-title">REQUESTS</p>
                    <APIRequestsTable selectedService={selectedService} selectedApiPath={selectedApiPath} apiTrace={apiTrace} app={app}/>
                </div>
            </div>
        )
    }
}

export default APICatalogAPIView;