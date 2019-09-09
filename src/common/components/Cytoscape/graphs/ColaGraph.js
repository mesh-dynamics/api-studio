export class ColaGraph {
    static getLayout() {
        return {
            /* name: 'cola',
            animate: false,
            fit: false,
            flow: { axis: 'x' },
            nodeDimensionsIncludeLabels: true,
            randomize: false */

            name: 'cola',
            animate: false,
            fit: true,
            flow: { axis: 'y' },
            nodeDimensionsIncludeLabels: true,
            randomize: false
        };
    }
}
