import React, { Component, Fragment } from 'react';
import {connect} from "react-redux";
import UserAvatar from 'react-user-avatar';
import { Radio, Checkbox, Tabs, Tab, Panel, Label } from 'react-bootstrap';
import "./Navigation.css"
import {cubeActions} from "../../actions";
import {cubeConstants} from "../../constants";
import {Link} from "react-router-dom";
import AddTestConfig from "../AddTestConfig";
import ViewSelectedTestConfig from "../ViewSelectedTestConfig";
import config from '../../config';
import { ipcRenderer } from '../../helpers/ipc-renderer';

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

    componentWillMount() {
        const { dispatch } = this.props;

        if(PLATFORM_ELECTRON) {
            ipcRenderer.on('get_config', (event, appConfig) => {
                ipcRenderer.removeAllListeners('get_config');
                
                config.apiBaseUrl= `${appConfig.domain}/api`;
                config.recordBaseUrl= `${appConfig.domain}/api/cs`;
                config.replayBaseUrl= `${appConfig.domain}/api/rs`;
                config.analyzeBaseUrl= `${appConfig.domain}/api/as`;               
                
                dispatch(cubeActions.getApps());
                dispatch(cubeActions.getInstances());
            });
        }
    }

    componentDidMount() {
        const { dispatch, user } = this.props;

        if(PLATFORM_ELECTRON) {
            ipcRenderer.send('get_config');
            ipcRenderer.send('set_user', user);
        } else {
            dispatch(cubeActions.getApps());
            dispatch(cubeActions.getInstances());
        }
    }

    // componentWillUnmount() {
    //     if(isElectron()) {
    //         ipcRenderer.removeAllListeners('get_config');
    //     }
    // }

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

    createAppList() {
        const { cube: { appsList, appsListReqStatus, selectedApp } } = this.props;

        if(appsList.length === 0 && appsListReqStatus === cubeConstants.REQ_LOADING) {
            return 'Loading...'
        }

        return (
            <Fragment>
            {
                appsList.map(item => 
                    (
                        <div key={item.id} className="app-wrapper" onClick={() => this.handleChangeForApps(item.name)}>
                            <div className="app-img">
                                <img src={"https://app.meshdynamics.io/assets/images/" + item.name + "-app.png"} alt=""/>
                                {/* <img src={"./assets/images/" + item.name + "-app.png"} alt=""/> */}
                            </div>
                            <div className={selectedApp == item.name ? "app-name selected" : "app-name"}>
                                {item.name}
                            </div>
                        </div>
                    )
                )
            }
            </Fragment>
        );
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
        const {lo, cube, user: {username}} = this.props;
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
                            <Link to={`/http_client?app=${cube.selectedApp}`}>
                                <div title="API Editor" className="link-q">
                                    <svg width="29"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                                    </svg>
                                </div>
                            </Link>
                            <Link to={`/api_catalog/api?app=${cube.selectedApp}`}> 
                                <div title="API Catalog" className="link-q"><i className="fas fa-indent"></i></div>
                            </Link>
                            <Link to={`/configs?app=${cube.selectedApp}`}>
                                <div title="Test Runner" className="link-q"><i className="fas fa-caret-square-right"></i></div>
                            </Link>
                            <Link to="/test_results">
                                <div title="Test Results" className="link-q"><i className="fas fa-chart-bar"></i></div>
                            </Link>                            
                        </div>
                        <div className="q-links">
                            {/* <div title="Notification" className="link-q"><i className="fas fa-bell"></i></div>
                            <div title="Settings" className="link-q"><i className="fas fa-cog"></i></div> */}
                            <div title={username} className="link-q">
                                
                                <a href="https://forms.office.com/Pages/ResponsePage.aspx?id=cFWmFBw2NkC9cZE-UHbkR9N-ipU4B_JMjy3zTczH9hFUM1RHRzhUSFkyM0wxVFFESU9ESk5MWlNMWC4u"
                                    target="_blank" title="">  
                                    <div title="Feedback" className="link-q">
                                        <i className="fa fa-comment" aria-hidden="true"></i>
                                    </div>
                                </a>
                                <a href="https://docs.meshdynamics.io" target="_blank" title="">  
                                    <div title="Help" className="link-q">
                                        <i className="fa fa-question" aria-hidden="true"></i>
                                    </div>
                                </a>
                                <Link to="/account">
                                    <UserAvatar size="24" name={username} className="user-avatar" color="#CCC6B0"/>
                                </Link>
                            </div>
                            <div title="Sign Out" className="link-q" onClick={lo}>
                                <i className="fas fa-sign-out-alt"></i>
                            </div>
                        </div>
                    </div>
                    <div className={appsVisible ? "app-select" : "app-select disp-none"}>
                        <h4 className="applic">Applications</h4>
                        <div className="app-list">
                            {this.createAppList()}
                        </div>
                    </div>
                    <div className="app-s-b">
                        <div className="sh-cont" onClick={this.handleShowHideApps}>
                            <i className={appsVisible ? "fa fa-angle-left" : "fa fa-angle-right"}></i>
                        </div>
                    </div>
                    
                    {!window.location.pathname.includes("http_client") && !window.location.pathname.includes("api_catalog")
                     && !window.location.pathname.includes("/account") && 
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
