import React, { Component } from 'react'
import { Row, Col, Clearfix } from 'react-bootstrap'
import { Form, FormGroup, ControlLabel, FormControl, Button } from 'react-bootstrap'
import { XPanel, PageTitle } from '../../components'
import { connect } from 'react-redux';
import { cubeActions } from '../../actions';
import { cubeConstants } from '../../constants';
import Select from 'react-select';
import Modal from "react-bootstrap/es/Modal";
import {Redirect} from "react-router-dom";

class configSample extends Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
            testIdPrefix: '',
            show: false,
        };
        this.doAnalysis = true;
        this.statusInterval;
        this.handleChangeForApps = this.handleChangeForApps.bind(this);
        this.handleChangeForTestIds = this.handleChangeForTestIds.bind(this);
        this.replay = this.replay.bind(this);
        this.getReplayStatus = this.getReplayStatus.bind(this);
        /*this.handleTestIdPrefixChange = this.handleTestIdPrefixChange.bind(this);
        this.handleTestIdPrefixSubmit = this.handleTestIdPrefixSubmit.bind(this);*/
    }

    componentDidMount() {
        const {
            dispatch,
            cube
        } = this.props;
        dispatch(cubeActions.getTestIds());
        /*if (cube.selectedTestId == cubeConstants.CREATE_NEW) {
            this.setState({testIdPrefix: cube.selectedApp.replace(' ', '-')})
        }*/
    }

    componentWillReceiveProps(nextProps, prevState) {
        // do things with nextProps.someProp and prevState.cachedSomeProp
        const cube = nextProps.cube;
        console.log(typeof cube.replayStatusObj);
        if (cube.replayStatusObj && (cube.replayStatusObj.status == 'Completed' || cube.replayStatusObj.status == 'Error')) {
            clearInterval(this.statusInterval);
            this.setState({show: false, toAnalysis: true});
            const {dispatch} = this.props;
            if(this.doAnalysis) {
                dispatch(cubeActions.getAnalysis(cube.selectedTestId, cube.replayId.replayid));
                dispatch(cubeActions.getReport(cube.selectedTestId, cube.replayId.replayid));
                this.doAnalysis = false;
            }

        }

    }

    handleChangeForApps (e) {
        const { user, match, history, dispatch, nctData } = this.props;
        if (e && e.label) {
            console.log('label is: ', e.label);
            dispatch(cubeActions.setSelectedApp(e.label));
            dispatch(cubeActions.getTestIds(e.label));
            dispatch(cubeActions.setSelectedTestId(''));
        }
    }

    replay () {
        const {cube, dispatch} = this.props;
        if (!cube.selectedTestId) {
            alert('select collection to replay');
        } else {
            this.setState({show: true});
            dispatch(cubeActions.startReplay(cube.selectedTestId, cube.replayId.replayid));
            this.doAnalysis = true;
            this.statusInterval = setInterval(checkStatus, 1500);
        }

        function checkStatus() {
            dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid));
        }
    }

    getReplayStatus() {
        const {cube, dispatch} = this.props;
        dispatch(cubeActions.getReplayStatus(cube.selectedTestId, cube.replayId.replayid));
    }

    renderAppsList ( cube ) {
        let options = [];
        if (cube.appsListReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.appsList.map(app => ({ label: app, value: app }));
        }
        let jsxContent = '';
        if (options.length) {
            let selectedAppObj = ''
            if (cube.selectedApp)
                selectedAppObj = { label: cube.selectedApp, value: cube.selectedApp};
            jsxContent = <div key={cube.selectedApp}>
                            <Select options={options} onChange={this.handleChangeForApps} isSearchable={true} defaultValue={selectedAppObj} placeholder={'Select...'}/>
                         </div>
        }
        if (cube.appsListReqStatus == cubeConstants.REQ_LOADING)
        jsxContent = <div><br/>Loading...</div>
        if (cube.appsListReqStatus == cubeConstants.REQ_FAILURE)
        jsxContent = <div><br/>Request failed!</div>

        return <Row>
                <Col md={2} sm={2} xs={2}>
                <br/>
                Test Config:
                </Col>
                <Col md={4} sm={4} xs={4}>
                    <div> 
                        { jsxContent }
                    </div> 
                </Col>
            </Row>
    }

    handleChangeForTestIds (e) {
        const { user, match, history, dispatch, cube } = this.props;
        if (e && e.label) {
            console.log('test-id label is: ', e.label);
            dispatch(cubeActions.setSelectedTestId(e.label));
            dispatch(cubeActions.getGraphData());
            dispatch(cubeActions.getReplayId(e.label));
            /*if (e.label == cubeConstants.CREATE_NEW) {
                this.setState({testIdPrefix: cube.selectedApp.replace(' ', '-')})
            }*/
        }
    } 


    renderTestIds ( cube ) {
        if (cube.testIdsReqStatus != cubeConstants.REQ_SUCCESS)
            return '';
        let options = [];
        if (cube.testIdsReqStatus == cubeConstants.REQ_SUCCESS) {
            options = cube.testIds.map(item => ({ label: item.collection, value: item.collection }));
        }
        // options.unshift({label: cubeConstants.CREATE_NEW, value: cubeConstants.CREATE_NEW});
        let jsxContent = '';
        if (options.length) {
            let selectedTestIdObj = ''
            if (cube.selectedTestId)
                selectedTestIdObj = { label: cube.selectedTestId, value: cube.selectedTestId};
            jsxContent = <div key={cube.selectedTestId}>
                            <Select options={options} onChange={this.handleChangeForTestIds} isSearchable={true} defaultValue={selectedTestIdObj} placeholder={'Select...'}/>
                         </div>
        }
        if (cube.testIdsReqStatus == cubeConstants.REQ_LOADING)
            jsxContent = <div><br/>Loading...</div>
        if (cube.testIdsReqStatus == cubeConstants.REQ_FAILURE)
            jsxContent = <div><br/>Request failed!</div>

        return <Row>
                    <Col md={2} sm={2} xs={2}>
                    <br/>
                    Collection:
                    </Col>
                    <Col md={4} sm={4} xs={4}>
                        <div> 
                            { jsxContent }
                        </div> 
                    </Col>
                    <Col md={4} sm={4}>
                        <button onClick={this.replay}>Replay</button>&nbsp;
                        <button onClick={this.getReplayStatus}>Replay Status</button>&nbsp;
                    </Col>
                </Row>
    }

    /*handleTestIdPrefixChange (e) {
        this.setState({ testIdPrefix: e.target.value });
    }

    handleTestIdPrefixSubmit (e) {
        e.preventDefault();
        console.log(`Click on create: ${this.state.testIdPrefix}`);
    }

    renderCreateNewTestIdForm ( cube ) {
        if (cube.selectedTestId != cubeConstants.CREATE_NEW)
            return '';
        let jsxContent = '';
        let placeHolder = cube.selectedApp.replace(' ', '-');
        jsxContent = <Form inline>
                        <FormGroup controlId="testIdPrefix">
                            <FormControl type="text" name="testIdPrefix" placeholder={placeHolder} value={this.state.testIdPrefix} onChange={this.handleTestIdPrefixChange}/>
                        </FormGroup>{' '}
                        <Button type="submit" onClick={this.handleTestIdPrefixSubmit}>Create</Button>
                    </Form>;

        return <Row>
                    <Col md={2} sm={2} xs={2}>
                    <br/>
                    Test Id prefix:
                    </Col>
                    <Col md={8} sm={8} xs={8}>
                        <div> 
                            { jsxContent }
                        </div> 
                    </Col>
                </Row>
    }*/

    render () {
    const { panelVisible } = this.state
    const onHide = e => this.setState({panelVisible: !panelVisible})
    const { user, cube } = this.props;
    if (this.state.toAnalysis === true) {
        <Redirect to='/analysis' push />
    }

    return (
        <div>
            {/*<PageTitle title="Test configuration" />*/}
            {/*<Clearfix />
            { this.renderAppsList (cube) }*/}
            <Clearfix />
            { this.renderTestIds (cube) }

            {/*{ this.renderCreateNewTestIdForm (cube) }*/}
            <Modal show={this.state.show}>
                <Modal.Header closeButton>
                    <Modal.Title>Replay</Modal.Title>
                </Modal.Header>
                <Modal.Body>Replay In Progress...</Modal.Body>
                <Modal.Footer>

                </Modal.Footer>
            </Modal>
        </div>
    )
    }
}




function mapStateToProps(state) {
  const { user } = state.authentication;
  const cube = state.cube;
  return {
    user,
    cube
  }
}

const connectedConfigSample = connect(mapStateToProps)(configSample);

export default connectedConfigSample