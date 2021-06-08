import React, { useEffect, useState } from "react";
import classNames from "classnames";
import { IStoreState } from "../reducers/state.types";
import { connect } from "react-redux";
import { httpClientActions } from "../actions/httpClientActions";
import _ from "lodash";
import { cube } from "../reducers/cube.reducer";
export interface ISplitSliderProps {
  slidingElement: HTMLDivElement;
  horizontal?: boolean;
  minSpace?: number; // Minimum height/width in pixels
  persistKey?: string;
  dispatch: any;
  existingPosition: number;
}
let isMouseDown = false;
let mousePositionDiff = 0;

export function SplitSlider(props: ISplitSliderProps) {
  const sliderRef = React.createRef<HTMLDivElement>();
  const [currentPosition, setPosition] = useState<number>(0);
  
  useEffect(() => {
    props.existingPosition && setPosition(props.existingPosition);
  }, [props.existingPosition]);

  useEffect(() => {
    if (props.slidingElement) {
      if (props.horizontal) {
        props.slidingElement.style.height = currentPosition + "px";
      } else {
        props.slidingElement.style.width = currentPosition + "px";
      }
    }
  }, [currentPosition]);

  const onPositionChange = React.useCallback(
    _.debounce((position) => {
      setPosition(position);
      props.dispatch(
        httpClientActions.setUiPreferenceKey(props.persistKey, position)
      );
    }, 1000),
    []
  );

  const persistValue = (position: number) => {
    props.persistKey && onPositionChange(position);
  };
  const getMin = (position: number) => {
    if (props.minSpace) {
      position = Math.max(props.minSpace, position);
    }
    persistValue(position);
    return position;
  };

  var onMouseMove = (event: MouseEvent) => {
    if (isMouseDown === true && props.slidingElement) {
      if (props.horizontal) {
        props.slidingElement.style.height =
          getMin(event.clientY - mousePositionDiff) + "px";
      } else {
        props.slidingElement.style.width =
          getMin(event.clientX - mousePositionDiff) + "px";
      }
    } else {
      onMouseUp();
    }
  };
  var onMouseUp = () => {
    isMouseDown = false;
    document.body.removeEventListener("mouseup", onMouseUp);
    document.body.removeEventListener("mousemove", onMouseMove);
  };
  var onMouseDown = (event: MouseEvent) => {
    isMouseDown = true;
    if (props.slidingElement) {
      if (props.horizontal) {
        mousePositionDiff = event.clientY - props.slidingElement.offsetHeight;
      } else {
        mousePositionDiff = event.clientX - props.slidingElement.offsetWidth;
      }
    }
    document.body.addEventListener("mousemove", onMouseMove);
    document.body.addEventListener("mouseup", onMouseUp);
  };

  useEffect(() => {
    if (sliderRef.current && props.slidingElement) {
      sliderRef.current.addEventListener("mousedown", onMouseDown);
    }
    return () => {
      document.body.removeEventListener("mouseup", onMouseUp);
      document.body.removeEventListener("mousemove", onMouseMove);
      if (sliderRef.current) {
        sliderRef.current.removeEventListener("mousedown", onMouseDown);
      }
    };
  }, [props.slidingElement]);

  const className = classNames({
    "split-slider": true,
    horizontal: props.horizontal,
  });
  return <div className={className} ref={sliderRef}></div>;
}

const mapStateToProps = (state: IStoreState, props: ISplitSliderProps) => ({
  existingPosition:
    props.persistKey && state.httpClient.uiPref
      ? state.httpClient.uiPref[props.persistKey]
      : 0,
});

const connectedSplitSlider = connect(mapStateToProps)(SplitSlider);

export default connectedSplitSlider;
