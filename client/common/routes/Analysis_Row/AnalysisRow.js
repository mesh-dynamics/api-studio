import React, { Component } from 'react';
import { connect } from 'react-redux';
import config from "../../config";
import Row from "react-bootstrap/es/Row";
import {Col} from "react-bootstrap";
import {cubeActions} from "../../actions";

class AnalysisRow extends Component {
    constructor(props) {
        super(props);
        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.getDiff = this.getDiff.bind(this);
        this.state = {
            replayList: null,
            replayListEx: null
        }

    }



    handleClose() {
        this.setState({ show: false, showDiff: false });
    }

    handleShow() {
        this.setState({ show: true });
    }

    componentDidMount() {
        this.fetchReplayList();
        this.fetchReplayListEx();
    }

    getDiff (res) {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getDiffData(cube.replayId.replayid, res.recordreqid, res.replayreqid));
    }

    render() {
        const {data} = this.props;
        const {replayList, replayListEx} = this.state;
        console.log(replayList);
        let disp = null;
        let disp1 = null;
        if ((!replayList || replayList.length === 0) && (!replayListEx || replayListEx.length === 0)) {
            disp = (<div className="padding-15">No Data</div>)
        } else {
            disp = replayList.map(item => (
                <Row key={item.replayreqid} style={{marginBottom: '15px'}}>
                    <Col md={6}><a className="links" onClick={() => this.getDiff(item)}>
                        {(item.qparams && Object.keys(item.qparams).length > 0) ? JSON.stringify(item.qparams) : JSON.stringify(item.qparams)}</a>
                    </Col>
                    <Col md={6}>{item.method}</Col>
                </Row>
            ));

            disp1 = replayListEx.map(item => (
                <Row key={item.replayreqid} style={{marginBottom: '15px'}}>
                    <Col md={6}><a className="links" onClick={() => this.getDiff(item)}>
                        {(item.qparams && Object.keys(item.qparams).length > 0) ? JSON.stringify(item.qparams) : JSON.stringify(item.qparams)}</a>
                    </Col>
                    <Col md={6}>{item.method}</Col>
                </Row>
            ));
        }

        return (<div className="padding-15">{disp}{disp1}</div>);
    }

    async fetchReplayList() {
        const {data} = this.props;
        const {cube} = this.props;
        console.log(cube);
        let response, json;
        let url = `${config.baseUrl}/as/analysisResByPath/${cube.replayId.replayid}?service=${data.service}&path=${data.path}%2A&start=0&nummatches=20&respmt=NoMatch`;
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
            console.log("fetchTimeline has errors!", e);
            throw e;
        }
        this.setState({replayList: dataList.res});
    }

    async fetchReplayListEx() {
        const {data} = this.props;
        const {cube} = this.props;
        console.log(cube);
        let response, json;
        let url = `${config.baseUrl}/as/analysisResByPath/${cube.replayId.replayid}?service=${data.service}&path=${data.path}%2A&start=0&nummatches=20&respmt=Exception`;
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
            console.log("fetchTimeline has errors!", e);
            throw e;
        }
        this.setState({replayListEx: dataList.res});
    }





}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}



const connectedAnalysisRow = connect(mapStateToProps)(AnalysisRow);
export default connectedAnalysisRow
