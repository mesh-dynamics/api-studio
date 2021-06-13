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

import React, { Fragment, useState } from "react";
import PropTypes from "prop-types";
import ViewType from "./ViewType";
import Table from "./Table";

import { 
    VIEW_TYPE, 
    REQUEST_RULE,
    REQUEST_TABS
} from "../../utils/enums/golden-visibility";


const RequestRules = (props) => {
    const { type, rules } = props;

    const [viewExpanded, setViewExpanded] = useState(true);

    const [selectedRequestTab, setSelectedRequestTab] = useState(REQUEST_TABS.HEADERS);

    const [requestViewType, setRequestViewType] =  useState(VIEW_TYPE.TABLE);

    const handleRequestTabClick = (view) => {
        setSelectedRequestTab(view);
        setRequestViewType(VIEW_TYPE.TABLE);
    };

    const getTableDataForView = () => {
        const tableData = {
            [REQUEST_TABS.HEADERS]: rules.headers.table,
            [REQUEST_TABS.QUERY_PARAMS]: rules.queryParams.table,
            [REQUEST_TABS.FORM_PARAMS]: rules.formParams.table,
            [REQUEST_TABS.BODY]: rules.body.table,
        };

        return tableData[selectedRequestTab];
    };

    const getJsonDataForView = () => {
        const jsonData = {
            [REQUEST_TABS.HEADERS]: rules.headers.json,
            [REQUEST_TABS.QUERY_PARAMS]: rules.queryParams.json,
            [REQUEST_TABS.FORM_PARAMS]: rules.formParams.json,
            [REQUEST_TABS.BODY]: rules.body.json,
        };

        return JSON.stringify(jsonData[selectedRequestTab], undefined, 4);
    };


    return (
        <Fragment>
            <div className="gv-sub-section-banner">
                {type === REQUEST_RULE.MATCH && <span>Match Rules</span>}
                
                {type === REQUEST_RULE.COMPARE && <span>Compare Rules</span>}

                <span onClick={() => setViewExpanded(!viewExpanded)} className="gv-link">
                {
                    viewExpanded 
                    ? <i className="fa fa-minus" aria-hidden="true"></i>
                    : <i className="fa fa-plus" aria-hidden="true"></i>
                }
                </span>
            </div>
            {
                viewExpanded &&
                <Fragment>
                    <div className="gv-tab-container">
                        <div 
                            onClick={() => handleRequestTabClick(REQUEST_TABS.HEADERS)} 
                            className={selectedRequestTab === REQUEST_TABS.HEADERS ? "gv-tab-button-selected" : "gv-tab-button"}
                        >
                            Headers
                        </div>
                        <div 
                            onClick={() => handleRequestTabClick(REQUEST_TABS.QUERY_PARAMS)} 
                            className={selectedRequestTab === REQUEST_TABS.QUERY_PARAMS ? "gv-tab-button-selected" : "gv-tab-button"}
                        >
                            Query Params
                        </div>
                        <div 
                            onClick={() => handleRequestTabClick(REQUEST_TABS.FORM_PARAMS)} 
                            className={selectedRequestTab === REQUEST_TABS.FORM_PARAMS ? "gv-tab-button-selected" : "gv-tab-button"}
                        >
                            Form Params
                        </div>
                        <div 
                            onClick={() => handleRequestTabClick(REQUEST_TABS.BODY)} 
                            className={selectedRequestTab === REQUEST_TABS.BODY ? "gv-tab-button-selected" : "gv-tab-button"}
                        >
                            Body
                        </div>
                    </div>
                    <div className="gv-contract-container">            
                        <ViewType viewType={requestViewType} setViewType={setRequestViewType} />
                        {
                            requestViewType === VIEW_TYPE.TABLE && 
                                <Table value={getTableDataForView()} />
                        }
                        {
                            requestViewType === VIEW_TYPE.JSON &&
                            <div className="gv-textarea-container">
                                <textarea 
                                    readOnly 
                                    rows="8" 
                                    cols="40"
                                    className="gv-textarea" 
                                    value={getJsonDataForView()} 
                                ></textarea>
                            </div>
                        }
                    </div>
                </Fragment>
            }
        </Fragment>
    );
}

RequestRules.propTypes = {
    rules: PropTypes.object.isRequired,
    type: PropTypes.oneOf([REQUEST_RULE.COMPARE, REQUEST_RULE.MATCH])
}

export default RequestRules;


