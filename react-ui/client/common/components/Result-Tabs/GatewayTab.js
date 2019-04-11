import React, { Component } from 'react';
import ScatterPlot from "../Graph/ScatterPlot";
import {Table} from "react-bootstrap";

class GatewayTab extends Component{
    constructor(props){
        super(props);
    }

    render() {
        return (
            <div className="tab-panel">
                <h4>All Results Gateway @Movieinfo</h4>
                <div style={{width:'60%'}}>
                <Table striped bordered hover>
                    <thead>
                    <tr>
                        <th className="grey" colSpan="3">Current Test</th>
                        <th className="grey" colSpan="3">Previous Test</th>
                    </tr>
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
                <ScatterPlot/>
            </div>
        );

    }
}

export default GatewayTab;
