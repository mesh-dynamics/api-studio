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

import React from "react";
/**
 * Use this component, when an external link needs to be embedded in Page. This will open out of Devtool
 */
export interface IExternalLinkProps {
  link: string;
  children: React.ReactChild[] | React.ReactChild;
}

export function ExternalLink(props: IExternalLinkProps) {
  var handleClick = React.useCallback((event) => {
    if (PLATFORM_ELECTRON) {
      event.preventDefault();
      window.require("electron").shell.openExternal(props.link);
    }
  }, []);
  return (
    <a href={props.link} target="_blank" title="" onClick={handleClick}>
      {props.children}
    </a>
  );
}
