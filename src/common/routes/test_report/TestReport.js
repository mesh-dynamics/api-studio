import React, { Component, Fragment } from 'react'
import { connect } from "react-redux";
import { Glyphicon, Table } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { cubeActions } from "../../actions";
import config from '../../config';
import _ from 'lodash';
import { cubeService } from '../../services';
import {processTimelineData, generatePathTableData} from '../../utils/test-report/test-report-utils.js'
import * as moment from 'moment';

class TestReport extends Component {

    constructor(props) {
        super(props);
        this.state = {
            replayId: "",
            testConfig: {},
            goldenName: "",
            pathTableData: [],
            timeStamp: "",
            timelineResults: {},
            recordingId: "",
            loading: true,
            errorText: "",
        }
    }

    async componentDidMount() {
        const {dispatch, cube} = this.props;
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();

        const replayId = urlParameters["replayId"];
        let errorText = "";
        // get replay details for replayId 
        let replayStatus;
        try {
            replayStatus = await cubeService.checkStatusForReplay(replayId);
        } catch (e) {
            errorText =  "error fetching replay details: " + e;
            console.error(errorText);
            this.setState({errorText : errorText})
            return
        }

        // get test config details if test config is present 
        // get results current and previous results
        const numResults = 30; // todo configurable?
        const [testConfig, timelineResults] = await Promise.all([
            replayStatus.testConfigName ? cubeService.getTestConfig(replayStatus.app, replayStatus.testConfigName) : Promise.resolve({}), 
            cubeService.fetchTimelineData(replayStatus.app, null, new Date(replayStatus.creationTimeStamp), null, numResults, replayStatus.testConfigName, replayStatus.goldenName) // todo user
        ]).catch(
            (e) => {
                errorText = "error fetching test config or timeline results: " + e;
                console.error(errorText);
                this.setState({errorText: errorText})
            }
        );

        if(timelineResults.numFound === 0) {
            // error
            errorText = "no results found";
            console.error(errorText);
            this.setState({errorText: errorText})
            return;
        }

        if (errorText) {
            return;
        }
    
        // if the exclude paths flag is true, no filter on the paths is needed since they won't be present
        const testPaths = replayStatus.excludePaths ? [] : replayStatus.paths;
        // transform the results data
        const processedTimelineData = processTimelineData(timelineResults, testPaths); 
        const pathTableData = generatePathTableData(processedTimelineData, replayId);

        this.setState({
            replayId: replayId, 
            replayStatus: replayStatus,
            testConfig: testConfig,

            pathTableData: pathTableData,
            timelineResults: timelineResults,
            
            loading: false,
        });
    }
    
    renderDetails = () => {
        const {replayId, testConfig, replayStatus} = this.state;
        return <Fragment>
            <table className="table table-striped">
            <tbody>
                <tr><td>App</td><td>{replayStatus.app || "N/A"}</td></tr>
                <tr><td>Test Configuration</td><td>{testConfig.testConfigName || "N/A"}</td></tr>
                <tr><td>Mocked Services</td><td>{testConfig.testMockServices ? _.join(testConfig.testMockServices, ", ") : "N/A"}</td></tr>
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
        const {pathTableData} = this.state;
        const aggrData = pathTableData.path_results.reduce((acc, p) => {  
            let total = acc.total + p.total;
            let curr_resp_mm = acc.curr_resp_mm + p.curr_resp_mm;
            let curr_resp_mm_fraction = curr_resp_mm / total;
            let prev_results_count = p.prev_path_results_count;
            
            // checking if the number of results per path is consistent
            if (acc.prev_results_count !== 0 && acc.prev_results_count !== p.prev_path_results_count) {
                console.error("path results count isn't consistent")
            }
            
            return {
                total: total,
                curr_resp_mm: curr_resp_mm,
                curr_resp_mm_fraction: curr_resp_mm_fraction,
                prev_results_count: prev_results_count,
            }
        }, {
                total: 0,
                curr_resp_mm: 0,
                curr_resp_mm_fraction: 0,
                prev_results_count: 0,
        });
        
        return(
            <table className="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th colSpan={3}>Current Test</th>
                        <th colSpan={2}>Previous Tests ({aggrData.prev_results_count})</th>
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
                            {aggrData.total}
                        </td>
                        <td>
                            {aggrData.curr_resp_mm}
                        </td>
                        <td>
                            {(aggrData.curr_resp_mm_fraction * 100).toFixed(2) + "%"}
                        </td>
                        <td>
                            {pathTableData.prev_avg_resp_mm!==null ? (pathTableData.prev_avg_resp_mm * 100).toFixed(2) + "%" : "N/A"}
                        </td>
                        <td>
                            {pathTableData.prev_95_ci_resp_mm!==null ? (pathTableData.prev_95_ci_resp_mm * 100).toFixed(2) + "%" : "N/A"}
                        </td>
                    </tr>
                </tbody>
            </table>
        );
    }


    renderPathSummary = () => {
        const {pathTableData} = this.state;
        const result_count = pathTableData.path_results[0] ? pathTableData.path_results[0].prev_path_results_count : 0;
        return(
            <table className="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th></th>
                        <th colSpan={3}>Current Test</th>
                        <th colSpan={2}>Previous Tests ({result_count})</th>
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
                    {pathTableData.path_results.map((pathEntry, i) => { 
                            return <tr key={pathEntry.path}>
                                <td>
                                    {pathEntry.path}
                                </td>
                                <td>
                                    {pathEntry.total}
                                </td>
                                <td>
                                    {pathEntry.curr_resp_mm}
                                </td>
                                <td>
                                    {(pathEntry.curr_resp_mm_fraction * 100).toFixed(2) + "%"}
                                </td>
                                <td>
                                    {pathEntry.prev_avg_resp_mm!==null ? (pathEntry.prev_avg_resp_mm * 100).toFixed(2) + "%" : "N/A"}
                                </td>
                                <td>
                                    {pathEntry.prev_95_ci_resp_mm!==null ? (pathEntry.prev_95_ci_resp_mm * 100).toFixed(2) + "%" : "N/A"}
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
                    <Link to={"/"} onClick={this.handleBackToDashboardClick}><span className="link-alt"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
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
    cube: state.cube
})

const connectedTestReport = connect(mapStateToProps)(TestReport);
export default connectedTestReport;
export { connectedTestReport as TestReport };

