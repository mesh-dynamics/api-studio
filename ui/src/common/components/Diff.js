import React, { Component } from 'react';
import _ from 'lodash';
import './Diff.css'

import ReactDiffViewer from '../utils/diff/diff-main';
import ReduceDiff from '../utils/ReduceDiff';

class Diff extends Component {
    constructor(props) {
        super(props);
        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.state = {
            show: false,
        };
    }

    handleShow() {
        this.setState({ show: true });
    }

    handleClose() {
        this.setState({ show: false });
    }

    render() {
        let {recorded, replayRes, diff, filterPath} = this.props;
        let actJSON = JSON.stringify(replayRes, undefined, 4),
            expJSON = JSON.stringify(recorded, undefined, 4);
        let reductedDiffArray = null;
        if(diff && diff.length > 0) {
            let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
            reductedDiffArray = reduceDiff.computeDiffArray();
        } else if (diff && diff.length == 0) {
            console.log("here i am!");
            if(_.isEqual(expJSON, actJSON)) {
                let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                reductedDiffArray = reduceDiff.computeDiffArray();
            }
        }

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

        return (
            <div>
                <div className="diff-wrapper">
                    {/* <h3>
                        Expected vs Actual
                    </h3> */}
                    < ReactDiffViewer 
                    styles = {newStyles}
                    oldValue = {expJSON}
                    newValue = {actJSON}
                    splitView = {true}
                    disableWordDiff = {false}
                    diffArray = {reductedDiffArray}
                    filterPath = {filterPath}
                    onLineNumberClick = {(lineId, e) => {{lineId, e}}}
                    />
                </div>
            </div>
        )
    }
}

export default Diff;
