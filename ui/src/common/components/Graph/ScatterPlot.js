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
import {
    LineChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, Legend, Line
} from 'recharts';

const data01 = [
    { x: '1-Mar', y: 0.1, z: 200 },
    { x: '2-Mar', y: 0.15, z: 260 },
    { x: '3-Mar', y: 0.24, z: 400 },
    { x: '4-Mar', y: 0.34, z: 280 },
    { x: '5-Mar', y: 0.28, z: 500 },
    { x: '6-Mar', y: 0.2, z: 200 },
    { x: '7-Mar', y: 0.18, z: 200 },
    { x: '8-Mar', y: 0.22, z: 200 },
    { x: '9-Mar', y: 0.24, z: 200 },
    { x: '10-Mar', y: 0.28, z: 200 },
];

class ScatterPlot extends Component {

    render() {
        const {timeline, app} = this.props;
        let graphData = [];
        for (const unit of timeline) {
            if (unit.results && unit.results.length > 0) {
                let tt = unit.timestamp.split(' ');
                let ot = tt[1].split(':');
                let obj = {
                    date: ot[0] + ':' + ot[1],
                };
                for (const res of unit.results) {
                    if (res.service && res.service.toLocaleLowerCase() == app.toLowerCase()) {
                        obj[res.service.toLowerCase()] = ((res.respnotmatched/(res.reqmatched + res.reqnotmatched + res.reqpartiallymatched)) * 100).toFixed(2);
                    }/* else {
                        obj['overall'] = ((res.respnotmatched/(res.reqmatched + res.reqnotmatched + res.reqpartiallymatched)) * 100).toFixed(2);
                    }*/

                }

                graphData.push(obj);
            }
        }

        graphData = graphData.reverse();

        console.log(graphData);

        return (
            <LineChart
                width={800}
                height={250}
                data={graphData}
                margin={{
                    top: 5, right: 30, left: 20, bottom: 5,
                }}
            >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis name="Error" unit="%"/>
                <Tooltip />
                <Legend />
                {/*<Line type="monotone" dataKey="overall" stroke="#8884d8" />*/}
                <Line type="monotone" dataKey={app} stroke="#82ca9d"  />
                {/*<Line type="monotone" dataKey="reviews" stroke="#6FCF97"  />
                <Line type="monotone" dataKey="ratings" stroke="#EB5757"  />
                <Line type="monotone" dataKey="details" stroke="#F2C94C"  />
                <Line type="monotone" dataKey="movieinfo" stroke="#F2C94C"  />*/}
            </LineChart>
        );
    }
}

export default ScatterPlot;
