import React, { useEffect, useState } from 'react';
// export interface ISplitSlider {
//     slidingElement: React.RefObject<HTMLElement>
// }
let isMouseDown = false;
let mousePositionDiff = 0;
export default function SplitSlider(props) {
    const sliderRef = React.createRef();

    var onMouseMove = (event) => {
        if (isMouseDown === true && props.slidingElement) {
            props.slidingElement.style.width = (event.clientX - mousePositionDiff) + "px"
        } else {
            onMouseUp()
        }
    }
    var onMouseUp = () => {
        isMouseDown = false;
        document.body.removeEventListener('mouseup', onMouseUp);
        document.body.removeEventListener('mousemove', onMouseMove);
    }
    var onMouseDown = (event) => {
        isMouseDown = true;
        if (props.slidingElement) {
            mousePositionDiff = event.clientX - props.slidingElement.offsetWidth;
        }
        document.body.addEventListener('mousemove', onMouseMove)
        document.body.addEventListener('mouseup', onMouseUp);
    }

    useEffect(() => {
        if (sliderRef.current && props.slidingElement) {
            sliderRef.current.addEventListener('mousedown', onMouseDown)
        }
        return () => {
            document.body.removeEventListener('mouseup', onMouseUp)
            document.body.removeEventListener('mousemove', onMouseMove);
            if (sliderRef.current) {
                sliderRef.current.removeEventListener('mousedown', onMouseDown)
            }
        }
    }, [props.slidingElement]);


    return (
        <div className="split-slider" ref={sliderRef}></div>
    )
}