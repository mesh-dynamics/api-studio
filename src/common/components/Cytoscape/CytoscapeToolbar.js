import * as React from 'react';
import { Row, Col, Clearfix, Button, ButtonGroup, Tabs, Tab  } from 'react-bootstrap';
import {getLayoutByName} from './graphs/LayoutDictionary';

/* const cytoscapeToolbarStyle = style({
    padding: '7px 10px',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: PfColors.Black500,
    backgroundColor: PfColors.White
});
const cytoscapeToolbarPadStyle = style({ marginLeft: '10px' }); */
const ZOOM_STEP = 0.2;

export class CytoscapeToolbar extends React.Component {
    constructor(props, context) {
        super(props, context);
        this.zoomIn = () => {
            console.log("zoom in")
            this.zoom(ZOOM_STEP);
        };
        this.zoomOut = () => {
            this.zoom(-ZOOM_STEP);
        };
        this.fit = () => {
            const cy = this.getCy();
            if (cy) {
                this.props.cytoscapeReactWrapperRef.current.safeFit(cy);
            }
        };
        this.layout = (layoutName) => {
            const cy = this.getCy();
            if (cy) {
                this.runLayout(layoutName);
            }
        }
    }

    componentDidUpdate() {
    }

    render() {
        return (
             <div style={{position: "absolute", top: 0}}>
                 <div style={{width: "40px"}}>
                     <div onClick={this.zoomIn} className="link-q bordered" title="Zoom In"><i className="fas fa-search-plus"></i></div>
                     <div onClick={this.zoomOut} className="link-q bordered" title="Zoom Out"><i className="fas fa-search-minus"></i></div>
                     <div onClick={this.fit} className="link-q bordered" title="Fit"><i className="fas fa-arrows-alt"></i></div>
                     <div onClick={this.layout.bind(this, "dagre")} className="link-q bordered" title="Dagre Layout"><i className="fas fa-project-diagram"></i></div>
                     <div onClick={this.layout.bind(this, "cose-bilkent")} className="link-q bordered" title="Cose-Bilkent Layout"><i className="fas fa-share-alt"></i></div>
                     <div onClick={this.layout.bind(this, "klay")} className="link-q bordered" title="Klay Layout"><i className="fas fa-network-wired"></i></div>
                 </div>
             {/*<ButtonGroup aria-label="Basic example">
                <Button variant="secondary" onClick={this.zoomIn}>
                    <span className="icon is-small">
                        <i className="fas fa-search-plus"></i>
                    </span>
                    <span>Zoom In</span>
                </Button>
                <Button variant="secondary" onClick={this.zoomOut}>
                    <span className="icon is-small">
                        <i className="fas fa-search-minus"></i>
                    </span>
                    <span>Zoom out</span>
                </Button>
                <Button variant="secondary" onClick={this.fit}>
                    <span className="icon is-small">
                        <i className="fas fa-compress-arrows-alt"></i>
                    </span>
                    <span>Fit</span>
                </Button>
                <Button variant="secondary" onClick={this.layout.bind(this, "dagre")}>
                    <span className="icon is-small">
                        <i className="fas fa-project-diagram"></i>
                    </span>
                    <span>Dagre Layout</span>
                </Button>
                <Button variant="secondary" onClick={this.layout.bind(this, "cose-bilkent")}>
                    <span className="icon is-small">
                        <i className="fas fa-chart-network"></i>
                    </span>
                    <span>Cose-Bilkent Layout</span>
                </Button>
                <Button variant="secondary" onClick={this.layout.bind(this, "klay")}>
                    <span className="icon is-small">
                        <i className="fas fa-network-wired"></i>
                    </span>
                    <span>Klay Layout</span>
                </Button>
            </ButtonGroup>*/}
            </div>
        );
    }

    getCy() {
        if (this.props.cytoscapeReactWrapperRef.current) {
            return this.props.cytoscapeReactWrapperRef.current.getCy();
        }
        return null;
    }

    zoom(step) {
        const cy = this.getCy();
        if (cy) {
            cy.zoom({
                level: cy.zoom() * (1 + step),
                renderedPosition: {
                    x: cy.container().offsetWidth / 2,
                    y: cy.container().offsetHeight / 2
                }
            });
        }
    }

    runLayout(layoutName) {
        const cy = this.getCy();
        const layoutParams = getLayoutByName(layoutName);
        if (cy) {
            return cy.layout(layoutParams).run();
            /* return cy.layout({name: 'dagre',
            fit: false,
            nodeDimensionsIncludeLabels: true,
            rankDir: 'LR',
            ranker: 'tight-tree'}).run(); */
        }
    }
}

export default CytoscapeToolbar;
