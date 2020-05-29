import React, { Component } from 'react';
import {connect} from "react-redux";
import "./TestConfig.css"
import {cubeActions} from "../../actions";
import {Link} from "react-router-dom";
import Breadcrumb from "../../components/breadcrumb/Breadcrumb";
import {cubeConstants} from "../../constants";

class TestConfig extends Component {

    setTestConfig(config) {
        const { dispatch, history } = this.props;
        dispatch(cubeActions.setTestConfig(config));
        setTimeout(() => {
            history.push("/test_config_view");
        })
    }

    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(true));
        dispatch(cubeActions.hideServiceGraph(true));
        dispatch(cubeActions.hideHttpClient(true));
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideTestConfig(false));
        dispatch(cubeActions.hideServiceGraph(false));
        dispatch(cubeActions.hideHttpClient(false));
    }

    createTestConfig(cube) {
        if (!cube.testConfigList || cube.testConfigList.length == 0) {
            return 'No Test Config Found'
        }
        let jsxContent = cube.testConfigList.map((item, index) => {
            return (
                <div key={item.id} className="grid-content" onClick={() => this.setTestConfig(item)}>
                    <div className={cube.testConfig && cube.testConfig.testConfigName ==  item.testConfigName ? "g-head selected" : "g-head"}>
                        {item.testConfigName}
                    </div>

                    <div className="g-body">
                        <div className="margin-bottom-10">
                            <span className="t-name">
                                GATEWAY:&nbsp;
                            </span>
                            <strong>{item.gatewayServiceName ? item.gatewayServiceName : ""}</strong>
                        </div>

                        <div className="margin-bottom-10">
                            <span className="t-name">
                                Paths:&nbsp;
                            </span>
                            <strong>{item.paths && item.paths.length > 0 ? item.paths.join(', ') : ""}</strong>
                        </div>

                        <div className={item.criteria ? "margin-bottom-10" : "hidden"}>
                            <span className="t-name">
                                CRITERIA:&nbsp;
                            </span>
                            <strong>{item.criteria}</strong>
                        </div>

                        <div className="margin-bottom-10">
                            <span className="t-name">
                                MOCK(s):&nbsp;
                            </span>
                            <strong>{item.mocks && item.mocks.length > 0 ? item.mocks.join(', ') : ""}</strong>
                        </div>
                    </div>
                </div>
            );
        });

        return jsxContent;
    }

    render() {
        const { cube } = this.props;
        return (
            <div className="content-wrapper">
                {/*<div className="crumb-wrap">
                    <Breadcrumb crumbs={[{label: "Application", value: "MovieInfo"}, {label: "Service", value: "List"},
                        {label: "Logical Service", value: "Reviews"}, {label: "API", value: "IMDb"}]}></Breadcrumb>
                </div>*/}
                <div>
                    <h4 className="inline-block margin-right-10">Test Configurations</h4>
                    <Link to="/test_config_setup">
                        <span className="cube-btn">CONFIGURE NEW TEST</span>
                    </Link>
                </div>

                <div className="tc-grid">
                    {this.createTestConfig(cube)}
                </div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedTestConfig = connect(mapStateToProps)(TestConfig);

export default connectedTestConfig
