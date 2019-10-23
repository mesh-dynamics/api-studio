import React, { Component } from 'react';
import { Checkbox, FormGroup, Glyphicon, FormControl } from 'react-bootstrap';
import _ from 'lodash';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';

import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import config from "../../config";

const cleanEscapedString = (str) => {
    // preserve newlines, etc - use valid JSON
    str = str.replace(/\\n/g, "\\n")
        .replace(/\\'/g, "\\'")
        .replace(/\\"/g, '\\"')
        .replace(/\\&/g, "\\&")
        .replace(/\\r/g, "\\r")
        .replace(/\\t/g, "\\t")
        .replace(/\\b/g, "\\b")
        .replace(/\\f/g, "\\f");
    // remove non-printable and other non-valid JSON chars
    str = str.replace(/[\u0000-\u0019]+/g, "");
    return str;
}

const uniqueFilterFunction = (value, index, self) => {
    return self.indexOf(value) === index;
}

class ShareableDiff extends Component {
    constructor(props) {
        super(props);
        this.state = {
            replayList: [],
            diffLayoutData: [],
            showResponseMessageHeader: false,
            showResponseMessageBody: true,
            filterPath: ''
        }

        this.handleChange = this.handleChange.bind(this);
        this.toggleMessageContents = this.toggleMessageContents.bind(this);

        this.inputElementRef = React.createRef();
    }

    componentDidMount() {
        this.fetchReplayList();
    }

    componentDidUpdate(prevProps, prevState) {
    }

    componentWillUnmount() {
    }

    handleChange(e) {
        this.setState({ filterPath: e.target.value });
    }

    toggleMessageContents(e) {
        if (e.target.value === "responseHeaders") this.setState({ showResponseMessageHeader: e.target.checked });
        if (e.target.value === "responseBody") this.setState({ showResponseMessageBody: e.target.checked });
    }

    async fetchReplayList() {
        let urlParameters = _.chain(window.location.search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();
        console.log("urlParameters: ", urlParameters);
        let response, json, { resultsFetched } = this.state;
        let user = JSON.parse(localStorage.getItem('user'));
        let url = `${config.analyzeBaseUrl}/analysisResNoTrace/${urlParameters["replayId"]}/${urlParameters["recordRequestId"]}`;
        let dataList = {};
        try {
            response = await fetch(url, {
                method: "get",
                headers: new Headers({
                    "cache-control": "no-cache",
                    "Authorization": "Bearer " + user['access_token']
                })
            });
            if (response.ok) {
                json = await response.json();
                dataList = json;
                let diffLayoutData = this.validateAndCreateDiffLayoutData([dataList]);
                this.setState({
                    replayList: this.state.replayList.concat([dataList]),
                    diffLayoutData: this.state.diffLayoutData.concat(diffLayoutData)
                });
            } else {
                console.log("Response not ok in fetchTimeline", response);
                throw new Error("Response not ok fetchTimeline");
            }
        } catch (e) {
            console.error("fetchTimeline has errors!", e);
            throw e;
        }
    }

    validateAndCreateDiffLayoutData(replayList) {
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders;
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                if (item.recordResponse.body) {
                    try {
                        if (item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
                            recordedData = JSON.parse(item.recordResponse.body);
                        }
                        else recordedData = item.recordResponse.body;
                    } catch (e) {
                        recordedData = JSON.parse('"' + cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
                    }
                }
                else {
                    recordedData = JSON.parse('""');
                }
            } else {
                recordedResponseHeaders = null;
                recordedData = null;
            }
            if (item.replayResponse) {
                replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
                if (item.replayResponse.body) {
                    try {
                        if (item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
                            replayedData = JSON.parse(item.replayResponse.body);
                        }
                        else replayedData = item.replayResponse.body;
                    } catch (e) {
                        replayedData = JSON.parse('"' + cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
                    }
                }
                else {
                    replayedData = JSON.parse('""');
                }
            } else {
                replayedResponseHeaders = null;
                replayedData = null;
            }
            let diff;
            if (item.diff) {
                try {
                    diff = JSON.parse(item.diff);
                } catch (e) {
                    diff = JSON.parse('"' + item.diff + '"')
                }
            }
            else diff = [];
            let actJSON = JSON.stringify(replayedData, undefined, 4),
                expJSON = JSON.stringify(recordedData, undefined, 4);
            let reductedDiffArray = null;
            if (diff && diff.length > 0) {
                let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            } else if (diff && diff.length == 0) {
                if (_.isEqual(expJSON, actJSON)) {
                    let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                }
            }
            return {
                ...item,
                recordedResponseHeaders,
                replayedResponseHeaders,
                recordedData,
                replayedData,
                actJSON,
                expJSON,
                parsedDiff: diff,
                reductedDiffArray,
                show: true
            }
        });
        console.log("diffLayoutData: ", diffLayoutData)
        return diffLayoutData;
    }

    render() {
        let { diffLayoutData } = this.state;
        const newStyles = {
            variables: {
                addedBackground: '#e6ffed !important',
                addedColor: '#24292e  !important',
                removedBackground: '#ffeef0  !important',
                removedColor: '#24292e  !important',
                wordAddedBackground: '#acf2bd  !important',
                wordRemovedBackground: '#fdb8c0  !important',
                addedGutterBackground: '#cdffd8  !important',
                removedGutterBackground: '#ffdce0  !important',
                gutterBackground: '#f7f7f7  !important',
                gutterBackgroundDark: '#f3f1f1  !important',
                highlightBackground: '#fffbdd  !important',
                highlightGutterBackground: '#fff5b1  !important',
            },
            line: {
                padding: '10px 2px',
                '&:hover': {
                    background: '#f7f7f7',
                },
            }
        };
        let jsxContent = diffLayoutData.map((item, index) => {
            return (<div key={item.path + '-' + index} style={{ borderBottom: "1px solid #eee", display: item.show ? "block" : "none" }}>
                <div style={{ backgroundColor: "#EAEAEA", paddingTop: "18px", paddingBottom: "18px", paddingLeft: "10px" }}>
                    <div className="inline-block margin-right-10">
                        <Tippy arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true} maxWidth={500}
                            content={
                                <div style={{ overflowY: "auto", fontSize: "14px" }} className="grey" id={`tooltip-${index}`}>
                                    <div style={{ padding: "10px", color: "#333333", textAlign: "left" }}>
                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Method:</div>
                                            <div className="col-md-9 bold">{item.method}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">URL:</div>
                                            <div className="col-md-9 bold">{item.path}</div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Parameters:</div>
                                            <div className="col-md-9">
                                                <table className="table table-bordered" style={{ width: "100%", tableLayout: "fixed" }}>
                                                    <thead>
                                                        <tr>
                                                            <th style={{ background: "#efefef", width: "30%" }}>Key</th>
                                                            <th style={{ background: "#efefef", width: "70%" }}>Value</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        {
                                                            item.qparams && !_.isEmpty(item.qparams) ?
                                                                Object.entries(item.qparams).map(([key, value]) => (<tr key={key}>
                                                                    <td style={{ background: "#ffffff", wordBreak: "break-word", whiteSpace: "normal", width: "30%" }}>{key}</td>
                                                                    <td style={{ background: "#ffffff", wordBreak: "break-word", whiteSpace: "normal", width: "70%" }}>{value}</td>
                                                                </tr>))
                                                                : <tr></tr>
                                                        }
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>

                                        <div className="row margin-bottom-10">
                                            <div className="col-md-3">Body:</div>
                                            <div className="col-md-9">
                                                <pre style={{ backgroundColor: "#D5D5D5" }}>{JSON.stringify(item.fparams, undefined, 4)}</pre>
                                            </div>
                                        </div>
                                    </div>
                                </div>}>
                            <div><Glyphicon glyph="option-horizontal" /> </div>
                        </Tippy>
                    </div>
                    {item.path}
                </div>
                {item.recordedResponseHeaders != null && item.replayedResponseHeaders != null && (
                    <div style={{ display: this.state.showResponseMessageHeader ? "" : "none" }}>
                        <div className="headers-diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={JSON.stringify(item.recordedResponseHeaders, undefined, 4)}
                                newValue={JSON.stringify(item.replayedResponseHeaders, undefined, 4)}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={null}
                                onLineNumberClick={(lineId, e) => { console.log({ lineId, e }); }}
                            />
                        </div>
                    </div>
                )}
                {item.recordedData == null && (
                    <div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Recorded Data</div>
                )}
                {item.replayedData == null && (
                    <div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Replayed Data</div>
                )}
                {item.recordedData != null && item.replayedData != null && (
                    <div style={{ display: this.state.showResponseMessageBody ? "" : "none" }}>
                        <div className="diff-wrapper">
                            < ReactDiffViewer
                                styles={newStyles}
                                oldValue={item.expJSON}
                                newValue={item.actJSON}
                                splitView={true}
                                disableWordDiff={false}
                                diffArray={item.reductedDiffArray}
                                filterPath={this.state.filterPath}
                                onLineNumberClick={(lineId, e) => { console.log({ lineId, e }); }}
                                inputElementRef={this.inputElementRef}
                            />
                        </div>
                    </div>
                )}
            </div >);
        });

        return (
            <div style={{ padding: "18px", marginTop: "36px" }}>
                <div style={{ marginBottom: "18px" }}>
                    <FormGroup>
                        <Checkbox inline disabled>Request Headers</Checkbox>
                        <Checkbox inline disabled>Request Params</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseHeaders" checked={this.state.showResponseMessageHeader}>Response Headers</Checkbox>
                        <Checkbox inline onChange={this.toggleMessageContents} value="responseBody" checked={this.state.showResponseMessageBody} >Response Body</Checkbox>
                    </FormGroup>
                    <FormGroup>
                        <FormControl
                            ref={this.inputElementRef}
                            type="text"
                            value={this.state.filterPath}
                            placeholder="Enter text"
                            onChange={this.handleChange}
                            id="filterPathInputId"
                            inputRef={ref => { this.input = ref; }}
                        />
                    </FormGroup>
                </div>
                <div>
                    {jsxContent}
                </div>
            </div>

        );
    }
}

export default ShareableDiff;