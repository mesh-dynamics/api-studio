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
    RESPONSE_TABS 
} from "../../utils/enums/golden-visibility";

const ResponseContract = (props) => {
    
    const [responseViewType, setResponseViewType] = useState(VIEW_TYPE.TABLE);

    const { handleExampleClick, responseContract: { body: { table, json }} } = props;

    return (
        <Fragment>
            <div className="gv-section-banner">
                <span>Response</span>
                <span onClick={() => handleExampleClick(RESPONSE_TABS.EXAMPLES)} className="gv-link">Examples</span>
            </div>
            <div className="gv-section-details">
                <div className="gv-tab-container">
                    <div className="gv-tab-button-selected gv-tab-single">Body</div>
                </div>
                <div className="gv-contract-container">
                    <ViewType viewType={responseViewType} setViewType={setResponseViewType} />
                    {
                        responseViewType === VIEW_TYPE.TABLE && 
                        <Table value={table} />
                    }
                    {
                        responseViewType === VIEW_TYPE.JSON &&
                        <div className="gv-textarea-container">
                            <textarea 
                                readOnly 
                                rows="15" 
                                cols="40"
                                className="gv-textarea" 
                                value={
                                    Object.keys(json).length !== 0 
                                    ? JSON.stringify(json, undefined, 4)
                                    : ""
                                } 
                            ></textarea>
                        </div>
                    }
                </div>                
            </div>
        </Fragment>
    );
}

ResponseContract.propTypes = {
    responseContract: PropTypes.object.isRequired,
    handleExampleClick: PropTypes.func.isRequired,
};


export default ResponseContract;
