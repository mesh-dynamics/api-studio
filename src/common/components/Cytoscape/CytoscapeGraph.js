import * as React from 'react';

import CytoscapeReactWrapper from './CytoscapeReactWrapper';


export class CystoscapeGraph extends React.Component {
    constructor(props) {
        super(props);
        this.cytoscapeReactWrapperRef = React.createRef();
    }

    render() {
        return (
          <div id="cytoscape-container">
            <div>
              <CytoscapeReactWrapper ref={e => this.setCytoscapeReactWrapperRef(e)} />
            </div>
          </div>
        );
    }

    setCytoscapeReactWrapperRef(cyRef) {
        this.cytoscapeReactWrapperRef.current = cyRef;
    }
}


export default CystoscapeGraph;