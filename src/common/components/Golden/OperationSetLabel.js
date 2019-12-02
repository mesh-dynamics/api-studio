import React from 'react'
import Tippy from '@tippy.js/react'
import 'tippy.js/themes/light-border.css'
import { Glyphicon } from 'react-bootstrap'
import { connect } from "react-redux"
import {cubeActions} from "../../actions";

class OperationSetLabel extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.state = {
            showBugTippy: false,
        }
    }

    findInOperationSet() {
        const {cube, jsonPath} = this.props;
        let indexMOS = cube.multiOperationsSet.findIndex((elem) => elem.path && elem.path == cube.pathResultsParams.path);
        if (indexMOS == -1)
            return false;
        for (const op of cube.multiOperationsSet[indexMOS].operationSet) {
            if (jsonPath.replace("<BEGIN>", "") == (op.path)) {
                return true;
            }
        }
        return false;
    }

    findInOperations() {
        const {cube, jsonPath} = this.props;
        for (let key in cube.templateOperationSetObject) {
            if (cube.templateOperationSetObject.hasOwnProperty(key) && cube.templateOperationSetObject[key].operations) {
                const keyObj = JSON.parse(key);
                if (keyObj['path'] == cube.pathResultsParams.path) {
                    for (const op of cube.templateOperationSetObject[key].operations) {
                        if (jsonPath.replace("<BEGIN>", "") == (op.path)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    findInJiraBugs(){
        const {cube, jsonPath} = this.props;
        for (const op of cube.jiraBugs) {
            if (jsonPath.replace("<BEGIN>", "") == (op.jsonPath)) {
                return true;
            }
        }
        return false;
    }

    removeFromOS = () => {
        const {cube, jsonPath, dispatch} = this.props;
        for (let i =  0; i < cube.newOperationSet.length; i++) {
            if (jsonPath.replace("<BEGIN>", "") == (cube.newOperationSet[i].path)) {
                dispatch(cubeActions.removeFromNOS(i));
                return;
            }
        }
        return;
    };

    removeFromOperations = () => {
        const {cube, jsonPath, dispatch} = this.props;
        for (let i =  0; i < cube.operations.length; i++) {
            if (jsonPath.replace("<BEGIN>", "") == (cube.operations[i].path)) {
                dispatch(cubeActions.removeFromOperations(i));
                return;
            }
        }
        return;
    };

    setShowBugTippy = () => this.setState({ showBugTippy: true });

    setHideBugTippy = () => this.setState({ showBugTippy: false });

    getIssueUrl = () => {
        const { cube: { jiraBugs }, jsonPath } = this.props;
        const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath);

        return  jiraBugs.length > 0 && bugItem ? bugItem.issueUrl : "";
    }
    
    getIssueId = () => {
        const { cube: { jiraBugs }, jsonPath } = this.props;
        const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath);

        return  jiraBugs.length > 0 && bugItem ? bugItem.issueKey : "";
    };

    handleIssueUrlClick = () => {
        this.setHideBugTippy();
        window.open(this.getIssueUrl())
    }

    removeFromJiraBugs = () => {
        // To Be Implemented
    }


    render() {
        const tippyContent = (
            <div>
                <strong>{this.props.jsonPath ? this.props.jsonPath.replace("<BEGIN>", "") : ""}</strong>.
            </div>
        );

        const bugContent = (
            <div style={{ fontSize: "14px", display: "flex", flexDirection: "column", alignItems: "flex-start", padding: "10px", background: "#ECE7E6", cursor: "default"}}>
                <div>
                    <span>JSON Path: </span>
                    <span>{this.props.jsonPath ? this.props.jsonPath.replace("<BEGIN>", "") : ""}</span>
                </div>
                <div>
                    <span>Jira Issue: </span>
                    <span style={{ cursor: "pointer", color: "#0052CC"}} onClick={this.handleIssueUrlClick}>{this.getIssueId()}</span>
                </div>
            </div>
        );

        return this.props.jsonPath && this.props.jsonPath.indexOf("<END>") < 0 ? (
            <span>
                <Tippy content={tippyContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true}>
                    <span onDoubleClick={this.removeFromOS} className={this.findInOperationSet() ? '' : 'hidden'}><Glyphicon glyph="asterisk" /></span>
                </Tippy>
                <Tippy content={tippyContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true}>
                    <span onDoubleClick={this.removeFromOperations} className={this.findInOperations() ? '' : 'hidden'}><Glyphicon glyph="retweet" /></span>
                </Tippy>
                <Tippy visible={this.state.showBugTippy} content={bugContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"} flipOnUpdate={true}>
                    <span onClick={this.setShowBugTippy} onDoubleClick={this.removeFromJiraBugs} className={this.findInJiraBugs() ? '' : "hidden"}><i className="fas fa-bug"></i></span>
                </Tippy>
            </span>
        ) : "";
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedOperationSetLabel = connect(mapStateToProps)(OperationSetLabel);

export default connectedOperationSetLabel;
