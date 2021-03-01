import * as React from "react";
import { Button } from "react-bootstrap";
import "./commonStyles.css";
import classNames from 'classnames';

/**
 * This component is reusable across application to provide Same user experience, without worrying CSS
 */

export interface ICubeButton {
  onClick: () => void;
  faIcon?: string;
  glyphIcon?: string;
  label?: string;
  title?: string;
  className?: string;
  size?: "sm" | "lg" | "md";
}

export function CubeButton(props: ICubeButton) {
  const faIcon = props.faIcon ? <i className={"fas " + props.faIcon} /> : <></>;
  const btnClass = "btn-" + props.size || "sm";
  const additionalClass = props.className || "";
  const classes = classNames({
    "btn cube-btn text-center": true,
    [btnClass] : true,
    [additionalClass]: true
  })
  return (
    <Button
      className={classes}
      onClick={props.onClick}
      title={props.title}
    >
      {faIcon}
      {props.label ? " " + props.label : ""}
    </Button>
  );
}
