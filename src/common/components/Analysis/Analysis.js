import React, { Component } from 'react';
import Diff from "../Diff";
import {connect} from "react-redux";
import config from "../../config";

class Analysis extends Component {
    constructor(props) {
        super(props);
        this.state = {
            replayList: [],
            selectedRowPath: null,
            isOpen: null
        };
        this.handleRowClick = this.handleRowClick.bind(this);
    }

    handleRowClick(tr) {
        this.setState({selectedRowPath: tr.path, isOpen: []});
        this.fetchReplayList(tr);
    }

    formatDataForTable(resByPath) {
        const formatted = [];
        for (const pathRes of resByPath) {
            if (pathRes.path) {
                formatted.push({
                    app: pathRes.app,
                    service: pathRes.service,
                    path: pathRes.path,
                    requests: pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched,
                    respMatched: pathRes.respmatched + pathRes.resppartiallymatched,
                    respNotMatched: pathRes.respnotmatched,
                    incomplete: (pathRes.reqmatched + pathRes.reqpartiallymatched + pathRes.reqnotmatched) - (pathRes.respmatched + pathRes.resppartiallymatched + pathRes.respnotmatched),
                    reviewed: '-',
                    actions: '-'
                });
            }
        }

        return formatted;
    }

    getDiff(itemDiff) {
        let st = JSON.parse(itemDiff);
        for (const s of st) {
            s.path = s.path.split('/body')[1];
        }
        return st;
    }

    open(id) {
        let {isOpen} = this.state;
        if (id == isOpen) {
            this.setState({isOpen: null});
        } else {
            this.setState({isOpen: id});
        }
    }

    render() {
        const {res, resByPath, cube} =  this.props;
        const {replayList, isOpen, selectedRowPath} = this.state;
        const allReplay = replayList;

        let accordion = allReplay.map((item, index) => {
            return (
                <div className="acc-wrapper" key={item.replayreqid + index}>
                    <div className="acc-title cursor-pointer" onClick={() => this.open(item.replayreqid + index)}>{item.recordreqid}</div>
                    <div className="acc-body" style={isOpen == (item.replayreqid + index) ? {display: "block"} : {display: "none"}}>
                        {(item.recordResponse && item.replayResponse && item.diff) ?
                            <Diff recorded={JSON.parse(item.recordResponse.body)} replayRes={JSON.parse(item.replayResponse.body)} diff={this.getDiff(item.diff)}/>
                            : 'Insufficient Data'
                        }
                    </div>
                </div>
            );
        });

        //this.formatData();
        const tableData = this.formatDataForTable(resByPath);
        let rows = tableData.map(item => (
            <tr key={item.path} className={item.path == selectedRowPath ? 'sel-row' : ''}
                onClick={() => this.handleRowClick(item)}><td>{item.path}</td><td>{item.respMatched}</td>
            </tr>))
        const columns = [
            {
                name: 'Path',
                selector: 'path',
                sortable: true,
            },
            /*{
                name: 'Requests',
                selector: 'requests',
                sortable: true,
            },*/
            {
                name: 'Responses Matched',
                selector: 'respMatched',
                sortable: true,
            },
            /*{
                name: 'Responses Not Matched',
                selector: 'respNotMatched',
                sortable: true,
            },*/
            /*{
                name: 'Did Not Complete',
                selector: 'incomplete',
                sortable: true,
            },
            {
                name: 'Reviewed',
                selector: 'reviewed',
                sortable: false,
            },
            {
                name: 'Actions',
                selector: 'actions',
                sortable: false,
            }*/
        ];

        return (<div style={{paddingTop: '15px'}}>

            <div className="row">
                <div className="col-md-2">
                    <table id="path-table" className="table table-hover table-striped">
                        <thead>
                        <tr>
                            <th className="header-row">Path</th>
                            <th className="header-row">Responses Matched</th>
                        </tr>
                        </thead>
                        <tbody>
                        {rows}
                        </tbody>
                    </table>
                </div>

                <div className="col-md-10">
                    {!selectedRowPath ? 'Please Select A Path' : accordion}
                </div>
            </div>

        </div>)
    }

    async fetchReplayList(tr) {
        const data = tr;
        const {cube} = this.props;
        console.log(cube);
        let response, json;
        let url = `${config.analyzeBaseUrl}/analysisResByPath/${cube.replayId.replayid}?service=${tr.service}&path=${tr.path}%2A&start=0&includediff=true`;
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



const connectedAnalysis = connect(mapStateToProps)(Analysis);
export default connectedAnalysis
