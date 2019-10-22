import * as React from 'react';

import cytoscape from 'cytoscape';
import canvas from 'cytoscape-canvas';
import cycola from 'cytoscape-cola';
import dagre from 'cytoscape-dagre';
import klay from 'cytoscape-klay';
import coseBilkent from 'cytoscape-cose-bilkent';
import popper from 'cytoscape-popper';

import CytoscapeContextMenu from './CytoscapeContextMenu';
import NodeContextMenu from './ContextMenu/NodeContextMenu';
import CytoscapeToolbar from './CytoscapeToolbar';
import {connect} from "react-redux";
import {cubeConstants} from "../../constants";
import {cubeActions} from "../../actions";

cytoscape.use(canvas);
cytoscape.use(cycola);
cytoscape.use(dagre);
cytoscape.use(coseBilkent);
cytoscape.use(popper);
cytoscape.use(klay);

//cytoscape('layout', 'group-compound-layout', GroupCompoundLayout);

/**
 * The purpose of this wrapper is very simple and minimal - to provide a long-lived <div> element that can be used
 * as the parent container for the cy graph (cy.container). Because cy does not provide the ability to re-parent an
 * existing graph (e.g. there is no API such as "cy.setContainer(div)"), the only way to be able to re-use a
 * graph (without re-creating and re-rendering it all the time) is to have it inside a wrapper like this one
 * that does not update/re-render itself, thus keeping the original <div> intact.
 *
 * Other than creating and initializing the cy graph, this component should do nothing else. Parent components
 * should get a ref to this component can call getCy() in order to perform additional processing on the graph.
 * It is the job of the parent component to manipulate and update the cy graph during runtime.
 *
 * NOTE: The context menu stuff is defined in the CytoscapeReactWrapper because that is
 * where the cytoscape plugins are defined. And the context menu functions are defined in
 * here because they are not normal Cytoscape defined functions like those found in CytoscapeGraph.
 */
export class CytoscapeReactWrapper extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.cy = null;
        this.zoomOptions = {
            fitPadding: 25
        };
        this.cytoscapeReactWrapperRef = React.createRef();
        this.contextMenuRef = React.createRef();
    }
    // For other components to be able to manipulate the cy graph.
    getCy() {
        return this.cy;
    }
    // This is VERY important - this must always return false to ensure the div is never destroyed.
    // If the div is destroyed, the cached cy becomes useless.
    shouldComponentUpdate(_nextProps, _nextState) {
        //TODO make it false and refactor such that cytoscape will not be rebuild but redraw with new elements; like the above comment
        return true;
    }
    componentDidMount() {
        this.build();
    }
    componentDidUpdate() {
        const {cube, dispatch} = this.props;
        if (cube.graphDataReqStatus == cubeConstants.REQ_SUCCESS) {
            dispatch(cubeActions.clear());
            this.build();
        }
    }
    componentWillUnmount() {
        this.destroy();
    }
    render() {
        return (
            <div className="cytoscape-wrapper">
                <CytoscapeContextMenu
                    ref={this.contextMenuRef}
                    edgeContextMenuContent={NodeContextMenu}
                    nodeContextMenuContent={NodeContextMenu}
                    groupContextMenuContent={NodeContextMenu}
                    cytoscapeReactWrapperRef={this.cytoscapeReactWrapperRef}
                />
                <div style={{position: "relative"}} ref={refInstance => this.setCytoscapeGraph(refInstance)}>
                    <div id="cy" className="graph" style={{height: 'calc(100vh - 150px)', padding: '45px'}} /> 
                    <CytoscapeToolbar cytoscapeReactWrapperRef={this.cytoscapeReactWrapperRef} />
                </div>
            </div>
        );
    }
    build() {
        if (this.cy) {
            this.destroy();
        }
        const {graphData} = this.props;
        const opts = Object.assign({ 
            container: document.getElementById('cy'),
            selectable: true,
            padding: 20,
            fitPadding: 25,
            /* zoom: 1, */
            pan: {x: 100, y: 100},
            elements: graphData,
            style: [ // the stylesheet for the graph
                {
                    selector: 'node[tag="service"]',
                    style: {
                        'background-color': '#eee',
                        'shape': 'round-rectangle',
                        'height': '50px',
                        'width': '140px',
                        'label': 'data(name)',
                        "text-valign": "center",
                        "text-halign": "center",
                        'border-style': 'solid',
                        'border-color': '#666',
                        'border-width': '1px'
                    }
                },
                {
                    selector: 'node[tag="realService"]',
                    style: {
                        'background-color': '#fff',
                        'shape': 'round-rectangle',
                        /* 'height': '250px',
                        'width': '250px', */
                        'text-margin-y': "-15px",
                        'label': 'data(name)',
                        'border-style': 'dashed',
                        'border-color': '#666',
                        'border-width': '1px'
                    }
                },
                {
                    selector: 'node[testConfig="gatewayService"]',
                    style: {
                        'background-color': '#FFA500',
                        'line-color': '#FFA500',
                        'target-arrow-color': '#FFA500',
                        'source-arrow-color': '#FFA500',
                        'color': "#fff",
                        'border-color': '#FFA500'
                    }
                },
                {
                    selector: 'node[testConfig="mockService"]',
                    style: {
                        'background-color': '#FFFFCC',
                        'line-color': '#FFFFCC',
                        'target-arrow-color': '#FFFFCC',
                        'source-arrow-color': '#FFFFCC',
                        'border-style': 'dotted',
                        'border-color': '#666',
                        'border-width': '2px'
                    }
                },
                {
                    selector: 'node[tag="store"]',
                    style: {
                        'background-color': '#eee',
                        'shape': 'barrel',
                        'height': '80px',
                        'width': '90px',
                        'text-wrap': 'manual',
                        'label': 'data(name)',
                        "text-valign": "center",
                        "text-halign": "center",
                        'border-style': 'solid',
                        'border-color': '#666',
                        'border-width': '1px'
                    }
                },
                {
                    selector: 'edge',
                    style: {
                        'width': 1,
                        'line-color': '#ccc',
                        'target-arrow-color': '#ccc',
                        'target-arrow-shape': 'vee',
                        'curve-style': 'straight'
                    }
                },
                {
                    selector: ':selected',
                    style: {
                        'background-color': '#FFA500',
                        'line-color': '#FFA500',
                        'target-arrow-color': '#FFA500',
                        'source-arrow-color': '#FFA500',
                        'color': "#fff",
                        'border-color': '#FFA500'
                    }
                },

                {
                    selector: 'edge:selected',
                    style: {
                        'width': 2
                    }
                },
                {
                    selector: 'edge[testConfig="testDirection"]',
                    style: {
                        'background-color': '#85cc00',
                        'line-style': 'dashed',
                        'line-color': '#85cc00',
                        'target-arrow-color': '#85cc00',
                        'source-arrow-color': '#85cc00',
                        'color': "#fff",
                        'border-color': '#85cc00',
                        'width': 2
                    }
                },
            ],
            layout: {
                name: 'cose-bilkent',
                animate: false,
                fit: false,
                nodeDimensionsIncludeLabels: true,
                randomize: true
                
            }
        }, { wheelSensitivity: 0.1 });
        this.cy = cytoscape(opts);
        //this.cy.resize();
        this.cy.fit();
        this.contextMenuRef.current.connectCy(this.cy);
        this.cy.on('tap', (event) => {
            let tapped = event.target;
            const cytoscapeEvent = this.getCytoscapeBaseEvent(event);
            if (cytoscapeEvent) {
                this.handleTap(cytoscapeEvent);
                this.selectTarget(event.target);
            }
        });
        this.cy.on('mouseover', 'node,edge', (evt) => {
            const cytoscapeEvent = this.getCytoscapeBaseEvent(evt);
            if (cytoscapeEvent) {
                this.handleMouseIn(cytoscapeEvent);
            }
        });
        this.cy.on('mouseout', 'node,edge', (evt) => {
            const cytoscapeEvent = this.getCytoscapeBaseEvent(evt);
            if (cytoscapeEvent) {
                this.handleMouseOut(cytoscapeEvent);
            }
        });
        this.cy.on('layoutstop', (_evt) => {
            // Don't allow a large zoom if the graph has a few nodes (nodes would look too big).
            this.safeFit(this.cy);
        });
        this.cy.ready((evt) => {
        });
        this.cy.on('destroy', (evt) => {
        });
    }

    setCytoscapeGraph(cytoscapeGraph) {
        this.cytoscapeReactWrapperRef.current = cytoscapeGraph ? this : null;
    }

    getCytoscapeBaseEvent(event) {
        const target = event.target;
        if (target === this.cy) {
            return { summaryType: 'graph', summaryTarget: this.cy };
        }
        else if (target.isNode()) {
            if (target.data('isGroup')) {
                return { summaryType: 'group', summaryTarget: target };
            }
            else {
                return { summaryType: 'node', summaryTarget: target };
            }
        }
        else if (target.isEdge()) {
            return { summaryType: 'edge', summaryTarget: target };
        }
        else {
            return null;
        }
    }

    safeFit(cy, centerElements) {
        cy.fit(centerElements, this.zoomOptions.fitPadding);
        if (cy.zoom() > 2.5) {
            cy.zoom(2.5);
            cy.center(centerElements);
        }
    }

    /* runLayout = (cy, layout) => {
        const layoutOptions = LayoutDictionary.getLayout(layout);
        if (cy.nodes('$node > node').length > 0) {
            // if there is any parent node, run the group-compound-layout
            cy.layout(Object.assign({}, layoutOptions, { name: 'group-compound-layout', realLayout: layout.name, 
                // Currently we do not support non discrete layouts for the compounds, but this can be supported if needed.
                compoundLayoutOptions: LayoutDictionary.getLayout(DagreGraph.getLayout()) })).run();
        }
        else {
            cy.layout(layoutOptions).run();
        }
    } */

    handleTap(event) {
    }

    handleMouseIn = (event) => {
    }

    handleMouseOut = (event) => {
    }

    selectTarget = (target) => {
        if (!target) {
            target = this.cy;
        }
        /* `this.cy
            .$(':selected')
            .selectify()
            .unselect()
            .unselectify();` */
        /* if (target !== this.cy) {
            target
                .selectify()
                .select()
                .unselectify();
        } */
    }

    destroy() {
        if (this.cy) {
            this.cy.destroy();
            this.cy = null;
        }
    }
}

function mapStateToProps(state) {
    const cube = state.cube;
    return {
        cube
    }
}

const connectedCytoscapeReactWrapper = connect(mapStateToProps)(CytoscapeReactWrapper);

export default connectedCytoscapeReactWrapper;
