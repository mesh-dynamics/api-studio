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
            replayNode: null,
            show: false,
            showDiff: false
        };
        this.height = '70vh';
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
                    'width': '230px',
                    'height': '70px',
                    'font-family': 'Roboto Condensed',
                    'font-size': '15px',
                    //width: 'label',
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'background-color': '#c4c4c4',
                    'text-outline-color': '#c4c4c4',
                    'text-outline-width': '2px',
                    'color': '#616161',
                    'z-index': '10',
                    'border-color': '#8F8E8E',
                    'border-width': '2px'
                }
            },
            {
                selector: '$node > node',
                style: {
                    'text-valign': 'center',
                    'text-halign': 'center',
                    'background-color': '#bbb',
                    'text-outline-color': '#555',
                    'text-outline-width': '0px',
                    'color': 'black',
                    //'overlay-padding': '6px',
                    'z-index': '10'
                }
            },
            {
                selector: 'node.selected-node',
                style: {
                    'background-color': '#e7f9fe',
                    'color': '#616161',
                    'text-outline-color': '#e7f9fe',
                }
            },
            {
                selector: 'node.replay-node',
                style: {
                    'border-color': '#DC143C',
                    'border-width': '2px'
                }
            },
            {
                selector: 'node.virtual-node',
                style: {
                    'border-color': '#39B200',
                    'border-width': '2px',
                    'border-style': 'dashed'
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
            replayNode: JSON.parse(JSON.stringify(this.state.selectedNode)),
            selectedNode: null,
            show: false
        })
    }

    setVP() {
        const {cube} = this.props;
        const gd = cube.graphData;
        for (const node of gd.nodes) {
            if (node.data.id == this.state.selectedNode.id) {
                node.data.isVirtualised = true;
                break;
            }
        }
        this.setState({
            selectedNode: null,
            show: false
        })
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
        this.setState({ show: false, showDiff: false });
    }

    handleShow() {
        const { cube } = this.props;
        if (cube.analysis) {
            this.setState({ showDiff: true });
        } else {
            this.setState({ show: true });
        }

    }

    render() {
        
        const $ = window.$;
        const element = $(this.refs.cyto);
        const { cube } = this.props;
        const recordedResponse = [{"actors_lastnames":["HARRIS","WILLIS","TEMPLE"],"display_actors":["DAN HARRIS","HUMPHREY WILLIS","BURT TEMPLE"],"film_id":851,"title":"STRAIGHT HOURS","actors_firstnames":["DAN","HUMPHREY","BURT"],"film_counts":[28,26,23],"timestamp":1641491700530174,"book_info":{"reviews":[{"reviewer":"Reviewer1","text":"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!"},{"reviewer":"Reviewer2","text":"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare."}],"id":"851"}}];
        const replayRes1 = [{"display_actors":["HARRIS,DAN","WILLIS,HUMPHREY","TEMPLE,BURT"],"film_id":851,"title":"STRAIGHT HOURS","actors_firstnames":["DAN","BURT","HUMPHREY"],"film_counts":["28","23","26"],"timestamp":27523407007561}];
        const diff1 = 'DIFF :: {"op":"remove","path":"/body/0/actors_lastnames","value":["HARRIS","WILLIS","TEMPLE"],"resolution":"OK_Optional"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/display_actors/0","value":"HARRIS,DAN","fromValue":"DAN HARRIS","resolution":"OK_OptionalMismatch"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/display_actors/1","value":"WILLIS,HUMPHREY","fromValue":"HUMPHREY WILLIS","resolution":"OK_OptionalMismatch"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/display_actors/2","value":"TEMPLE,BURT","fromValue":"BURT TEMPLE","resolution":"OK_OptionalMismatch"}\n' +
            'DIFF :: {"op":"add","path":"/body/0/actors_firstnames/1","value":"BURT","resolution":"OK_OtherValInvalid"}\n' +
            'DIFF :: {"op":"remove","path":"/body/0/actors_firstnames/3","value":"BURT","resolution":"OK"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/film_counts/0","value":"28","fromValue":28,"resolution":"ERR_ValTypeMismatch"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/film_counts/1","value":"23","fromValue":26,"resolution":"ERR_ValTypeMismatch"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/film_counts/2","value":"26","fromValue":23,"resolution":"ERR_ValTypeMismatch"}\n' +
            'DIFF :: {"op":"replace","path":"/body/0/timestamp","value":27523407007561,"fromValue":1641491700530174,"resolution":"OK_OptionalMismatch"}\n' +
            'DIFF :: {"op":"remove","path":"/body/0/book_info","value":{"reviews":[{"reviewer":"Reviewer1","text":"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!"},{"reviewer":"Reviewer2","text":"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare."}],"id":"851"},"resolution":"OK"}';

        var textedJson = JSON.stringify(recordedResponse, undefined, 4);
        var textedJson1 = JSON.stringify(replayRes1, undefined, 4);

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
                            <Modal.Title>{this.state.selectedNode && this.state.selectedNode.text ? this.state.selectedNode.text : ''}</Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            <div className="text-center">
                                <span className={"cube-btn " + (this.state.replaySelected ? 'disabled' : '')} onClick={this.setRP}>Set Gateway Point</span><br/><br/>
                                <span className="cube-btn" onClick={this.setVP}>Set Virtualization Point</span>
                            </div>
                        </Modal.Body>
                    </Modal>

                    <Modal show={this.state.showDiff} onHide={this.handleClose}>
                        <Modal.Header closeButton>
                            <Modal.Title>Response Diff</Modal.Title>
                        </Modal.Header>
                        <Modal.Body>
                            <div className="left-json">
                                <h4>Recorded</h4>
                                <textarea name="" id="myTextarea" cols="30" rows="10">
                                    {textedJson}
                                </textarea>
                            </div>
                            <div className="right-json">
                                <h4>Replay</h4>
                                <textarea name="" id="myTextarea" cols="30" rows="10">
                                    {textedJson1}
                                </textarea>
                            </div>
                            <div className="diff-json">
                                <pre>
                                    {diff1}
                                </pre>
                            </div>
                        </Modal.Body>
                    </Modal>
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
        for (const node of gd.nodes) {
            if (cube.analysis && node.data.id == 'movieinfo') {
                let an = cube.analysis;
                node.data.text += ('\n\n' + an.reqcnt + ' / ' + (an.respmatched + an.resppartiallymatched) + ' / ' + an.respnotmatched + ' / ' +
                    (an.reqcnt - (an.respmatched + an.resppartiallymatched + an.respnotmatched)));
                node.data.class = 'selected-node';
            }

            if (node.data.isReplayPoint) {
                node.classes = 'replay-node';
            } else if (node.data.isVirtualised) {
                node.classes = 'virtual-node';
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
        cy.userZoomingEnabled(false);

        cy.removeListener('click', 'node').on('click', 'node', function(evt){
            evt.preventDefault();
            var node = evt.target;
            node.addClass('selected-node');
            _this.setState({
                selectedNode: node.data(),
                show: true
            });
            _this.handleShow();
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
