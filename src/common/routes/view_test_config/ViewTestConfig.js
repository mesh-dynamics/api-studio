import React, { Component } from 'react';
import {connect} from "react-redux";
import {Redirect} from "react-router";
import {Route} from "react-router-dom";
import ServiceGraph from "../service_graph/ServiceGraph";
import GoldenVisibility from "../../components/Golden-Visibility/GoldenVisibility";
import TestClusterStatus from "../../components/Test-Cluster/TestClusterStatus";
import { cubeActions } from "../../actions";

class ViewTestConfig extends Component {
    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCInfo(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(true));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.showTCInfo(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(false));
    }

    renderSubRoutes = () => (
        <div style={{ width:"100%" }}>
            <Route exact path="/test_config_view" component={ServiceGraph}/>
            <Route exact path="/test_config_view/golden_visibility" component={GoldenVisibility} />
            <Route exact path="/test_config_view/test_cluster" component={TestClusterStatus} />
        </div>
    );

    render() {
        const {cube} = this.props;
        return (
            <React.Fragment>
                {
                    cube.testConfig && cube.selectedApp != cube.testConfig.appName 
                    ? <Redirect to="/test_config" /> 
                    : this.renderSubRoutes()
                }
            </React.Fragment>
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
});

export default connect(mapStateToProps)(ViewTestConfig);