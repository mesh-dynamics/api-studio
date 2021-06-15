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

import React from 'react'
import { connect } from "react-redux";
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';
import MultiSelect from '../MultiSelect/MultiSelect';
import { Glyphicon } from 'react-bootstrap';
import { findGoldenOrCollectionInSource } from '../../utils/api-catalog/api-catalog-utils';

function APICatalogTopFilters(props) {
    const { 
        apiCatalog: { 
            selectedService, 
            services, 
            apiPaths, 
            selectedApiPath, 
            selectedSource, 
            selectedCollection, 
            selectedGolden 
        },
        gcBrowse: {
            userGoldens,
            actualGoldens
        } 
    } = props;

    const selectedItem = findGoldenOrCollectionInSource({
        selectedSource, 
        selectedCollection, 
        selectedGolden, 
        userGoldens, 
        actualGoldens                               
    });

    const onChangeService = React.useCallback((value) => {
        const { dispatch } = props;
        dispatch(apiCatalogActions.handleFilterChange("selectedService", value));
    });

    const onChangeAPI = React.useCallback((value) => {
        const { dispatch } = props;
        dispatch(apiCatalogActions.handleFilterChange("selectedApiPath", value));
    });

    let servicesToDisplay = [], apiPathsToDisplay= [];

    //Sometimes, even if nothing is selected, it fetches old values, which should not be displayed.
    if(selectedSource && selectedItem && selectedItem.id && (selectedCollection || selectedGolden || selectedSource == "Capture")){
        servicesToDisplay = services;
        apiPathsToDisplay = apiPaths;
    }

    return <div className="topFilters">
        <MultiSelect options={servicesToDisplay} value={selectedService} onChange={onChangeService} title="SERVICE" placeholder="Search Service..." />
        <div className="left-arrow-glyph">
            <Glyphicon glyph="play" />
        </div>
        <MultiSelect options={apiPathsToDisplay}  value={selectedApiPath} onChange={onChangeAPI} title="API"  placeholder="Search API..." />
    </div>
}


const mapStateToProps = (state) => ({
    apiCatalog: state.apiCatalog,
    gcBrowse: state.gcBrowse
});

const connectedAPICatalogTopFilters = connect(mapStateToProps)(APICatalogTopFilters);

export default connectedAPICatalogTopFilters;
export { connectedAPICatalogTopFilters as APICatalogTopFilters }