import React, { Component } from 'react';
import { connect } from 'react-redux';
import CytoscapeComponent from 'react-cytoscapejs';
import Modal from "react-bootstrap/es/Modal";
import {Clearfix} from "react-bootstrap";

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
                    'width': '260px',
                    'height': '80px',
                    'font-size': '20px',
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
        const gd = this.getGD();
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
        const gd = this.getGD();
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
        this.setState({ show: true });
    }

    render() {
        
        const $ = window.$;
        const element = $(this.refs.cyto);
        const { cube } = this.props;

        if (Object.keys(this.cy).length) {
            this.renderServiceGraph(this.cy, cube);
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
                </div>
            </div>
        )
    }

    getGD() {
        const { cube } = this.props;
        const gdCrude = JSON.parse(JSON.stringify(cube.graphData));
        for (const key in gdCrude) {
            if (gdCrude[key].app.name === cube.selectedApp) {
                console.log(JSON.parse(gdCrude[key].serviceGraph));
                return JSON.parse(gdCrude[key].serviceGraph);
            }
        }
    }

    renderServiceGraph(cy, cube) {
        // First remove everything
        cy.remove(cy.nodes()); cy.remove(cy.edges());
        const gd = this.getGD();

    
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
        });

        // Create edges
        let style = {
            'source-arrow-shape': 'circle',
            'target-arrow-shape': 'triangle',
            //'curve-style': 'unbundled-bezier',
            'curve-style': 'haystack',
            'width': '2px'
        };

        for (const edge of this.getGD().edges) {
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
