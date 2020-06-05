import React, { Component } from 'react'
import CytoscapeReactWrapper from '../Cytoscape/CytoscapeReactWrapper';
import { connect } from "react-redux";
import './APICatalog.css';

class APICatalogServiceGraph extends Component {

    constructor(props) {
        super(props);
        this.state = {
            showGraph: true
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

    showHideGraph = () => {
        const { showGraph } = this.state;
        this.setState({showGraph : !showGraph})
    }

    render() {
        const { cube } = this.props;
        const graphData = this.getGD(cube);
        const { showGraph } = this.state;

        return (
            <div className="api-catalog-bordered-bottom" style={{}}>
                <p style={{fontWeight: 300}}>SERVICE GRAPH&nbsp;
                    <span onClick={this.showHideGraph} className="text-center"  style={{width:"fit-content", borderRadius: "50%", cursor: "pointer" }}>
                        {showGraph ? 
                        (<i class="fa fa-chevron-circle-up"/> )
                        :
                        (<i class="fa fa-chevron-circle-down"/>)
                        }
                        
                    </span>
                </p>
                {showGraph && <div style={{display:"flex", flexDirection: "column", margin: "10px"}}>
                    <CytoscapeReactWrapper graphData={graphData}/>
                </div>}
                
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    cube: state.cube,
})

const connectedAPICatalogServiceGraph = connect(mapStateToProps)(APICatalogServiceGraph);
export default connectedAPICatalogServiceGraph;
export { connectedAPICatalogServiceGraph as APICatalogServiceGraph};
