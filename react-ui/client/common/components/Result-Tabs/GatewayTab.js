import React, { Component } from 'react';
import ScatterPlot from "../Graph/ScatterPlot";
import {Table} from "react-bootstrap";

class GatewayTab extends Component{
    constructor(props){
        super(props);
    }

    render() {
        const {res, resByPath} =  this.props;
        const error =  res.respnotmatched;
        const errorP = Math.round((100 * error/res.reqcnt) * 100) / 100;
        const incomplete = res.reqcnt - (res.respmatched + res.resppartiallymatched + res.respnotmatched);
        const incompleteP = Math.round((100 * incomplete/res.reqcnt) * 100) / 100;

        let errorByPath = resByPath.map(pathRes => (
            <tr>
                <td>{pathRes.path ? pathRes.path : '-'}</td>
                <td>{res.reqcnt}</td>
                <td>{pathRes.respnotmatched}</td>
                <td>{Math.round((100 * pathRes.respnotmatched/res.reqcnt) * 100) / 100}</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
                <td>-</td>
            </tr>
        ));

        return (
            <div className="tab-panel">
                <h4>Gateway @Movieinfo</h4>
                <div style={{width:'60%'}}>
                <Table striped bordered hover>
                    <thead>
                    <tr>
                        <th className="grey" colSpan="3">Current Test</th>
                        <th className="grey" colSpan="3">Previous Tests</th>
                    </tr>
                    <tr>
                        <th className="default">Reccomendation: Fail</th>
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
                        <td>{error}</td>
                        <td>{errorP}</td>
                        <td>0.63</td>
                        <td>0.78</td>
                        <td>-</td>
                    </tr>
                    <tr>
                        <td>Incomplete</td>
                        <td>{incomplete}</td>
                        <td>{incompleteP}</td>
                        <td>0.22</td>
                        <td>0.34</td>
                        <td>-</td>
                    </tr>
                    </tbody>
                </Table>
                </div>
                <ScatterPlot/>

                <div>
                    <h4>
                        Errors observed By Path
                    </h4>
                    <Table striped bordered hover>
                        <thead>
                        <tr>
                            <th className="default">Path</th>
                            <th className="default">Tested</th>
                            <th className="default">Error</th>
                            <th className="default">Error%</th>
                            <th className="default">Avg. Error%</th>
                            <th className="default">95% CI</th>
                            <th className="default">Recommendation</th>
                            <th className="default">7 Day Trend</th>
                        </tr>
                        </thead>
                        <tbody>
                            {errorByPath}
                        </tbody>
                    </Table>
                </div>
            </div>
        );

    }
}

export default GatewayTab;
