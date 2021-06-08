import * as React from 'react';

export class NodeContextMenu extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.onClick = (_e) => {
            console.log('hi');
        };
    }

    createMenuItem(menuItem) {
        return (
            <div>
                <a href="javascript:void(0);" onClick={this.onClick}>
                    {menuItem}
                </a>
            </div>
        );
    }

    render() {
    
        return (
          <div className="">
            <div className="">
              <strong>Select Replay Mode</strong>
            </div>
            <div className="dropdown-menu" id="dropdown-menu2" role="menu" style={{display: 'block', position: 'relative'}}>
                <div className="dropdown-content" style={{'boxShadow': 'none', textAlign: 'left'}}>
                    <div className="dropdown-item">
                        {/* <p>You can insert <strong>any type of content</strong> within the dropdown menu.</p> */}
                        <a className="is-light"><p> <code>Gateway Point</code></p></a>
                        {/* {this.createMenuItem('Select this as Gateway point')} */}
                    </div>
                    <hr className="dropdown-divider" />
                    <div className="dropdown-item">
                        {/* <p>You simply need to use a <code>&lt;div&gt;</code> instead.</p> */}
                        <a className="is-light"><p> <code>Intermediate Point</code></p></a>
                    </div>
                    <hr className="dropdown-divider" />
                    {/* <a href="#" className="dropdown-item">
                        Select this as a Gateway point
                    </a> */}
                    <div className="dropdown-item">
                        {/* <p>You simply need to use a <code>&lt;div&gt;</code> instead.</p> */}
                        <a className="is-light"><p> <code>Virtualized Point</code></p></a>
                    </div>
                </div>
            </div>
          </div>
        );
    }
}

export default NodeContextMenu;