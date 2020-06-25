import React, { Component } from 'react'
import { APIListTable } from './APIListTable'
import './APICatalog.css';

class APICatalogServiceView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("service");
    }

    render() {
        const {app, apiPaths, selectedService, startTime, endTime} = this.props;
        return (
            <div style={{display: "flex", flexDirection:"column" }}>
                <div className="margin-top-10">
                    <span style={{fontWeight: 300}}>SERVICE</span>
                    <p><b>{selectedService}</b></p>
                </div>
                <div className="api-catalog-bordered-box width-50" style={{width:"60%", height:"60%"}}>
                    <p className="api-catalog-box-title">INCOMING</p>
                    <APIListTable app={app} apiPaths={apiPaths} selectedService={selectedService} startTime={startTime} endTime={endTime} />
                </div>
            </div>
        )
    }
}

export default APICatalogServiceView;