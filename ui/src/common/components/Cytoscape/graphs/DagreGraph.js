export class DagreGraph {
    static getLayout() {
        return {
            /* name: 'dagre',
            fit: false,
            nodeDimensionsIncludeLabels: true,
            rankDir: 'LR' */

            name: 'dagre',
            fit: false,
            nodeDimensionsIncludeLabels: true,
            rankDir: 'LR',
            ranker: 'tight-tree'
        };
    }
}
