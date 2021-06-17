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

import React, {PureComponent} from 'react';
import {VelocityComponent} from 'velocity-react';

export interface ITreeNodeContainerProps{
    style: any,
    decorators: any,
    terminal: boolean,
    onClick: ()=>void,
    animations: boolean | any,
    node: any
}
class TreeNodeContainer extends PureComponent<ITreeNodeContainerProps> {
    renderToggle() {
        const {animations} = this.props;

        if (!animations) {
            return this.renderToggleDecorator();
        }

        return (
            <VelocityComponent
                animation={animations.toggle.animation}
                duration={animations.toggle.duration}
            >
                {this.renderToggleDecorator()}
            </VelocityComponent>
        );
    }

    renderToggleDecorator() {
        const {style, decorators, onClick} = this.props;
        return <decorators.Toggle style={style.toggle} onClick={onClick}/>;
    }

    render() {
        const {style, decorators, terminal, node} = this.props;
        return (
            <div
                style={node.active ? {...style.container} : {...style.link}}
            >
                {!terminal ? this.renderToggle() : null}
                <decorators.Header node={node} style={style.header}/>
            </div>
        );
    }
}


export default TreeNodeContainer;