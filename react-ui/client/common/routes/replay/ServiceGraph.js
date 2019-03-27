import React, { Component } from 'react';
import { connect } from 'react-redux';
import CytoscapeComponent from 'react-cytoscapejs';
import ConfigSample from '../config/configSample';
import Modal from "react-bootstrap/es/Modal";
import Button from "react-bootstrap/es/Button";
import {cubeConstants} from "../../constants";
import popper from 'cytoscape-popper';
import {Clearfix, Col, Row} from "react-bootstrap";

class ServiceGraph extends Component {
    constructor(props) {
        super(props);
        this.handleShow = this.handleShow.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.state = {
            panelVisible: true,
            selectedNode: null,
            replaySelected: false,
            show: false,
        }
        this.height = '90vh';
        this.width = '100%';
        this.renderServiceGraph = this.renderServiceGraph.bind(this);
        this.setRP = this.setRP.bind(this);
        this.setVP = this.setVP.bind(this);
        this.render = this.render.bind(this);
        this.cy = {};
        
        this.style = [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    shape: 'roundrectangle',
                    content: 'data(text)',
                    "text-wrap": "wrap",
                    'font-size': '10px',
                    width: 'label',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'background-color': '#c4c4c4',
                    'text-outline-color': '#c4c4c4',
                    'text-outline-width': '2px',
                    'color': '#616161',
                    'border-color': '#c4c4c4',
                    'border-width': '3px',
                    'overlay-padding': '6px',
                    'z-index': '10'
                }
            },
            {
                selector: '$node > node',
                style: {
                    'font-size': '10px',
                    'text-valign': 'top',
                    'text-halign': 'center',
                    'width': 'auto',
                    'height': 'auto',
                    'background-color': '#bbb',
                    'text-outline-color': '#555',
                    'text-outline-width': '0px',
                    'color': 'black',
                    'border-color': 'black',
                    'border-width': '0px',
                    'overlay-padding': '6px',
                    'z-index': '10'
                }
            },
            {
                selector: 'node.selected-node',
                style: {
                    'background-color': '#555', 'color': '#fff', 'text-outline-color': '#555', width: '150px'
                }
            },
            {
                selector: 'node.replay-node',
                style: {
                    'background-color': '#e7f9fe',
                    'color': '#616161',
                    'text-outline-color': '#e7f9fe',
                    width: '150px'
                }
            }
        ]
        
    }

    setRP() {
        if (this.state.replaySelected) {
            return;
        }
        const {cube} = this.props;
        const gd = cube.graphData;
        for (const node of gd.nodes) {
            if (node.data.id == this.state.selectedNode.id) {
                node.data.isReplayPoint = true;
                break;
            }
        }
        this.setState({
            replaySelected: true,
            selectedNode: null,
            show: false
        })
    }

    setVP() {

    }

    componentWillUnmount () {
        if(Object.keys(this.cy).length)
            this.cy.destroy();
    }
    
    focusDivWithoutScroll(div) {
        const $ = window.$;
        let x = window.scrollX, y = window.scrollY;
        $(div).focus();
        window.scrollTo(x, y);
    }

    handleClose() {
        this.setState({ show: false });
    }

    handleShow() {
        this.setState({ show: true });
    }

    render() {
        
        const $ = window.$;
        const element = $(this.refs.cyto);
        const { cube } = this.props;
        
        if (Object.keys(this.cy).length) {
            /*this.cy.destroy();
            this.cy = {};*/
            this.renderServiceGraph(this.cy, cube);
            this.focusDivWithoutScroll(element)
        } else {
            //setTimeout(this.render, 1);
        }

        if(Object.keys(this.cy).length){
            this.cy.panzoom({});
            //const $ = window.$;
            //const element = $(this.refs.cyto);
            let cy = this.cy;
            $(element).bind('keydown', function (event) {
              switch (event.keyCode) {
                //....your actions for the keys .....
                case 40: //down
                  cy.panBy({ x: 0, y: -50 });
                  event.preventDefault();
                  break;
                case 34: //pg-down
                  cy.panBy({ x: 0, y: -250 });
                  event.preventDefault();
                  break;
                case 38: //up
                  cy.panBy({ x: 0, y: 50 });
                  event.preventDefault();
                  break;
                case 33: //pg-up
                  cy.panBy({ x: 0, y: 250 });
                  event.preventDefault();
                  break;
                case 39://right
                  cy.panBy({ x: -50, y: 0 });
                  event.preventDefault();
                  break;
                case 37://left
                  cy.panBy({ x: 50, y: 0 });
                  event.preventDefault();
                  break;
              }
            });        
        }

        let graph = '';
        if (cube.selectedTestId) {
            graph = <div ref='cyto' tabIndex='1'>
                <CytoscapeComponent style={{ width: this.width, height: this.height }} stylesheet={this.style} cy={cy => this.cy = cy} wheelSensitivity='0.05' />
            </div>;
        } else {
            graph = <div className="select-text">Please Select a Collection to Proceed</div>
        }

        /*let analysis = 'No Analysis';
        let report = 'No Report';
        if (cube.analysis) {
            analysis = Object.keys(cube.analysis).map((key, index) => {
                return (<div key={index}> Key: {key}, Value: {cube.analysis[key]}</div>)
            });
        }

        if (cube.report) {
            report = Object.keys(cube.report[1]).map((key, index) => {
                return (<div key={index}> Key: {key}, Value: {cube.report[1][key]}</div>)
            });
        }*/


 
        return(
            <div>
                {/*<br/>
                <ConfigSample />
                <br/>
                <div></div>
                <br/>*/}
                <div className='col-sm-12'>
                    {graph}
                    <Clearfix />
                    <br/>
                    <Modal show={this.state.show} onHide={this.handleClose}>
                        <Modal.Header closeButton>
                            <Modal.Title>Node Details</Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            <h4>Node: {this.state.selectedNode && this.state.selectedNode.text ? this.state.selectedNode.text : ''}</h4>
                            <span className="cube-btn" onClick={this.setRP}>Set Replay Point</span>&nbsp;&nbsp;
                            <span className="cube-btn" onClick={this.setVP}>Set Virtualization Point</span>
                        </Modal.Body>
                        <Modal.Footer>
                            {/*<Button variant="secondary" onClick={this.handleClose}>
                                Close
                            </Button>
                            <Button variant="primary" onClick={this.handleClose}>
                                Save Changes
                            </Button>*/}
                        </Modal.Footer>
                    </Modal>
                    {/*<div style={{padding: '15px', border: '1px solid gray'}}>
                        <Row>
                            <Col md={6} sm={6} xs={6}>
                                <div style={{overflow: 'hidden'}}>
                                    {analysis}
                                </div>
                            </Col>
                            <Col md={6} sm={6} xs={6}>
                                <div style={{overflow: 'hidden'}}>
                                    {report}
                                </div>
                            </Col>
                        </Row>
                    </div>
                    <Clearfix />*/}
                </div>
            </div>
        )
    }

    renderServiceGraph(cy, cube) {
        if (cube.graphDataReqStatus != cubeConstants.REQ_SUCCESS) {
            return '';
        }
        // First remove everything
        cy.remove(cy.nodes()); cy.remove(cy.edges());
        //const arr = [];

        const gd = JSON.parse(JSON.stringify(cube.graphData));
    
        // Create nodes
        let styleN = { "text-wrap": "wrap", width: 150, height: 50 };
        for (const node of gd.nodes) {
            if (cube.analysis && node.data.id == 'movieinfo') {
                let an = cube.analysis;
                node.data.text += ('\n\n' + an.reqcnt + ' / ' + an.reqmatched + ' / ' + an.respmatched + ' / ' + an.respnotmatched);
                node.data.class = 'selected-node';
                node.style = styleN;
            }

            if (node.data.isReplayPoint) {
                node.classes = 'replay-node';
            }
            cy.add(node);
        }

        if (cube.analysis) {
            let an = cube.analysis;
            let node = cy.nodes().first();
            node.addClass('selected-node');
            node.data.text += (an.reqcnt)
        }

        const _this = this;

        cy.removeListener('click', 'node').on('click', 'node', function(evt){
            evt.preventDefault();
            _this.handleShow();
            var node = evt.target;
            console.log(node.data());
            _this.setState({
                selectedNode: node.data(),
                show: true
            });
            /*console.log(node);
            cy.$(node).addClass('selected-node');*/
        });

        // Create edges
        let style = {
            'source-arrow-shape': 'circle',
            'target-arrow-shape': 'triangle',
            //'curve-style': 'unbundled-bezier',
            'curve-style': 'haystack',
            'width': '2px'
        };

        for (const edge of cube.graphData.edges) {
            const ed = {
                data: edge,
                style: style
            };
            cy.add(ed);
        }


        // Layout
        if ( 1 ) {
            let layout;
            layout = cy.layout({
                name: 'circle', // 'cose'
                fit: true,
                idealEdgeLength: function (edge) { return 100; },
                edgeElasticity: function (edge) { return 100; },
            });
            layout.run();
        } else {
            let computedPositions = [];
            let layout = cy.layout({
                name: 'preset',
                fit: true,
                positions: computedPositions
            });
            layout.run();
        }
        return true;
    }

   
}

function mapStateToProps(state) {
    const { user } = state.authentication;
    const cube = state.cube;
    return {
      user, cube
    }
  }



  const connectedServiceGraph = connect(mapStateToProps)(ServiceGraph);
  export default connectedServiceGraph  
