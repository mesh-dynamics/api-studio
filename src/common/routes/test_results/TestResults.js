import React, { Component } from 'react';
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';
import { connect } from "react-redux";
import * as moment from 'moment';
import { cubeActions } from "../../actions";
import withFixedColumns from 'react-table-hoc-fixed-columns';
import 'react-table-hoc-fixed-columns/lib/styles.css';
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

const ReactTableFixedColumns = withFixedColumns(ReactTable);

class TestResults extends Component {

    constructor(props) {
        super(props);
        this.state = {
            endDate: new Date(),
            userFilter: "ALL"
        };
        this.setPathResultsParams = this.setPathResultsParams.bind(this);
    }

    clearFilter = () => {
        const {dispatch, cube} = this.props;
        dispatch(cubeActions.getTimelineData(cube.selectedApp));
        this.setState({
            endDate: new Date(),
            userFilter: "ALL"
        });
    };

    changeUserFilter = event => {
        const {dispatch, cube} = this.props;
        dispatch(cubeActions.getTimelineData(cube.selectedApp, event.target.value, this.state.endDate));
        this.setState({
            userFilter: event.target.value
        });
    };

    handleDateFilter = date => {
        const {dispatch, cube} = this.props;
        dispatch(cubeActions.getTimelineData(cube.selectedApp, this.state.userFilter, date));
        this.setState({
            endDate: date
        });
    };

    setPathResultsParams(path, service, replayId, recordingId, currentTemplateVer, dateTime, cellData) {
        if (!cellData) return;
        const { dispatch, history } = this.props;
        dispatch(cubeActions.setPathResultsParams({
            path: path,
            service: service,
            replayId: replayId,
            recordingId: recordingId,
            timeStamp: dateTime,
            currentTemplateVer: currentTemplateVer
        }));
        setTimeout(() => {
            history.push("/path_results");
        });
    }

    render() {
        const { cube } = this.props;
        if (cube.timelineData == null) {
            return (
                <div style={{ margin: "27px" }}>
                    <h5>Test Results</h5>
                </div >);
        }

        let timelineData = cube.timelineData.timelineResults,
            columns = [], uniquePaths = [], tableData = [], tableDataPathMap = {},
            allRunsTimestamps = [];
        let tempTableData = {};
        for (let testResult of timelineData) {
            let testResultsPerReplay = testResult.results;
            let momentDateObject = testResult.timestamp ? moment.utc(testResult.timestamp) : "",
                localMomentDateObejct = moment(momentDateObject).local(),
                dateString = "";
            if (momentDateObject) dateString = localMomentDateObejct.format('l');
            let collection = testResult.collection,
                dateTime = localMomentDateObejct.format('lll'),
                recordingId = testResult.recordingid,
                templateVer = testResult.templateVer;
            if (allRunsTimestamps.indexOf(momentDateObject.valueOf()) < 0) allRunsTimestamps.push(momentDateObject.valueOf());
            for (let eachReplayResult of testResultsPerReplay) {
                if (eachReplayResult.service != null && eachReplayResult.path != null) {
                    if (uniquePaths.indexOf(eachReplayResult.path) < 0) uniquePaths.push(eachReplayResult.path);
                }
                let rPath = eachReplayResult.path ? eachReplayResult.path : "--",
                    rService = eachReplayResult.service ? eachReplayResult.service : "--",
                    rApp = eachReplayResult.app, rTableRowKey = "";
                rTableRowKey = rService + "/" + rPath;
                if (!tableDataPathMap[rTableRowKey]) tableDataPathMap[rTableRowKey] = [];
                tableDataPathMap[rTableRowKey].push({
                    ...eachReplayResult,
                    timestamp: momentDateObject.valueOf(),
                    collection,
                    dateTime,
                    recordingId,
                    templateVer
                });
                if (rTableRowKey.indexOf("--") > 0) {
                    let tempGroupKey = rTableRowKey.replace("--", "");
                    if (!tempTableData[tempGroupKey]) tempTableData[tempGroupKey] = null;
                }
            }
        }
        for (let eachPath in tableDataPathMap) {
            let tempData = {};
            for (let eachRun of tableDataPathMap[eachPath]) {
                tempData[eachRun.timestamp] = {
                    ...eachRun
                }
            }
            if (eachPath.indexOf("--") == 0) {
                tableData.push({
                    serviceRowKey: eachPath,
                    ...tempData
                });
                continue;
            }
            if (eachPath.indexOf("--") > 0) {
                if (tempTableData[eachPath.replace("--", "")] == null) {
                    tempTableData[eachPath.replace("--", "")] = {
                        serviceRowKey: eachPath,
                        ...tempData,
                        subRows: []
                    };
                }
            }
        }
        for (let eachPath in tableDataPathMap) {
            if (eachPath.indexOf("--") > -1) {
                //continue;
            }
            let tempData = {};
            for (let eachRun of tableDataPathMap[eachPath]) {
                tempData[eachRun.timestamp] = {
                    ...eachRun
                }
            }
            if (eachPath.indexOf("--") < 0) {
                for (let eachGroupKey in tempTableData) {
                    if (eachPath.startsWith(eachGroupKey)) {
                        tempTableData[eachGroupKey].subRows.push({
                            serviceRowKey: eachPath.replace(eachGroupKey, "/"),
                            ...tempData
                        });
                        break;
                    }
                }
            }
        }
        for (let eachGroupKey in tempTableData) {
            tableData.push(tempTableData[eachGroupKey]);
        }
        /*columns.push({
            Header: "Expand",
            fixed: "left",
            columns: [{
                expander: true,
                Header: () => <strong></strong>,
                width: 65,
                sortable: false,
                Expander: ({ isExpanded, ...rest }) => {
                    if (rest.row && rest.row.serviceRowKey.indexOf("--") > 0) {
                        return (<div>
                            {isExpanded
                                ? <span style={{ position: "relative", bottom: "5px" }}><i className="fas fa-minus-circle" style={{ "fontSize": "12px" }}></i></span>
                                : <span style={{ position: "relative", bottom: "5px" }}><i className="fas fa-plus-circle" style={{ "fontSize": "12px" }}></i></span>}
                        </div>);
                    }
                    rest.expander = false;
                    return "";
                },
                headerClassName: "freeze-column",
                className: "freeze-column",
                style: {
                    cursor: "pointer",
                    fontSize: 25,
                    padding: "0",
                    textAlign: "center",
                    userSelect: "none"
                }
            }]
        });*/
        columns.push({
            Header: "Service",
            fixed: "left",
            columns: [{
                expander: true,
                Header: () => <strong></strong>,
                width: 65,
                sortable: false,
                Expander: ({ isExpanded, ...rest }) => {
                    if (rest.row && rest.row.serviceRowKey.indexOf("--") > 0) {
                        return (<div>
                            {isExpanded
                                ? <span style={{ position: "relative", bottom: "5px" }}><i className="fas fa-minus-circle" style={{ "fontSize": "12px" }}></i></span>
                                : <span style={{ position: "relative", bottom: "5px" }}><i className="fas fa-plus-circle" style={{ "fontSize": "12px" }}></i></span>}
                        </div>);
                    }
                    rest.expander = false;
                    return "";
                },
                headerClassName: "freeze-column",
                className: "freeze-column",
                style: {
                    cursor: "pointer",
                    fontSize: 25,
                    padding: "0",
                    textAlign: "center",
                    userSelect: "none"
                }
            }, {
                Header: () => <strong>Test Summary</strong>,
                id: "serviceRowKey",
                headerClassName: "freeze-column",
                className: "freeze-column",
                accessor: "serviceRowKey",
                sortable: false,
                filterable: true,
                width: 300,
                Cell: row => {
                    if (row.value && row.value.indexOf("--") == 0) {
                        return (<strong></strong>);
                    }
                    if (row.value && row.value.indexOf("--") > 0) {
                        return (
                            <div>{row.value}</div>
                        )
                    }
                    return (
                        <div style={{ paddingLeft: '18px'}}>{row.value}</div>
                    )
                }
            }]
        });
        let count = 0;
        for (let eachTimeStamp of allRunsTimestamps) {
            columns.push({
                Header: "" + moment(eachTimeStamp).format('lll'),
                accessor: "" + eachTimeStamp,
                sortable: false,
                minWidth: count == 0 ? 240 : 150,
                columns: [{
                    Header: (args) => {
                        if (!args.column && !args.column.id) return "";
                        if (!args.data && !args.data.length == 0) return "";
                        let cellId = args.column.id;
                        let data = args.data;
                        if (!data) return "";
                        for (let eachRow of data) {
                            let cellData = eachRow[cellId];
                            if (!cellData) return (<strong>NA</strong>);
                            return (<strong
                                style={{
                                    color: cellData.respnotmatched && cellData.respnotmatched > 0 ? '#ff2e00' : '#85cc00',
                                }}
                            >
                                {cellData.respnotmatched && cellData.respnotmatched > 0 ? 'Fail' : 'Pass'}
                            </strong>)
                        }
                        return (<strong></strong>);
                    },
                    accessor: "" + eachTimeStamp,
                    minWidth: count == 0 ? 240 : 150,
                    sortable: false,
                    Cell: row => {
                        if (!row.value) {
                            return (<div
                                style={{
                                    width: '100%',
                                    height: '50%',
                                    borderRadius: '2px',
                                    textAlign: "center"
                                }}
                            >
                                <div>NA</div>
                            </div>)
                        }
                        if (row.row.serviceRowKey && row.row.serviceRowKey.indexOf("--") == 0) {
                            return (<strong></strong>);
                        }
                        if (row.row.serviceRowKey && row.row.serviceRowKey.indexOf("--") > 0) {
                            return (
                                <div
                                    style={{
                                        width: '100%',
                                        height: '50%',
                                        backgroundColor: '#dadada',
                                        borderRadius: '2px'
                                    }}
                                >
                                    <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"mouseenter"} appendTo={"parent"} flipOnUpdate={true} maxWidth={450}
                                        content={
                                            <div style={{ overflowY: "auto", fontSize: "14px" }} className="grey">
                                                <div style={{ color: "#333333", padding: "15px", textAlign: "left" }}>
                                                    <div style={{ paddingBottom: '5px', borderBottom: '1px solid #333', marginBottom: '9px' }}>
                                                        {row.value.app + " > " + row.value.service}
                                                    </div>
                                                    <div>
                                                        <div className="row margin-bottom-10">
                                                            <div className="col-md-6">Errors:</div>
                                                            <div className="col-md-4 bold">{row.value.respnotmatched}</div>
                                                            <div className="col-md-6">Low severity errors:</div>
                                                            <div className="col-md-4 bold">{row.value.resppartiallymatched}</div>
                                                            <div className="col-md-6">Incomplete requests:</div>
                                                            <div className="col-md-4 bold">{row.value.reqnotmatched}</div>
                                                            <div className="col-md-6">Mock errors:</div>
                                                            <div className="col-md-4 bold">{row.value.mockReqNotMatched}</div>
                                                            <div className="col-md-6">New errors:</div>
                                                            <div className="col-md-4 bold">{row.value.respnotmatched}</div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        }>
                                        <div
                                            style={{
                                                width: `${100}%`,
                                                height: '100%',
                                                backgroundColor: row.value.respnotmatched && row.value.respnotmatched > 0 ? '#ff2e00' : '#85cc00',
                                                borderRadius: '2px',
                                                transition: 'all .2s ease-out'
                                            }}
                                        />
                                    </Tippy>
                                </div >
                            )
                        }
                        return (
                            <div
                                style={{
                                    width: '100%',
                                    height: '50%',
                                    backgroundColor: '#dadada',
                                    borderRadius: '2px',
                                    cursor: "pointer"
                                }}
                                onClick={
                                    () => {
                                        return this.setPathResultsParams(row.value.path, row.value.service, row.value.replayid, row.value.recordingId, row.value.templateVer, row.value.dateTime, 1)
                                    }
                                }
                            >
                                <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"mouseenter"} appendTo={"parent"} flipOnUpdate={true} maxWidth={450}
                                    content={
                                        <div style={{ overflowY: "auto", fontSize: "14px" }} className="grey">
                                            <div style={{ color: "#333333", padding: "15px", textAlign: "left" }}>
                                                <div style={{ paddingBottom: '5px', borderBottom: '1px solid #333', marginBottom: '9px' }}>
                                                    {row.value.app + " > " + row.value.service + " > " + row.value.path}
                                                </div>
                                                <div>
                                                    <div className="row margin-bottom-10">
                                                        <div className="col-md-6">Errors:</div>
                                                        <div className="col-md-4 bold">{row.value.respnotmatched}</div>
                                                        <div className="col-md-6">Low severity errors:</div>
                                                        <div className="col-md-4 bold">{row.value.resppartiallymatched}</div>
                                                        <div className="col-md-6">Incomplete requests:</div>
                                                        <div className="col-md-4 bold">{row.value.reqnotmatched}</div>
                                                        <div className="col-md-6">Mock errors:</div>
                                                        <div className="col-md-4 bold">{row.value.mockReqNotMatched}</div>
                                                        <div className="col-md-6">New errors:</div>
                                                        <div className="col-md-4 bold">{row.value.respnotmatched}</div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    }>
                                    <div
                                        style={{
                                            width: `${100}%`,
                                            height: '100%',
                                            backgroundColor: row.value.respnotmatched && row.value.respnotmatched > 0 ? '#ff2e00' : '#85cc00',
                                            borderRadius: '2px',
                                            transition: 'all .2s ease-out'
                                        }}
                                    />
                                </Tippy>
                            </div>
                        )
                    }
                }]
            });
            count++;
        }
        return (
            <div className="content-wrapper">
                <div className="heading">
                    <h5 className="pull-left">Test Results</h5>
                    <div className="fiters pull-right" style={{width: "300px"}}>
                        <div className="inline-block margin-bottom-10" style={{paddingRight: "10px", borderRight: "1px solid #ddd", marginRight:"10px"}}>
                            <div className="margin-bottom-10">
                                <span>USER: </span>
                                <select onChange={(event) => this.changeUserFilter(event)} value={this.state.userFilter} style={{width: "140px"}}>
                                    <option value="ALL">ALL</option>
                                    <option value="ME">ME</option>
                                </select>
                            </div>

                            <div className="">
                                <span>DATE: </span>
                                <DatePicker
                                    style={{width: "140px"}}
                                    selected={this.state.endDate}
                                    maxDate={new Date()}
                                    onChange={this.handleDateFilter} />
                            </div>
                        </div>

                        <div className="inline-block" style={{verticalAlign: "top"}}>
                            <i className="fas fa-filter"></i>&nbsp;
                            <span className="link" onClick={this.clearFilter}>Clear</span>
                        </div>

                    </div>
                    <div className="clear"></div>
                </div>

                <ReactTableFixedColumns
                    data={tableData}
                    columns={columns}
                    subRowsKey="subRows"
                    style={{
                        height: "600px" // This will force the table body to overflow and scroll, since there is not enough room
                    }}
                    showPagination={true}
                    defaultPageSize={10}
                />
            </div >
        );
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedTestResults = connect(mapStateToProps)(TestResults);

export default connectedTestResults;
