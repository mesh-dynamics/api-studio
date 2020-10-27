import React, { useEffect, useState } from 'react';
import classNames from 'classnames';
export interface ISplitSliderProps {
    slidingElement: HTMLDivElement,
    horizontal?: boolean
}
let isMouseDown = false;
let mousePositionDiff = 0;
export default function SplitSlider(props:ISplitSliderProps) {
    const sliderRef = React.createRef<HTMLDivElement>();

    var onMouseMove = (event: MouseEvent) => {
        if (isMouseDown === true && props.slidingElement) {
            if(props.horizontal){
                props.slidingElement.style.height = (event.clientY - mousePositionDiff) + "px"
            }else{
                props.slidingElement.style.width = (event.clientX - mousePositionDiff) + "px"
            }
        } else {
            onMouseUp()
        }
    }
    var onMouseUp = () => {
        isMouseDown = false;
        document.body.removeEventListener('mouseup', onMouseUp);
        document.body.removeEventListener('mousemove', onMouseMove);
    }
    var onMouseDown = (event: MouseEvent) => {
        isMouseDown = true;
        if (props.slidingElement) {
            if(props.horizontal){
                mousePositionDiff = event.clientY - props.slidingElement.offsetHeight;
            }else{
                mousePositionDiff = event.clientX - props.slidingElement.offsetWidth;
            }
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

    const className = classNames({"split-slider": true, "horizontal": props.horizontal});
    return (
        <div className={className} ref={sliderRef}></div>
    )
}