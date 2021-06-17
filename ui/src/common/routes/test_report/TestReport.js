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

import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import { Glyphicon, Table } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { cubeActions } from "../../actions";
import config from '../../config';
import _ from 'lodash';
import { cubeService } from '../../services';
import * as moment from 'moment';

class TestReport extends Component {

    constructor(props) {
        super(props);
        this.state = {
            replayId: "",
            timeLineData: {},
            loading: true,
            errorText: "",
        }
    }

    async componentDidMount() {
        const { user } = this.props;
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();

        const replayId = urlParameters["replayId"];
        let errorText = "";
        // get replay details for replayId 
        let testReport;
        try {
            testReport = await cubeService.fetchTestReport(replayId);
        } catch (e) {
            errorText =  "error fetching test report details: " + e;
            this.setState({errorText : errorText})
            return
        }

        if (errorText) {
            return;
        }

        this.setState({
            replayId: replayId, 
            replayStatus: testReport.replay,
            timeLineData: testReport.timeLineData,            
            loading: false,
        });
    }
    
    renderDetails = () => {
        const {replayId, replayStatus} = this.state;
        return <Fragment>
            <table className="table table-striped">
            <tbody>
                <tr><td>App</td><td>{replayStatus.app || "N/A"}</td></tr>
                <tr><td>Test Configuration</td><td>{replayStatus.testConfigName || "N/A"}</td></tr>
                <tr><td>Mocked Services</td><td>{replayStatus.mockServices ? _.join(replayStatus.mockServices, ", ") : "N/A"}</td></tr>
                <tr><td>Instance</td><td>{replayStatus.instanceId}</td></tr>
                <tr><td>Test API Paths</td><td>{(replayStatus.excludePaths ? "All paths excluding: " : "") + _.join(replayStatus.paths, ", ")}</td></tr>
                <tr><td>Golden</td><td>{replayStatus.goldenName}</td></tr>
                <tr><td>Test ID</td><td>{replayId}</td></tr>
                <tr><td>Recording ID</td><td>{replayStatus.recordingId}</td></tr>
                <tr><td>Date</td><td>{moment(replayStatus.creationTimeStamp).toLocaleString()}</td></tr>
            </tbody>
            </table>
        </Fragment>
    }

    renderAggregateSummary = () => {
        const {timeLineData} = this.state;
        
        return(
            <table className="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th colSpan={3}>Current Test</th>
                        <th colSpan={2}>Previous Tests ({timeLineData.previousCount})</th>
                    </tr>
                    <tr>
                        <th>
                            Tested
                        </th>
                        <th>
                            Errors
                        </th>
                        <th>
                            Error %
                        </th>
                        <th>
                            Average Error %
                        </th>
                        <th>
                            95% CI
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>
                            {timeLineData.currentAllTotal}
                        </td>
                        <td>
                            {timeLineData.currentAllResponseMismatches}
                        </td>
                        <td>
                            {(timeLineData.currentAllMismatchFraction * 100).toFixed(2) + "%"}
                        </td>
                        <td>
                            {timeLineData.previousAverage > 0 ? (timeLineData.previousAverage * 100).toFixed(2) + "%" : "N/A"}
                        </td>
                        <td>
                            {timeLineData.previousAll95CIRespMismatches > 0 ? timeLineData.previousAll95CIRespMismatches : "N/A"}
                        </td>
                    </tr>
                </tbody>
            </table>
        );
    }


    renderPathSummary = () => {
        const {timeLineData} = this.state;
        return(
            <table className="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th></th>
                        <th colSpan={3}>Current Test</th>
                        <th colSpan={2}>Previous Tests ({timeLineData.previousCount})</th>
                    </tr>
                    <tr>
                        <th>
                            API Path
                        </th>
                        <th>
                            Tested
                        </th>
                        <th>
                            Errors
                        </th>
                        <th>
                            Error %
                        </th>
                        <th>
                            Average Error %
                        </th>
                        <th>
                            95% CI
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {Object.entries(timeLineData.pathResults).map(([key, pathEntry]) => { 
                            return <tr key={key}>
                                <td>
                                    {key}
                                </td>
                                <td>
                                    {pathEntry.currentTotal}
                                </td>
                                <td>
                                    {pathEntry.currentResponseMismatches}
                                </td>
                                <td>
                                    {(pathEntry.currentMismatchFraction * 100).toFixed(2) + "%"}
                                </td>
                                <td>
                                    {pathEntry.previousMismatchFraction > 0 ? (pathEntry.previousMismatchFraction * 100).toFixed(2) + "%" : "N/A"}
                                </td>
                                <td>
                                    {pathEntry.previous95CIRespMismatches > 0 ? pathEntry.previous95CIRespMismatches : "N/A"}
                                </td>
                            </tr>
                        })}
                </tbody>
            </table>
        )
    }

    handleBackToDashboardClick = () => {
        const { history, dispatch } = this.props;
        dispatch(cubeActions.clearPathResultsParams());
    }

    render() {
        const {loading, errorText} = this.state;
        return (
            <div className="content-wrapper">
                    
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/test_results"} onClick={this.handleBackToDashboardClick}><span className="link-alt"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                </div>

                <div>
                    <h4>Test Report</h4>
                    {loading ? 
                    (errorText ? <div> <p>Report not available.</p> <p>{errorText}</p></div> : <div>Loading...</div>)
                    :
                    <div>
                        <br/>
                        <h5>Details</h5>
                        {this.renderDetails()}
                        <br/>
                        <h5>Aggregate Summary Results (Response Mismatches)</h5>
                        {this.renderAggregateSummary()}
                        <br/>
                        <h5>Response Mismatches</h5>
                        {this.renderPathSummary()}
                    </div>
                    }
                </div>

                
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
    user: state.authentication.user
})

const connectedTestReport = connect(mapStateToProps)(TestReport);
export default connectedTestReport;
export { connectedTestReport as TestReport };

