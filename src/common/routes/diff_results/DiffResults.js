import React, { Component } from 'react';
import Diff from "../../components/Diff";
import { FormGroup, FormControl, Glyphicon } from 'react-bootstrap';
import _ from 'lodash';
import Tippy from '@tippy.js/react';
import 'tippy.js/themes/light.css';

import ReactDiffViewer from '../../utils/diff/diff-main';
import ReduceDiff from '../../utils/ReduceDiff';
import {Link} from "react-router-dom";

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

class DiffResults extends Component {
    constructor(props, context) {
        super(props, context);

        this.handleChange = this.handleChange.bind(this);

        this.state = {
            filterPath: '',
            computedDiffReplayList: []
        };

        this.inputElementRef = React.createRef();
    }

    componentDidUpdate(prevProps) {
        if (this.props.completeReplayList.length === prevProps.completeReplayList.length) {
            return;
        }
        let { completeReplayList } = this.props;
        let computedDiffReplayList = completeReplayList.map((item, index) => {
            let recordedData, replayedData;
            if (item.recordResponse) {
                if (item.recordResponse.body) {
                    try {
                        if(item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
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
                recordedData = null;
            }
            if (item.replayResponse) {
                if (item.replayResponse.body) {
                    try {
                        if(item.recordResponse.mimeType.indexOf('json') > -1 && item.replayResponse.mimeType.indexOf('json') > -1) {
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
                recordedData: recordedData,
                replayedData: replayedData,
                actJSON: actJSON,
                expJSON: expJSON,
                reductedDiffArray: reductedDiffArray
            }
        });
        this.setState({ "computedDiffReplayList": computedDiffReplayList });
    }

    handleChange(e) {
        this.setState({ filterPath: e.target.value });
    }
    render() {
        let { completeReplayList, showHide, updateGolden } = this.props;
        let computedDiffReplayList = this.state.computedDiffReplayList;
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
        let jsxContent = computedDiffReplayList.map((item, index) => {
            return (<div key={item.path + '-' + index} style={{ borderBottom: "1px solid #eee" }}>
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
                                                                Object.entries(item.qparams).map(([key, value]) => (<tr>
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
                {item.recordedData == null && (<div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Recorded Data</div>)}
                {item.replayedData == null && (<div style={{ margin: "27px", textAlign: "center", fontSize: "24px" }}>No Replayed Data</div>)}
                {item.recordedData != null && item.replayedData != null && (
                    <div>
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
            <div>
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <span className="link" onClick={showHide}><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO PATH RESULTS</span>
                    <span className="link pull-right" onClick={updateGolden}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-check-square font-15"></i>&nbsp;UPDATE OPERATIONS</span>
                    <Link to="/review_golden_updates" className="hidden">
                        <span className="link pull-right"><i className="fas fa-pen-square font-15"></i>&nbsp;REVIEW GOLDEN UPDATES</span>
                    </Link>
                </div>
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
                {jsxContent}
            </div>

        );
    }
}

export default DiffResults;
