/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { withRouter } from "react-router-dom";
import CytoscapeReactWrapper from '../../components/Cytoscape/CytoscapeReactWrapper';
import GoldenVisibility from '../../components/Golden-Visibility/GoldenVisibility';
import TestClusterStatus from '../../components/Test-Cluster/TestClusterStatus';
import {cubeActions} from "../../actions";
import "./ServiceGraph.css";

class ServiceGraph extends Component {
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
        if(testConfig && testConfig.gatewayServiceName) {
            gatewayServices.push(testConfig.gatewayServiceName);
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
                    target: dp.toService.id,
                    testConfig: (gatewayServices.indexOf(dp.fromService.name) > -1 && mockServices.indexOf(dp.toService.name) > -1) ? "testDirection": ""
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

    renderServiceGraph = () => {
        const { cube } = this.props;
        const graphData = this.getGD(cube);

        return (
            <div className="content-wrapper">                    
                <h4>Service Graph</h4>
                <CytoscapeReactWrapper graphData={graphData}/>
            </div>
        );
    }

    render () {
        const { cube } = this.props;
        return (
            <div>
            {
                !cube.hideGoldenVisibilityView ?
                <GoldenVisibility /> 
                : this.renderServiceGraph()
            }
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    user: state.authentication.user,
    cube: state.cube
});

export default withRouter(connect(mapStateToProps)(ServiceGraph));