import React from 'react';
import PropTypes from 'prop-types';
import styled from '@emotion/styled';

const Polygon = styled('polygon', {
    shouldForwardProp: prop => ['className', 'children', 'points'].indexOf(prop) !== -1
})((({style}) => style));

const TreeNodeToggle = ({style, onClick}) => {
    const {height, width} = style;
    const midHeight = height * 0.5;
    const points = `0,0 0,${height} ${width},${midHeight}`;

    return (
        <div style={style.base} onClick={onClick}>
            <div style={style.wrapper}>
                <svg {...{height, width}}>
                    <Polygon points={points} style={style.arrow}/>
                </svg>
            </div>
        </div>
    );
};

TreeNodeToggle.propTypes = {
    style: PropTypes.object
};

export default TreeNodeToggle;