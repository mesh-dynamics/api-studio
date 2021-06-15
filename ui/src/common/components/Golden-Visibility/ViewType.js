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

import React from "react";
import PropTypes from "prop-types";
import { VIEW_TYPE } from "../../utils/enums/golden-visibility";
import "./ViewType.css";

const ViewType = (props) => {

    const { viewType, setViewType } = props;

    return (
        <div className="gv-view-type-container">
            <span>View as : </span>
            <form className="gv-view-type-container-options">
                <div className="gv-view-type-option">
                    <input type="radio" name="gender" value="male" onChange={() => setViewType(VIEW_TYPE.TABLE)} checked={viewType === VIEW_TYPE.TABLE} /> 
                    <span className="gv-view-type-label">Table</span>
                </div>
                <div className="gv-view-type-option">
                    <input type="radio" name="gender" value="male"  onChange={() => setViewType(VIEW_TYPE.JSON)} checked={viewType === VIEW_TYPE.JSON} /> 
                    <span className="gv-view-type-label">JSON</span>
                </div>
            </form>
        </div>
    )
}

ViewType.propType = {
    viewType: PropTypes.string.isRequired,
    setViewType: PropTypes.func.isRequired
};

export default ViewType;
