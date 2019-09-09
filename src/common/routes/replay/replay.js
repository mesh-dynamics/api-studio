import React, { Component } from 'react';
import { connect } from 'react-redux';
import CytoscapeReactWrapper from '../../components/Cytoscape/CytoscapeReactWrapper';
import "./ReplayAttribute.css";
import {cubeActions} from "../../actions";

class replay extends Component {
    constructor (props) {
        super(props)
        this.state = {
            panelVisible: true,
        };
        this.pieRef = React.createRef();
        this.handleChangeNeDropDown = this.handleChangeNeDropDown.bind(this);
    }

    componentWillReceiveProps(nextProps, nextContext) {
        //this.setState({});
    }

    componentDidMount () {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideServiceGraph(true));
    }


    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideServiceGraph(false));
    }

    handleChangeNeDropDown(e){
        const { user, match, history, dispatch, nctData } = this.props;
        if (e && e.label) {
            let neIp = e.label.props.children[0];
            history.push(`${match.url}/${neIp}`);
        }
    }

    findInNodes(nodeList, id) {
        for (const node of nodeList) {
            if (node.data.id == id) {
                return true;
            }
        }
        return false;
    }

    getGD(cube) {
        const gdCrude = cube.graphData,
            testConfig = cube.testConfig;
        let gatewayServices = [], mockServices = [];
        if(testConfig && testConfig.gatewayService) {
            gatewayServices.push(testConfig.gatewayService.name);
        }
        if(testConfig && testConfig.mocks) {
            mockServices = testConfig.mocks;
        }
        if (!gdCrude) {
            return null;
        }
        let nodes = [];
        let edges = [];
        for (const dp of gdCrude) {
            if (dp.fromService.serviceGroup && !this.findInNodes(nodes, dp.fromService.serviceGroup.id)) {
                if (dp.fromService.serviceGroup.name != 'GLOBAL') {
                    let node = {data: {id: dp.fromService.serviceGroup.id, name: dp.fromService.serviceGroup.name}};
                    node.data.tag = "realService";
                    if(gatewayServices.indexOf(node.data.name) > -1) node.data.testConfig = "gatewayService";
                    if(mockServices.indexOf(node.data.name) > -1) node.data.testConfig = "mockService";
                    nodes.push(node);
                } 
            }
            if (dp.fromService && !this.findInNodes(nodes, dp.fromService.id)) {
                let node = {data: {id: dp.fromService.id, name: dp.fromService.name}};
                if (dp.fromService.serviceGroup && dp.fromService.serviceGroup.name != 'GLOBAL') {
                    node.data.parent = dp.fromService.serviceGroup.id;
                    node.data.tag = "service";
                } else {
                    node.data.tag = "realService";
                }
                if(gatewayServices.indexOf(node.data.name) > -1) node.data.testConfig = "gatewayService";
                if(mockServices.indexOf(node.data.name) > -1) node.data.testConfig = "mockService";
                nodes.push(node);
            }

            if (dp.toService && !this.findInNodes(nodes, dp.toService.id)) {
                let node = {data: {id: dp.toService.id, name: dp.toService.name}};
                if (dp.toService.serviceGroup && dp.toService.serviceGroup.name != 'GLOBAL') {
                    node.data.parent = dp.toService.serviceGroup.id;
                    node.data.tag = "service";
                } else {
                    node.data.tag = "realService";
                }
                if(gatewayServices.indexOf(node.data.name) > -1) node.data.testConfig = "gatewayService";
                if(mockServices.indexOf(node.data.name) > -1) node.data.testConfig = "mockService";
                nodes.push(node);
            }

            if (dp.fromService && dp.toService) {
                edges.push({data: {
                    id: dp.fromService.id + '_' + dp.toService.id,
                    name: '',
                    source: dp.fromService.id,
                    target: dp.toService.id
                }});
            }
        }
        let parents = [];
        for(let eachElement of nodes) {
            if(eachElement.data.parent && parents.indexOf(eachElement.data.parent) < 0) parents.push(eachElement.data.parent);
        }
        for(let eachElement of nodes) {
            if(!eachElement.data.parent && parents.indexOf(eachElement.data.id) < 0) eachElement.data.tag = 'service';
        }
        if (nodes.length > 0) {
            return nodes.concat(edges);
        } else {
            return null;
        }
    }

    render () {
        const { cube } = this.props;
        const graphData = this.getGD(cube);
        return (
            <div>
                <div className="content-wrapper">
                    <h4>Service Graph</h4>
                    <CytoscapeReactWrapper graphData={graphData}/>
                </div>

            </div>
        )
    }
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
        user, cube
    }
}

const connectedReplay = connect(mapStateToProps)(replay);

export default connectedReplay
