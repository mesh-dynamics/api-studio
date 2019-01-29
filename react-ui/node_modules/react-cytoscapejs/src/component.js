import React from 'react';
import ReactDOM from 'react-dom';
import { types } from './types';
import { defaults } from './defaults';
import Cytoscape from 'cytoscape';
import { patch } from './patch';
var panzoom = require('cytoscape-panzoom');
panzoom(cytoscape); // register extension

/**
 * The `CytoscapeComponent` is a React component that allows for the declarative creation
 * and modification of a Cytoscape instance, a graph visualisation.
 */
export default class CytoscapeComponent extends React.Component {
  static get propTypes() {
    return types;
  }

  static get defaultProps() {
    return defaults;
  }

  static normalizeElements(elements) {
    const isArray = elements.length != null;

    if (isArray) {
      return elements;
    } else {
      let { nodes, edges } = elements;

      if (nodes == null) {
        nodes = [];
      }

      if (edges == null) {
        edges = [];
      }

      return nodes.concat(edges);
    }
  }

  constructor(props) {
    super(props);
  }

  componentDidMount() {
    console.log('I AM HERE');
    const $ = window.$;
    const element = $(this.refs.div);
    const container = ReactDOM.findDOMNode(this);
    const { global } = this.props;
    const cy = (this._cy = new Cytoscape({ container }));

    if (global) {
      window[global] = cy;
    }
    cy.panzoom({});
    
    
    $(element).bind('keydown', function (event) {
      console.log(`${Date()}:`, event.keyCode);
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
    
    this.updateCytoscape(null, this.props);
  }

  updateCytoscape(prevProps, newProps) {
    const cy = this._cy;
    const { diff, toJson, get, forEach } = newProps;

    // batch for peformance
    cy.batch(() => {
      patch(cy, prevProps, newProps, diff, toJson, get, forEach);
    });

    if (newProps.cy != null) {
      newProps.cy(cy);
    }
  }

  componentDidUpdate(prevProps) {
    this.updateCytoscape(prevProps, this.props);
  }

  componentWillUnmount() {
    this._cy.destroy();
  }

  render() {
    const { id, className, style } = this.props;

    return React.createElement('div', {
      id,
      className,
      style
    });
  }
}
