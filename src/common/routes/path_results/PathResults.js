import React, { Component } from 'react';
import { ButtonToolbar, Button, Glyphicon } from 'react-bootstrap';
import _ from 'lodash';
import { Link } from "react-router-dom";
import ReactTable from 'react-table';
import 'react-table/react-table.css';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';
import config from "../../config";
import { connect } from "react-redux";
import DiffResults from "../diff_results/DiffResults";
import { Redirect } from "react-router";
import Breadcrumb from "../../components/breadcrumb/Breadcrumb";
import { cubeActions } from "../../actions";
import Modal from "react-bootstrap/es/Modal";

class PathResults extends Component {
    constructor(props) {
        super(props);
        let { cube } = this.props;
        this.state = {
            replayList: [],
            showDiff: false,
            app: cube.selectedApp,
            showNewGolden: false
        };
        this.showHide = this.showHide.bind(this);
        this.updateGolden = this.updateGolden.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    componentWillReceiveProps(nextProps, prevState) {
        let { cube, dispatch } = nextProps;
        if (cube.goldenInProg || cube.newGoldenId) {
            this.setState({ showNewGolden: true });
        }
    }

    componentDidMount() {
        let { cube, dispatch, history } = this.props;
        if (!cube.pathResultsParams) {
            history.push("/");
        } else {
            dispatch(cubeActions.getCollectionUpdateOperationSet(cube.selectedApp));
            dispatch(cubeActions.getNewTemplateVerInfo(cube.selectedApp, cube.pathResultsParams.currentTemplateVer));
            this.fetchReplayList();
        }
    }

    componentWillUnmount() {
        this.setState({ showNewGolden: false });
    }

    showHide() {
        const { showDiff } = this.state;
        this.setState({ showDiff: !showDiff });
    }

    handleClose() {
        const { history } = this.props;
        this.setState({ showNewGolden: false });
        setTimeout(() => {
            history.push("/test_config");
        })
    }

    updateGolden() {
        const { cube, dispatch } = this.props;
        let user = JSON.parse(localStorage.getItem('user'));
        let gObj = {
            "operationSetId": cube.collectionUpdateOperationSetId.operationSetId,
            "service": cube.pathResultsParams.service,
            "path": cube.pathResultsParams.path,
            "operationSet": cube.newOperationSet,
            "customer": user.customer_name,
            "app": cube.selectedApp
        };

        let keyObjForRule = {
            customerId: user.customer_name,
            appId: cube.selectedApp,
            serviceId: cube.pathResultsParams.service,
            path: cube.pathResultsParams.path,
            version: cube.pathResultsParams.currentTemplateVer,
            reqOrResp: "Response"
        };

        const rObj = {};
        const rObjKey = JSON.stringify(keyObjForRule);
        for (const op of cube.operations) {

        }
        rObj[rObjKey] = { operations: cube.operations };

        dispatch(cubeActions.updateRecordingOperationSet(gObj, cube.pathResultsParams.replayId,
            cube.collectionUpdateOperationSetId.operationSetId, cube.newTemplateVerInfo['ID'],
            cube.pathResultsParams.recordingId, cube.selectedApp));
        dispatch(cubeActions.updateTemplateOperationSet(cube.newTemplateVerInfo['ID'], rObj));
    }

    render() {
        const { replayList, showDiff } = this.state;
        const { cube } = this.props;
        let completeReplayList = replayList,
            columns = [];
        const Cell = props => {
            return (
                < div {...props}>
                    <div style={{ margin: "1px", width: "90%" }}>
                        <div className="pull-left">{props.celldata}</div>
                        <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true} maxWidth={800}
                            content={
                                <div style={{overflowY: "auto", fontSize: "14px" }} className="grey" id={`tooltip-${props.id}`}>
                                    <div style={{color: "#333333", padding: "15px", textAlign: "left" }}>
                                        <div className="row margin-bottom-10">
                                            <div className="col-md-2">Method:</div>
                                            <div className="col-md-10 bold">{props.method}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-2">URL:</div>
                                            <div className="col-md-10 bold">{props.path}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-2">Parameters:</div>
                                            <div className="col-md-10">
                                                <table className="table table-bordered" style={{ width: "100%", tableLayout: "fixed" }}>
                                                    <thead>
                                                        <tr>
                                                            <th style={{ background: "#efefef", width: "30%" }}>Key</th>
                                                            <th style={{ background: "#efefef", width: "70%"  }}>Value</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {
                                                            props.qparams && !_.isEmpty(props.qparams) ?
                                                                Object.entries(props.qparams).map(([key, value]) => (<tr>
                                                                    <td style={{ background: "#ffffff", width: "30%", wordBreak: "break-word", whiteSpace: "normal", textAlign: "left" }}>{key}</td>
                                                                    <td style={{ background: "#ffffff", width: "70%", wordBreak: "break-word", whiteSpace: "normal", textAlign: "left" }}>{value}</td>
                                                                </tr>))
                                                                : <tr></tr>
                                                        }
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-2">Body:</div>
                                            <div className="col-md-10">
                                                <pre style={{ backgroundColor: "#D5D5D5" }}>{JSON.stringify(props.fparams, undefined, 4)}</pre>
                                            </div>
                                        </div>
                                    </div>
                                </div>}>
                            <div className="pull-right"><Glyphicon glyph="option-horizontal" /> </div>
                        </Tippy>
                    </div>
                </div>
            )
        }

        columns.push({
            Header: () => <strong>REQUESTS</strong>,
            accessor: "path",
            Cell: row => {
                return (
                    <div>
                        <div>{row.value}</div>
                        <div>
                            <Cell {...row.original} />
                        </div>
                    </div>);
            }
        });

        columns.push({
            Header: () => <strong>STATUS</strong>,
            accessor: "respmt",
            Cell: row => {
                return row.value
            }
        });

        const crumbs = [
            { label: "APPLICATION", value: cube.selectedApp },
            { label: "SERVICE", value: cube.pathResultsParams ? cube.pathResultsParams.service : "" },
            { label: "API", value: cube.pathResultsParams ? cube.pathResultsParams.path : "" },
        ];

        return (
            <React.Fragment>
                {
                    !cube.timelineData || cube.selectedApp != this.state.app ? <Redirect to="/" /> :
                        (<div className="content-wrapper">
                            <Breadcrumb crumbs={crumbs} />
                            <div className={showDiff ? "hidden" : ""}>
                                <ReactTable
                                    data={completeReplayList}
                                    columns={columns}
                                    style={{
                                        height: "600px" // This will force the table body to overflow and scroll, since there is not enough room
                                    }}
                                    showPagination={true}
                                    defaultPageSize={10}
                                />
                                <div style={{ marginTop: "36px" }}>
                                    <ButtonToolbar>
                                        <Link to="/">
                                            <Button style={{ width: "225px" }}><Glyphicon glyph="chevron-left" /> BACK</Button>
                                        </Link>
                                        <Button onClick={this.showHide} style={{ width: "225px" }}>VIEW RESPONSES</Button>
                                    </ButtonToolbar>
                                </div>

                            </div >

                            <div className={showDiff ? "" : "hidden"}>
                                <DiffResults updateGolden={this.updateGolden} completeReplayList={completeReplayList} showHide={this.showHide} />
                            </div>

                            <Modal show={this.state.showNewGolden}>
                                <Modal.Header>
                                    <Modal.Title>Golden Update</Modal.Title>
                                </Modal.Header>
                                <Modal.Body>
                                    <p className={cube.newGoldenId ? "" : "hidden"}>Golden ID: {cube.newGoldenId}</p>
                                    <p className={cube.newGoldenId ? "hidden" : ""}>Updating Operations...</p>
                                </Modal.Body>
                                <Modal.Footer className={cube.newGoldenId ? "" : "hidden"}>
                                    <div>
                                        <span onClick={this.handleClose} className="cube-btn">Go TO Test Config</span>
                                    </div>
                                </Modal.Footer>
                            </Modal>
                        </div>)
                }
            </React.Fragment>
        );
    }

    async fetchReplayList() {
        const { cube, history } = this.props;
        let pathResultsParams = cube.pathResultsParams;
        if (!pathResultsParams) {
            history.push("/")
            return
        }
        let response, json;
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${pathResultsParams.replayId}?service=${pathResultsParams.service}&path=${pathResultsParams.path}%2A&start=0&includediff=true`;
        let dataList = {};
        try {
            response = await fetch(url, {
                method: "get",
                headers: new Headers({
                    "cache-control": "no-cache"
                })
            });
            if (response.ok) {
                json = await response.json();
                dataList = json;
            } else {
                console.log("Response not ok in fetchTimeline", response);
                throw new Error("Response not ok fetchTimeline");
            }
        } catch (e) {
            console.error("fetchTimeline has errors!", e);
            throw e;
        }
        this.setState({ replayList: dataList.res });
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedPathResults = connect(mapStateToProps)(PathResults);

export default connectedPathResults;
