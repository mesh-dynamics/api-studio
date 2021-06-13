/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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