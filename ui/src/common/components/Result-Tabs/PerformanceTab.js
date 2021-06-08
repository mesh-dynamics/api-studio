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
