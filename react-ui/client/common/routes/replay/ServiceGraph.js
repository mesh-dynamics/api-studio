import React, { Component } from 'react';
import { connect } from 'react-redux';
import CytoscapeComponent from 'react-cytoscapejs';

class ServiceGraph extends Component {
    constructor(props) {
        super(props)
        this.state = {
            panelVisible: true,
        }
        this.height = '350px';
        this.width = '100%';
        this.renderServiceGraph = this.renderServiceGraph.bind(this);
        this.render = this.render.bind(this);
        this.cy = {};
        
        this.style = [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    shape: 'roundrectangle',
                    content: 'data(text)',
                    "font-size": "12px",
                    "text-valign": "center",
                    "text-halign": "center",
                    "background-color": "#555",
                    "text-outline-color": "#555",
                    "text-outline-width": "2px",
                    "color": "#fff",
                    "border-color": "white",
                    "border-width": "3px",
                    "overlay-padding": "6px",
                    "z-index": "10"
                }
            },
            {
                selector: '$node > node',
                style: {
                    "font-size": "10px",
                    "text-valign": "top",
                    "text-halign": "center",
                    "background-color": "#bbb",
                    "text-outline-color": "#555",
                    "text-outline-width": "0px",
                    "color": "black",
                    "border-color": "black",
                    "border-width": "0px",
                    "overlay-padding": "6px",
                    "z-index": "10"
                }
            }
        ]
        
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

    render() {
        
        const $ = window.$;
        const element = $(this.refs.cyto);
        
        if (Object.keys(this.cy).length) {
            this.renderServiceGraph(this.cy);
            this.focusDivWithoutScroll(element)
        } else {
            setTimeout(this.render, 1);
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
             
 
        return(
            <div>
                <br/>
                <div className="col-sm-12">
                    <div ref='cyto' tabIndex="1">
                        <CytoscapeComponent style={{ width: this.width, height: this.height }} stylesheet={this.style} cy={cy => this.cy = cy} wheelSensitivity='0.25' />
                    </div>
                </div>
            </div>
        )
    }

    renderServiceGraph(cy) {

        // First remove everything
        cy.remove(cy.nodes()); cy.remove(cy.edges());
    
        // Create nodes
        for (let i = 1; i <= 7; i++) {
            let style = { "text-wrap": "wrap", width: 50, height: 50 }
            let eleObj = {
                data: { id: `s${i}.ztc.io`, text: `s${i}.ztc.io`},
                style: style
            };
            cy.add(eleObj);
        }

        // Create edges
        let style = {
            'source-arrow-shape': 'circle',
            'target-arrow-shape': 'triangle',
            // 'curve-style': 'unbundled-bezier'
            'curve-style': 'haystack'
        };
        let eleObj = {
            data: {
                id: 's1_s2',
                source: 's1.ztc.io', target: 's2.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's1_s6',
                source: 's1.ztc.io', target: 's6.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's2_s6',
                source: 's2.ztc.io', target: 's6.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's2_s3',
                source: 's2.ztc.io', target: 's3.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's6_s7',
                source: 's6.ztc.io', target: 's7.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's3_s4',
                source: 's3.ztc.io', target: 's4.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);
        eleObj = {
            data: {
                id: 's3_s5',
                source: 's3.ztc.io', target: 's5.ztc.io'
            },
            style: style
        };
        cy.add(eleObj);

        // Layout
        if ( 1 ) {
            let layout;
            layout = cy.layout({
                name: 'grid', // 'cose'
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

    return {
      user
    }
  }



  const connectedServiceGraph = connect(mapStateToProps)(ServiceGraph);
  export default connectedServiceGraph  