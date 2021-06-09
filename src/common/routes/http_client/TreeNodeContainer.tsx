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