import React, { Component } from 'react';
import {connect} from "react-redux";
import "./Navigation.css"
import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";
import {Link} from "react-router-dom";
import AddTestConfig from "../AddTestConfig";
import ViewSelectedTestConfig from "../ViewSelectedTestConfig";

class Navigation extends Component{
    constructor (props) {
        super(props)
        this.state = {
            appsVisible: false,
            gphFS: false,
            tcFS: false
        };
        this.pieRef = React.createRef();
        this.handleShowHideApps = this.handleShowHideApps.bind(this);
        this.handleChangeForApps = this.handleChangeForApps.bind(this);
        this.statusInterval;
    }

    componentDidMount() {
        const {
            dispatch,
            cube
        } = this.props;
        setTimeout(() => {
            dispatch(cubeActions.getApps());
            dispatch(cubeActions.getInstances());
        }, 0);
    }

    handleShowHideApps() {
        const {appsVisible} = this.state;
        this.setState({appsVisible: !appsVisible});
    }

    handleChangeForApps (e) {
        const { dispatch } = this.props;
        const {cube} = this.props;
        if (e !== cube.selectedApp) {
            dispatch(cubeActions.setSelectedApp(e));
            setTimeout(() => {
                const {cube} = this.props;
                dispatch(cubeActions.clearGolden());
                dispatch(cubeActions.getGraphDataByAppId(cube.selectedAppObj.id));
                dispatch(cubeActions.getTimelineData(e));
                dispatch(cubeActions.getTestConfigByAppId(cube.selectedAppObj.id));
                dispatch(cubeActions.getTestIds(e));
                dispatch(cubeActions.setSelectedTestIdAndVersion('', ''));
            });
        }
    }

    createAppList(cube) {
        if (cube.appsListReqStatus != cubeConstants.REQ_SUCCESS || !cube.appsList) {
            return 'Loading...'
        }
        let jsxContent = cube.appsList.map(item => {
            return (
                <div key={item.id} className="app-wrapper" onClick={() => this.handleChangeForApps(item.name)}>
                    <div className="app-img">
                        <img src={"/assets/images/" + item.name + "-app.png"} alt=""/>
                    </div>
                    <div className={cube.selectedApp == item.name ? "app-name selected" : "app-name"}>
                        {item.name}
                    </div>
                </div>
            );
        })

        return jsxContent;
    }

    checkReplayStatus = (replayId) => {
        const { dispatch, cube } = this.props;
        this.statusInterval = setInterval(() => {
            const {cube} = this.props;
            if (cube.replayStatusObj && (cube.replayStatus == 'Completed' || cube.replayStatus == 'Error')) {
                clearInterval(this.statusInterval);
            } else {
                checkStatus();
            }
        }, 1000);
        
        let checkStatus = () => {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, replayId, cube.selectedApp));
        };
    }

    render() {
        const {lo, cube} = this.props;
        const {appsVisible} = this.state;

        return (
            <div className="navigation-main">

                <div className="nav-cont h-100">
                    <div className="left-pane text-center">
                        <div className="company-name">
                            <img className="inline-block" src="/assets/images/md-circle-logo.png" alt="MESH DYNAMICS"/>
                        </div>
                        <div className="q-links-top">
                            <Link to="/">
                                <div className="link-q"><i className="fas fa-chart-bar"></i></div>
                            </Link>
                            <Link to="/test_config">
                                <div className="link-q"><i className="fas fa-caret-square-right"></i></div>
                            </Link>
                        </div>
                        <div className="q-links">
                            <div className="link-q"><i className="fas fa-bell"></i></div>
                            <div className="link-q"><i className="fas fa-cog"></i></div>
                            <div className="link-q"><i className="fas fa-user-circle"></i></div>
                            <div className="link-q" onClick={lo}><i title="Sign Out" className="fas fa-sign-out-alt"></i></div>
                        </div>
                    </div>
                    <div className={appsVisible ? "app-select" : "app-select disp-none"}>
                        <h4 className="applic">Applications</h4>
                        <div className="app-list">
                            {this.createAppList(cube)}
                        </div>
                    </div>
                    <div className="app-s-b">
                        <div className="sh-cont" onClick={this.handleShowHideApps}>
                            <i className={appsVisible ? "fa fa-angle-left" : "fa fa-angle-right"}></i>
                        </div>
                    </div>
                    <div className="info-wrapper">
                        <div>
                            <div className="label-n">APPLICATION</div>
                            <div className="application-name">{cube.selectedApp ? cube.selectedApp : "N/A"}</div>
                        </div>
                        <div className={!cube.hideTestConfig && cube.testConfig ? "info-div" : "hidden"}>
                            <div className="div-label">
                                Test Configuration
                            </div>
                            <div className="margin-top-10">
                                <div className="label-n">TEST NAME</div>
                                <div className="value-n">{cube.testConfig ? cube.testConfig.testConfigName : ''}</div>
                            </div>
                            <div className={cube.golden ? "margin-top-10" : "hidden"}>
                                <div className="label-n">GOLDEN</div>
                                <div className="value-n">{cube.golden ? cube.golden : ''}</div>
                            </div>
                            <div className={cube.goldenTimeStamp ? "margin-top-10" : "hidden"}>
                                <div className="label-n">Run Time</div>
                                <div className="value-n">{cube.goldenTimeStamp ? cube.goldenTimeStamp : ''}</div>
                            </div>
                            <div className={cube.testConfig && cube.testConfig.gatewayService ? "margin-top-10" : "hidden"}>
                                <div className="label-n">GATEWAY</div>
                                <div className="value-n">{cube.testConfig && cube.testConfig.gatewayService ? cube.testConfig.gatewayService.name : ''}</div>
                            </div>
                            <div className={cube.testConfig && cube.testConfig.criteria ? "margin-top-10" : "hidden"}>
                                <div className="label-n">CRITERIA</div>
                                <div className="value-n">{cube.testConfig && cube.testConfig.criteria ? cube.testConfig.criteria : ''}</div>
                            </div>
                            <div className={cube.testConfig && cube.testConfig.mocks ? "margin-top-10" : "hidden"}>
                                <div className="label-n">MOCK(S)</div>
                                <div className="value-n">{cube.testConfig && cube.testConfig.mocks ? cube.testConfig.mocks.join(",") : ''}</div>
                            </div>
                            {
                                cube.pathResultsParams && cube.pathResultsParams.replayId ?
                                <div className="margin-top-10">
                                    <div className="label-n">TEST ID</div>
                                    <div className="value-n">{cube.pathResultsParams.replayId}</div>
                                    <div className="label-n">TIMESTAMP</div>
                                    <div className="value-n">{cube.pathResultsParams.timeStamp}</div>    
                                </div> : null
                            }
                        </div>

                        <div className={!cube.hideServiceGraph ? "margin-top-10 info-div" : "hidden"}>
                            <div className="div-label">
                                Service Map
                            </div>
                            <div className="service-gph">
                                <img src={"/assets/images/" + cube.selectedApp + "-app.png"} alt=""/>
                            </div>
                        </div>

                        <div className={!cube.hideTestConfigSetup ? "margin-top-10 info-div" : "hidden"}>
                            <AddTestConfig />
                        </div>

                        {!cube.hideTestConfigView ? <ViewSelectedTestConfig checkReplayStatus={this.checkReplayStatus}/> : null}
                    </div>
                </div>
            </div>
        );
    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}

const connectedNavigation = connect(mapStateToProps)(Navigation);

export default connectedNavigation
