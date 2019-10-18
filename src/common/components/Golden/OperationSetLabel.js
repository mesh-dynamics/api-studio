import React from 'react'
import Tippy from '@tippy.js/react'
import 'tippy.js/themes/light-border.css'
import { Glyphicon } from 'react-bootstrap'
import { connect } from "react-redux"
import {cubeActions} from "../../actions";

class OperationSetLabel extends React.Component {
    constructor(props, context) {
        super(props, context);
        //cube.newOperationSet []
        //cube.operations []
    }

    findInOperationSet() {
        const {cube, jsonPath} = this.props;
        for (const op of cube.newOperationSet) {
            if (op.path == jsonPath) {
                return true;
            }
        }
        return false;
    }

    findInOperations() {
        const {cube, jsonPath} = this.props;
        for (const op of cube.operations) {
            if (op.path == jsonPath) {
                return true;
            }
        }
        return false;
    }

    removeFromOS = () => {
        const {cube, jsonPath, dispatch} = this.props;
        for (let i =  0; i < cube.newOperationSet.length; i++) {
            if (cube.newOperationSet[i].path == jsonPath) {
                dispatch(cubeActions.removeFromNOS(i));
                return;
            }
        }
        return;
    };

    removeFromOperations = () => {
        const {cube, jsonPath, dispatch} = this.props;
        for (let i =  0; i < cube.operations.length; i++) {
            if (cube.operations[i].path == jsonPath) {
                dispatch(cubeActions.removeFromOperations(i));
                return;
            }
        }
        return;
    };

    render() {
        const tippyContent = (
            <div>
                <strong>{this.props.jsonPath ? this.props.jsonPath.replace("<BEGIN>", "") : ""}</strong>.
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
