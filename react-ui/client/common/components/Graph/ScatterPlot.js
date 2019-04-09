import React, { Component } from 'react';
import {
    ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine
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
        return (
            <ScatterChart
                width={800}
                height={200}
                margin={{
                    top: 20, right: 20, bottom: 20, left: 20,
                }}
            >
                <CartesianGrid vertical={false}/>
                <XAxis dataKey="x" name="Date" unit="" />
                <YAxis yAxisId="left" type="number" dataKey="y" name="Error" unit="%" stroke="#8884d8" />
                <ReferenceLine yAxisId="left" y={0.15} label="" stroke="yellow" />
                <ReferenceLine yAxisId="left" y={0.25} label="" stroke="red" />
                <Tooltip cursor={{ strokeDasharray: '3 3' }} />
                <Scatter yAxisId="left" name="A school" data={data01} fill="#8884d8" />
            </ScatterChart>
        );
    }
}

export default ScatterPlot;
