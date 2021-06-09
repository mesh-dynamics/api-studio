export class KlayGraph {
    static getLayout() {
        return {
            name: 'klay',
            klay: {
                layoutHierarchy: true,
                nodeLayering: 'LONGEST_PATH',
                nodePlacement: 'LINEAR_SEGMENTS',
                direction: 'DOWN',
                edgeRouting: 'SPLINES',
                compactComponents: true
            }
        };
    }
}
