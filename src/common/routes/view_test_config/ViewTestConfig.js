import React, { Component } from 'react';
import {connect} from "react-redux";
import Replay from "../replay/replay";
import {cubeActions} from "../../actions";
import {Redirect} from "react-router";

class ViewTestConfig extends Component {
    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCInfo(true));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCInfo(false));
    }

    render() {
        const {cube} = this.props;
        return (
            <React.Fragment>
                {
                    cube.testConfig && cube.selectedApp != cube.testConfig.app.name ? <Redirect to="/test_config" /> :
                        (<div>
                            <Replay />
                        </div>)
                }
            </React.Fragment>
        )
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedViewTestConfig = connect(mapStateToProps)(ViewTestConfig);

export default connectedViewTestConfig
