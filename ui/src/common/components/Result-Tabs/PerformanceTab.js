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

import React, { Component } from 'react';
import {Table} from "react-bootstrap";

class PerformanceTab extends Component{
    constructor(props){
        super(props);
    }

    render() {
        return (
            <div>
                <div style={{width:'60%'}}>
                    <Table striped bordered hover>
                        <thead>
                        <tr>
                            <th className="default">Reccomendation Fail</th>
                            <th className="default">Error Count</th>
                            <th className="default">Error%</th>
                            <th className="default">Avg. Error%</th>
                            <th className="default">95% CI</th>
                            <th className="default">Trend</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td>Errors</td>
                            <td>72</td>
                            <td>0.66</td>
                            <td>0.63</td>
                            <td>0.78</td>
                            <td>Sparkline</td>
                        </tr>
                        <tr>
                            <td>Incomplete</td>
                            <td>72</td>
                            <td>0.66</td>
                            <td>0.63</td>
                            <td>0.78</td>
                            <td>Sparkline</td>
                        </tr>
                        </tbody>
                    </Table>
                </div>
            </div>
        );

    }
}

export default PerformanceTab;
