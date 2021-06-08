import { ColaGraph } from './ColaGraph';
import { CoseGraph } from './CoseGraph';
import { DagreGraph } from './DagreGraph';
import { KlayGraph } from './KlayGraph';
const LayoutMap = {
    'cola': ColaGraph.getLayout(),
    'dagre': DagreGraph.getLayout(),
    'cose-bilkent': CoseGraph.getLayout(),
    'klay': KlayGraph.getLayout()
};
const getLayout = (layout) => LayoutMap.hasOwnProperty(layout.name) ? LayoutMap[layout.name] : LayoutMap.dagre;
const getLayoutByName = (layoutName) => LayoutMap.hasOwnProperty(layoutName) ? LayoutMap[layoutName] : LayoutMap.dagre;
export { getLayout, getLayoutByName };
