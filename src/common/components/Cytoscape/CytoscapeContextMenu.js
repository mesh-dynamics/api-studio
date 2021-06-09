import * as React from 'react';
import * as ReactDOM from 'react-dom';
import tippy from 'tippy.js';
import 'tippy.js/themes/light-border.css';

import NodeContextMenu from './ContextMenu/NodeContextMenu';

export class CytoscapeContextMenu extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.handleDocumentMouseUp = (event) => {
            if (event.button === 2) {
                // Ignore mouseup of right button
                return;
            }
            const currentContextMenu = this.getCurrentContextMenu();
            if (currentContextMenu) {
                // Allow interaction in our popper component (Selecting and copying) without it disappearing
                if (event.target && currentContextMenu.popper.contains(event.target)) {
                    return;
                }
                currentContextMenu.hide();
            }
        };
        this.handleContextMenu = (event) => {
            // Disable the context menu in popper
            const currentContextMenu = this.getCurrentContextMenu();
            if (currentContextMenu) {
                if (event.target && currentContextMenu.popper.contains(event.target)) {
                    event.preventDefault();
                }
            }
            return true;
        };
        this.contextMenuRef = React.createRef();
    }

    componentDidMount() {
        document.addEventListener('mouseup', this.handleDocumentMouseUp);
    }

    componentWillUnmount() {
        document.removeEventListener('mouseup', this.handleDocumentMouseUp);
    }

    // Connects cy to this component
    connectCy(cy) {
        cy.on('cxttapstart taphold', (event) => {
            event.preventDefault();
            if (event.target) {
                const currentContextMenu = this.getCurrentContextMenu();
                if (currentContextMenu) {
                    currentContextMenu.hide(0); // hide it in 0ms
                }
                let contextMenuComponentType;
                if (event.target === cy) {
                    contextMenuComponentType = undefined;
                }
                else if (event.target.isNode() && event.target.isParent()) {
                    contextMenuComponentType = this.props.groupContextMenuContent;
                }
                else if (event.target.isNode()) {
                    contextMenuComponentType = this.props.nodeContextMenuContent;
                }
                else if (event.target.isEdge()) {
                    contextMenuComponentType = this.props.edgeContextMenuContent;
                }
                if (contextMenuComponentType) {
                    this.props.cytoscapeReactWrapperRef.current.selectTarget(event.target);
                    this.makeContextMenu(contextMenuComponentType, event.target);
                }
            }
            return false;
        });
    }

    render() {
        return (
            <div className="hidden">
                <div ref={this.contextMenuRef} />
            </div>
        );
    }

    getCurrentContextMenu() {
        return this.contextMenuRef.current._contextMenu;
    }

    setCurrentContextMenu(current) {
        this.contextMenuRef.current._contextMenu = current;
    }

    tippyDistance(target) {
        if(target.isParent()) {
            return 20;
        }
        if (target.isNode === undefined || target.isNode()) {
            return 10;
        }
        if (target.isEdge) {
            return 5;
        }
        return -30;
    }

    addContextMenuEventListener() {
        document.addEventListener('contextmenu', this.handleContextMenu);
    }

    removeContextMenuEventListener() {
        document.removeEventListener('contextmenu', this.handleContextMenu);
    }

    makeContextMenu(ContextMenuComponentClass, target) {
        // Prevent the tippy content from picking up the right-click when we are moving it over to the edge/node
        this.addContextMenuEventListener();
        const content = this.contextMenuRef.current;
        const tippyInstance = tippy(target.popperRef(), {
            content: content,
            trigger: 'manual',
            arrow: true,
            placement: 'right',
            hideOnClick: false,
            multiple: false,
            sticky: true,
            interactive: true,
            theme: 'light-border',
            size: 'large',
            distance: this.tippyDistance(target)
        });
        const result = (
            <NodeContextMenu element={target} contextMenu={tippyInstance} {...target.data()} />
        );
        ReactDOM.render(result, content, () => {
            this.setCurrentContextMenu(tippyInstance);
            tippyInstance.show();
            // Schedule the removal of the contextmenu listener after finishing with the show procedure, so we can
            // interact with the popper content e.g. select and copy (with right click) values from it.
            setTimeout(() => {
                this.removeContextMenuEventListener();
            }, 0);
        });
    }
}

export default CytoscapeContextMenu;