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
            replayList: null
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
    }

    getDiff (res) {
        console.log(res);
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getDiffData(cube.replayId.replayid, res.recordreqid, res.replayreqid));
    }

    render() {
        const {data} = this.props;
        const {replayList} = this.state;
        console.log(replayList);
        let disp = null;
        if (!replayList || replayList.length === 0) {
            disp = (<div className="padding-15">No Data</div>)
        } else {
            disp = replayList.map(item => (
                <Row key={item.replayreqid} style={{marginBottom: '10px'}}>
                    <Col md={4}><a className="links" onClick={() => this.getDiff(item)}>{item.replayreqid}</a></Col>
                    <Col md={4}>{JSON.stringify(item.qparams)}</Col>
                    <Col md={4}>{JSON.stringify(item.qparams)}</Col>
                </Row>
            ));
        }

        return (<div className="padding-15">{disp}</div>);
    }

    async fetchReplayList() {
        const {data} = this.props;
        const {cube} = this.props;
        console.log(cube);
        let response, json;
        let url = `${config.baseUrl}/as/analysisResByPath/${cube.replayId.replayid}?service=${data.service}&path=${data.path}%2A&start=20&nummatches=20&reqmt=FuzzyMatch&respmt=NoMatch`;
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
