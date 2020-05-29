import React, { Component } from 'react';
import {connect} from "react-redux";
import CytoscapeReactWrapper from "../replay/replay";
import {cubeActions} from "../../actions";

class SetupTestConfig extends Component {
    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCSetup(true));
        dispatch(cubeActions.hideHttpClient(true));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCSetup(false));
        dispatch(cubeActions.hideHttpClient(false));
    }

    render() {
        return (
            <div>
                <div className="content-wrapper">
                    <CytoscapeReactWrapper />
                </div>

            </div>
        )
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedSetupTestConfig = connect(mapStateToProps)(SetupTestConfig);

export default connectedSetupTestConfig