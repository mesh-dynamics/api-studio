import React, { Component } from 'react';
import './Diff.css'
import Modal from "react-bootstrap/es/Modal";

import ReactDiffViewer from '../utils/diff/diff-main';
import generator from '../utils/generator/json-path-generator';
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

    getButton(resolution) {
        switch (resolution) {
            case 'OK_Optional':
                return (<span className="pull-right small orange-b">OPTIONAL</span>);
            case 'OK_OptionalMismatch':
                return (<span className="pull-right small orange-b">OPTIONAL MISMATCH</span>);
            case 'OK_OtherValInvalid':
                return (<span className="pull-right small orange-b">OPTIONAL VALUE INVALID</span>);
            case 'ERR_ValTypeMismatch':
                return (<span className="pull-right small red-b">ERROR VAL TYPE MISMATCH</span>);
            case 'OK':
                return (<span className="pull-right small green-b">OK</span>);
        }
    }

    formatDiff() {
        let {diff} = this.props;
        let formattedDiff = [];
        let pathList = [];
        for (const d of diff) {
            pathList.push(d.path);
        }

        for (let i =  0; i < diff.length; i++) {
            const d = diff[i];
            let pArr = pathList[i].split('/');
            pArr.shift();
            let li = pArr.pop();
            pArr =  pArr.join('--');
            let type = 'obj';
            if (parseInt(li) == li) {
                type = 'array'
            }

            if (d.type == 'obj') {
                formattedDiff.push({
                    pArr: pArr,
                    type: type,
                    children: [d]
                });
            } else {
                let result = formattedDiff.filter(obj => {
                    return obj.pArr === pArr
                });

                if (result && result[0] && result[0].children) {
                    result[0].children.push(d);
                } else {
                    formattedDiff.push({
                        pArr: pArr,
                        type: type,
                        children: [d]
                    });
                }
            }
        }

        return (formattedDiff);
    }

    handleShow() {
        this.setState({ show: true });
    }

    handleClose() {
        this.setState({ show: false });
    }

    render() {
        let {recorded, replayRes, diff} = this.props;
        //const formattedDiff = this.formatDiff();
        let actJSON = JSON.stringify(replayRes),
            expJSON = JSON.stringify(recorded);
        console.log("actJSON - replayRes: ", actJSON);
        console.log("expJSON - recorded: ", expJSON);
        console.log(diff);
        let reduceDiff = new ReduceDiff("", actJSON, expJSON, diff);
        let reductedDiffArray = reduceDiff.computeDiffArray();
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

        var textedJson = JSON.stringify(recorded, undefined, 4);
        var textedJson1 = JSON.stringify(replayRes, undefined, 4);

        /*let formattedDiffElements = formattedDiff.map((d, i, fde) => {
            const keys = d.pArr.indexOf('--') != -1 ? d.pArr.split('--') : [d.pArr];

            let childrenElem = d.children.map(c => {
                //console.log(c.path);
                let ck = c.path.indexOf('/') != -1 ? c.path.split('/') : [c.path];
                let tempBef = JSON.parse(JSON.stringify(recorded));
                ck.shift();
                // keys.shift();
                // for (const key of ck) {
                //     temp = temp[key];
                // }

                for (let i = 0; i < ck.length; i++) {
                    tempBef = tempBef[ck[i]];
                }

                if (c.op == 'remove') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            {typeof c.value == 'string' ? (<span className="removed">{c.value}</span>) : (
                                <pre><div className="removed">{JSON.stringify(c.value, undefined, 4)}</div></pre>
                                )}
                                {this.getButton(c.resolution)}
                        </div>
                    )
                } else if (c.op == 'replace') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            <span className="removed">{JSON.stringify(c.fromValue)}</span>&nbsp;&nbsp;
                            <span className="added">{JSON.stringify(c.value)}</span>
                            {this.getButton(c.resolution)}
                        </div>
                    )
                } else if (c.op == 'add') {
                    return (
                        <div key={c.path} className="tabbed">
                            <span>{ck[ck.length-1] + ': '}</span>
                            <span className="removed">{c.fromValue ? JSON.stringify(c.fromValue) : JSON.stringify(tempBef)}</span>&nbsp;&nbsp;
                            <span className="added">{JSON.stringify(c.value)}</span>
                            {this.getButton(c.resolution)}
                        </div>
                    )
                }
            });

            if (d.type == 'array') {
                return (
                    <div key={d.pArr}>
                        <div>{keys[keys.length-1] + ': ['}</div>
                        {childrenElem}
                        <div>]</div>
                    </div>
                )
            } else {
                return (
                    <div key={d.pArr}>
                        <div>{keys[keys.length-1] + ': {'}</div>
                        {childrenElem}
                        <div>}</div>
                    </div>
                )
            }
        });*/

        /*let diffElems = diff.map((d, i, darr) => {
            const keys = d.path.split('/');
            let temp = JSON.parse(JSON.stringify(recorded));
            let tempBef = JSON.parse(JSON.stringify(recorded));
            keys.shift();
            for (const key of keys) {
                temp = temp[key];
            }

            for (let i = 0; i < keys.length - 1; i++) {
                tempBef = tempBef[keys[i]];
            }

            if (d.op == 'remove') {
                return (
                    <pre>
                        {keys[keys.length-1] + ': '}
                        <span className="removed">
                            {JSON.stringify(temp, undefined, 4)}
                        </span>
                    </pre>
                )
            } else {
                return (
                    <pre>
                        {keys[keys.length-1] + ': '}
                        {JSON.stringify(tempBef, undefined, 4)}&nbsp;&nbsp;
                        {JSON.stringify(d.value, undefined, 4)}
                    </pre>
                )
            }

        });*/

        return (
            <div style={{marginTop: '20px'}}>
                <div className="diff-wrapper">
                    <h3>
                        Expected vs Actual:&nbsp;&nbsp;
                        <span className="cube-btn" onClick={this.handleShow}>View JSON</span>
                    </h3>
                    {/*formattedDiffElements*/}
                    < ReactDiffViewer 
                    styles = {newStyles}
                    oldValue = {""}
                    newValue = {""}
                    splitView = {true}
                    disableWordDiff = {false}
                    diffArray = {reductedDiffArray}
                    onLineNumberClick = {() => {console.log(arguments)}}
                    />
                </div>

                <Modal show={this.state.show} onHide={this.handleClose}>
                    <Modal.Header closeButton>
                        <Modal.Title>Response Diff</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div className="left-json">
                            <h4>Expected</h4>
                            <textarea disabled name="" id="myTextarea" cols="30" rows="22">
                                {textedJson}
                            </textarea>
                        </div>
                        <div className="right-json">
                            <h4>Actual</h4>
                            <textarea disabled name="" id="myTextarea" cols="30" rows="22">
                                {textedJson1}
                            </textarea>
                        </div>
                    </Modal.Body>
                </Modal>
            </div>
        )
    }
}

export default Diff;
