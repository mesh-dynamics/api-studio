import React, { Component } from 'react'
import { APIListTable } from './APIListTable'

class APICatalogServiceView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("service");
    }

    render() {
        const {app, apiPaths, selectedService, startTime, endTime} = this.props;
        return (
            <div style={{display: "flex", flexDirection:"column" }}>
                {/* <div>
                    <img src="/assets/images/serviceGraph.png" style={{width:"60%", height:"60%"}}></img>
                </div> */}
                <div style={{width:"60%", height:"60%"}}>
                    <APIListTable app={app} apiPaths={apiPaths} selectedService={selectedService} startTime={startTime} endTime={endTime} />
                </div>
            </div>
        )
    }
}

export default APICatalogServiceView;