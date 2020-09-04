import React, { useState } from 'react'
import { connect } from "react-redux";
import _ from "lodash";
import { apiCatalogActions } from '../../actions/api-catalog.actions';
import MultiSelect from '../MultiSelect/MultiSelect';
import { Glyphicon } from 'react-bootstrap';

function APICatalogTopFilters(props) {
    const { apiCatalog: { selectedService, services, apiPaths, selectedApiPath } } = props;

    const [selectedValue, setSelectedValue] = useState([selectedService]);
    const onChangeService = React.useCallback((value) => {
        const { dispatch } = props;
        dispatch(apiCatalogActions.handleFilterChange("selectedService", value));
    });
    const onChangeAPI = React.useCallback((value) => {
        const { dispatch } = props;
        dispatch(apiCatalogActions.handleFilterChange("selectedApiPath", value));
    });

    return <div className="topFilters">
        <MultiSelect options={services} value={selectedService} onChange={onChangeService} title="SERVICE" placeholder="Search Service..." />
        <div className="left-arrow-glyph">
            <Glyphicon glyph="play" />
        </div>
        <MultiSelect options={apiPaths}  value={selectedApiPath} onChange={onChangeAPI} title="API"  placeholder="Search API..." />
    </div>
}


const mapStateToProps = (state) => ({
    apiCatalog: state.apiCatalog,
});

const connectedAPICatalogTopFilters = connect(mapStateToProps)(APICatalogTopFilters);

export default connectedAPICatalogTopFilters;
export { connectedAPICatalogTopFilters as APICatalogTopFilters }