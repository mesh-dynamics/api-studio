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

import * as React from "react";
import { Button } from "react-bootstrap";
import "./commonStyles.css";
import classNames from 'classnames';

/**
 * This component is reusable across application to provide Same user experience, without worrying CSS
 */

export interface ICubeButton{
  onClick: () => void;
  faIcon?: string;
  glyphIcon?: string;
  label?: string;
  title?: string;
  className?: string;
  disabled?: boolean;
  size?: "sm" | "lg" | "md";
  style?: React.CSSProperties | undefined;
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
      disabled ={props.disabled || false}
      className={classes}
      onClick={props.onClick}
      title={props.title}
      style={props.style}
    >
      {faIcon}
      {props.label ? " " + props.label : ""}
    </Button>
  );
}
