import React, { Component } from 'react'
import { connect } from "react-redux";
import { APICountTable } from './APICountTable'
import { APIRequestsTable } from './APIRequestsTable'
import './APICatalog.scss';

class APICatalogAPIView extends Component {
    componentDidMount() {
        this.props.setCurrentPage("api");
    }

    render() {
        const { apiCatalog: {apiFacets, selectedService, selectedApiPath, selectedInstance}, app } = this.props;
        return (
            <div style={{ display: "flex", flexDirection: "column" }}>
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
