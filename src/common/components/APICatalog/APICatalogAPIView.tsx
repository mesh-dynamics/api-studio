import React, { Component } from 'react'
import { connect } from "react-redux";
import { APIRequestsTable } from './APIRequestsTable'
import './APICatalog.scss';
import { IApiCatalogState, IStoreState } from '../../reducers/state.types';
export interface IAPICatalogAPIViewProps{
    setCurrentPage: (page:string) => void;
    app: string; //This can be removed from props passed but can be taken from redux
    apiCatalog: IApiCatalogState;
}
class APICatalogAPIView extends Component<IAPICatalogAPIViewProps> {
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

const mapStateToProps = (state: IStoreState) => ({
    apiCatalog: state.apiCatalog
});

const connectedAPICatalogAPIView = connect(mapStateToProps)(APICatalogAPIView);

export default connectedAPICatalogAPIView;
export { connectedAPICatalogAPIView as APICatalogAPIView }
