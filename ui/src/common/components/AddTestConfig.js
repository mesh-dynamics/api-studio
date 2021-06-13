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

import React from 'react';
import {Link} from "react-router-dom";

class AddTestConfig extends React.Component {
    render() {
        return (
            <div>
                <div className="div-label">
                    Test Configuration
                    <Link to="/configs">
                        <i className="fas fa-link pull-right link"></i>
                    </Link>
                </div>
                <div className="margin-top-10">
                    <div className="label-n">TEST NAME</div>
                    <div className="value-n"><input className="width-100" type="text"/></div>
                </div>
                <div className="margin-top-10">
                    <div className="label-n">SELECT COLLECTION</div>
                    <div className="value-n">
                        <select className="width-100">
                            <option value="">--Select--</option>
                        </select>
                    </div>
                </div>

                <div className="margin-top-10 filter-tc">
                    <div style={{paddingBottom: "5px", borderBottom: "1px solid #efefef"}}>
                        FILTER
                    </div>
                    <div className="margin-top-10 vertical-middle">
                        <input type="radio"/>&nbsp;
                        <span className="r-text"> All Paths</span>
                    </div>
                    <div className="margin-top-10">
                        <input type="radio"/>&nbsp;
                        <span className="r-text"> Specific Paths</span>
                        <input type="text" className={"pull-right width-50"}/>
                    </div>
                    <div className="margin-top-10">
                        <input type="radio"/>&nbsp;
                        <span className="r-text"> Specific Requests</span>
                        <input type="text" className={"pull-right width-50"}/>
                    </div>
                    <div className="margin-top-10">
                        <input type="radio"/>&nbsp;
                        <span className="r-text"> Mismatch Type</span>
                        <input type="text" className={"pull-right width-50"}/>
                    </div>
                    <div className="margin-top-15 margin-bottom-10">
                        <span className="cube-btn pull-right">RESET</span>
                        <span className="cube-btn pull-right margin-right-10">APPLY</span>&nbsp;&nbsp;
                    </div>
                </div>

                <div className="margin-top-10">
                    <div className="label-n">SELECT TEST INSTANCE FOR RUN</div>
                    <div className="value-n">
                        <select className="width-100">
                            <option value="">--Select--</option>
                        </select>
                    </div>
                </div>

                <div className="margin-top-10">
                    <div className="cube-btn width-100 text-center">SAVE TEST CONFIG</div>
                </div>
            </div>
        );
    }
}
export default AddTestConfig