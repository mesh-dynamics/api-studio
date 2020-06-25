import React, { Component } from 'react'
import { connect } from "react-redux";
import { APICountTable } from './APICountTable'
import { APIRequestsTable } from './APIRequestsTable'
import './APICatalog.css';

class APICatalogAPIView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("api");
    }

    render() {
        const { apiCatalog: {selectedApiPath,apiCount}, app } = this.props;

        return (
            <div style={{ display: "flex", flexDirection: "column" }}>
                <div className="margin-top-10">
                    <span style={{ fontWeight: 300 }}>API</span>
                    <p><b>{selectedApiPath}</b></p>
                </div>
                <div className="api-catalog-bordered-box width-50">
                    <p className="api-catalog-box-title">FROM SERVICE</p>
                    <APICountTable apiCount={apiCount} />
                </div>
                <div className="api-catalog-bordered-box">
                    <APIRequestsTable
                        app={app}
                    />
                </div>
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    apiCatalog: state.apiCatalog
});

const connectedAPICatalogAPIView = connect(mapStateToProps)(APICatalogAPIView);

export default connectedAPICatalogAPIView;
export { connectedAPICatalogAPIView as APICatalogAPIView }
