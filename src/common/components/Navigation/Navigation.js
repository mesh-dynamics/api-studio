import React, { Component } from 'react';
import {connect} from "react-redux";
import { Radio, Checkbox } from 'react-bootstrap';
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
        this.replayStatusInterval;
        this.analysisStatusInterval;
    }

    componentDidMount() {
        const { dispatch } = this.props;

        dispatch(cubeActions.getApps());
        dispatch(cubeActions.getInstances());
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
                dispatch(cubeActions.clearTimeline());
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
                        <img src={"https://app.meshdynamics.io/assets/images/" + item.name + "-app.png"} alt=""/>
                        {/* <img src={"./assets/images/" + item.name + "-app.png"} alt=""/> */}
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
        this.replayStatusInterval = setInterval(() => {
            const {cube} = this.props;
            if (cube.replayStatusObj && (cube.replayStatus == 'Completed' || cube.replayStatus == 'Error')) {
                // after the replay is completed stop polling and poll for analysis status
                clearInterval(this.replayStatusInterval);
                this.checkAnalysisStatus(replayId);
            } else {
                checkStatus();
            }
        }, 1000);
        
        let checkStatus = () => {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, replayId, cube.selectedApp));
        };
    }

    checkAnalysisStatus = (replayId) => {
        const { dispatch, cube } = this.props;
        this.analysisStatusInterval = setInterval(() => {
            const {cube} = this.props;
            if (cube.analysisStatusObj && (cube.analysisStatus == 'Completed' || cube.analysisStatus == 'Error')) {
                clearInterval(this.analysisStatusInterval);
            } else {
                checkStatus();
            }
        }, 1000);
        let checkStatus = () => {
            dispatch(cubeActions.getAnalysisStatus(replayId, cube.selectedApp));
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
                        <img className="inline-block" src="https://app.meshdynamics.io/assets/images/md-circle-logo.png" alt="MESH DYNAMICS"/>
                            {/* <img className="inline-block" src="./assets/images/md-circle-logo.png" alt="MESH DYNAMICS"/> */}
                        </div>
                        <div className="q-links-top">
                            <Link to="/">
                                <div className="link-q"><i className="fas fa-chart-bar"></i></div>
                            </Link>
                            <Link to="/test_config">
                                <div className="link-q"><i className="fas fa-caret-square-right"></i></div>
                            </Link>
                            <Link to={`/api_catalog?app=${cube.selectedApp}`}> 
                                <div className="link-q"><i className="fas fa-indent"></i></div>
                            </Link>

                            <Link to={`/http_client?app=${cube.selectedApp}`}>
                                <div className="link-q">
                                    <svg width="29"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                                    </svg>
                                </div>
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
                    
                    {!window.location.pathname.includes("api_catalog") && 
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
                                <img src={"https://app.meshdynamics.io/assets/images/" + cube.selectedApp + "-app.png"} alt=""/>
                                {/* <img src={"./assets/images/" + cube.selectedApp + "-app.png"} alt=""/> */}
                            </div>
                        </div>

                        <div className={!cube.hideTestConfigSetup ? "margin-top-10 info-div" : "hidden"}>
                            <AddTestConfig />
                        </div>

                        {!cube.hideTestConfigView ? <ViewSelectedTestConfig checkReplayStatus={this.checkReplayStatus}/> : null}

                        <div className={!cube.hideHttpClient ? "margin-top-10 info-div" : "hidden"}>
                            <div className="margin-top-10">
                                <div className="value-n">VIEW</div>
                            </div>
                            <div className="margin-top-10">
                                <div className="margin-top-10 vertical-middle">
                                    <Radio>
                                        TEST REQUESTS
                                    </Radio>
                                </div>
                                <div className="margin-top-10 vertical-middle">
                                    <Radio>
                                        MOCK REQUESTS
                                    </Radio>
                                </div>
                            </div>
                        </div>
                        <div className={!cube.hideHttpClient ? "margin-top-10 vertical-middle" : "hidden"}>
                            <Checkbox>
                                SAVE AS COPY
                            </Checkbox>
                        </div>
                        <div className={!cube.hideHttpClient ? "margin-top-20 text-center" : "hidden"}>
                            <div className="cube-btn width-50 text-center" style={{margin: "0 auto"}}>SAVE</div>
                        </div>
                    </div>}
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
