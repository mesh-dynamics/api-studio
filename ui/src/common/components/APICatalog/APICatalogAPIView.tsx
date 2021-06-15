/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        const { app } = this.props;
        return (
            <div style={{ display: "flex", flexDirection: "column" }}>
                <div className="api-catalog-bordered-box">
                    <APIRequestsTable app={app} />
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
