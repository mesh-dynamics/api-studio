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
        const {timeline} = this.props;
        let graphData = [];
        for (const unit of timeline) {
            if (unit.results && unit.results.length > 0) {
                let obj = {
                    date: unit.timestamp,
                };
                for (const res of unit.results) {
                    if (res.service) {
                        obj[res.service] = ((res.respnotmatched/(res.reqmatched + res.reqnotmatched + res.reqpartiallymatched)) * 100).toFixed(2);
                    } else {
                        obj['overall'] = ((res.respnotmatched/(res.reqmatched + res.reqnotmatched + res.reqpartiallymatched)) * 100).toFixed(2);
                    }

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
                <Line type="monotone" dataKey="overall" stroke="#8884d8" />
                <Line type="monotone" dataKey="restwrapjdbc" stroke="#82ca9d"  />
                <Line type="monotone" dataKey="reviews" stroke="#6FCF97"  />
                <Line type="monotone" dataKey="ratings" stroke="#EB5757"  />
                <Line type="monotone" dataKey="details" stroke="#F2C94C"  />
                <Line type="monotone" dataKey="movieinfo" stroke="#F2C94C"  />
            </LineChart>
        );
    }
}

export default ScatterPlot;
