/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react'
import Tippy from '@tippy.js/react'
import 'tippy.js/themes/light-border.css'
import { Glyphicon } from 'react-bootstrap'
import { connect } from "react-redux"
import { cubeActions } from "../../actions";
import Modal from "react-bootstrap/lib/Modal";

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
            if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == (op.path)) {
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
                        if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == (op.path)) {
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
            if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == op.jsonPath) {
                return true;
            }
        }
        return false;
    }

    removeFromGoldenOS = () => {
        const {cube, jsonPath, dispatch} = this.props;
        let indexMOS = cube.multiOperationsSet.findIndex((elem) => elem.path && elem.path == cube.pathResultsParams.path);
        if (indexMOS == -1)
            return false;
        /*for (const op of cube.multiOperationsSet[indexMOS].operationSet) {
            if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == (op.path)) {
                return true;
            }
        }*/
        let opList = cube.multiOperationsSet[indexMOS].operationSet;
        for (let ind = 0; ind < opList.length; ind++) {
            if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == (opList[ind].path)) {
                dispatch(cubeActions.removeFromNOS(ind, opList.length, indexMOS));
                break;
            }
        }
        return;
    };

    removeFromOperations = () => {
        const {cube, jsonPath, dispatch} = this.props;
        for (let key in cube.templateOperationSetObject) {
            if (cube.templateOperationSetObject.hasOwnProperty(key) && cube.templateOperationSetObject[key].operations) {
                const keyObj = JSON.parse(key);
                if (keyObj['path'] == cube.pathResultsParams.path) {
                    let opList = cube.templateOperationSetObject[key].operations;
                    for (let ind = 0; ind < opList.length; ind++) {
                        if (jsonPath.replace("<BEGIN>", "").replace("<END>", "") == (opList[ind].path)) {
                            dispatch(cubeActions.removeFromRuleBook(jsonPath.replace("<BEGIN>", "").replace("<END>", "")));
                            dispatch(cubeActions.removeFromOperations(ind, opList.length, key));
                            break;
                        }
                    }
                }
            }
        }
        return;
    };

    setShowBugTippy = () => this.setState({ showBugTippy: true });

    setHideBugTippy = () => this.setState({ showBugTippy: false });

    getIssueUrl = () => {
        const { cube: { jiraBugs }, jsonPath } = this.props;
        
        if(jsonPath) {
            const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath.replace("<BEGIN>", "").replace("<END>", ""));

            return  jiraBugs.length > 0 && bugItem ? bugItem.issueUrl : "";
        }

        return;
    }
    
    getIssueId = () => {
        const { cube: { jiraBugs }, jsonPath } = this.props;
        if(jsonPath) {
            const bugItem = jiraBugs.find(item => item.jsonPath === jsonPath.replace("<BEGIN>", "").replace("<END>", ""));

            return  jiraBugs.length > 0 && bugItem ? bugItem.issueKey : "";
        }
        
        return;
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
                <strong>{this.props.jsonPath ? this.props.jsonPath.replace("<BEGIN>", "").replace("<END>", "") : ""}</strong>.
            </div>
        );

        return this.props.jsonPath && this.props.jsonPath.indexOf("<END>") < 0 ? (
            <span>
                <Tippy flip={false} content={tippyContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"}>
                    <span onDoubleClick={this.removeFromGoldenOS} className={this.findInOperationSet() ? '' : 'hidden'}><Glyphicon glyph="asterisk" /></span>
                </Tippy>
                <Tippy flip={false} content={tippyContent} arrow={true} interactive={true} animateFill={false} distance={7} animation={"fade"} size={"large"} theme={"light-border"} trigger={"click"} appendTo={"parent"}>
                    <span onDoubleClick={this.removeFromOperations} className={this.findInOperations() ? '' : 'hidden'}><Glyphicon glyph="retweet" /></span>
                </Tippy>
                
                <span onClick={this.setShowBugTippy} onDoubleClick={this.removeFromJiraBugs} className={this.findInJiraBugs() ? '' : "hidden"}><i className="fas fa-bug"></i></span>
                
                <Modal show={this.state.showBugTippy} dialogClassName="os-popover-modal">
                    <div style={{ background: "#ECE7E6", cursor: "default", borderRadius: "3px", padding: "3px" }}>
                        <span
                            onClick={this.setHideBugTippy}
                            style={{ display: "flex", justifyContent: "flex-end", padding: "3px", cursor: "pointer", width: "100%", fontSize: "12px"}}
                        >
                            <i className="fas fa-times" style={{ color: "#616060"}}></i>
                        </span>
                        <div style={{ fontSize: "14px", display: "flex", flexDirection: "column", alignItems: "flex-start", padding: "10px" }}>
                            <div>
                                <span>JSON Path: </span>
                                <span>{this.props.jsonPath ? this.props.jsonPath.replace("<BEGIN>", "").replace("<END>", "") : ""}</span>
                            </div>
                            <div>
                                <span>Jira Issue: </span>
                                <span style={{ cursor: "pointer", color: "#0052CC"}} onClick={this.handleIssueUrlClick}>{this.getIssueId()}</span>
                            </div>
                        </div>
                    </div>
                </Modal>
            </span>
        ) : "";
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube
});

export default connect(mapStateToProps)(OperationSetLabel);
