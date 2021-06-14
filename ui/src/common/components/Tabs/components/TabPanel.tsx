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

import React, { Component } from 'react';

export default class TabPanel extends Component<ITabPanelProps> {
  static defaultProps = {
    getContent: undefined,
    children: undefined
  }

  shouldComponentUpdate(nextProps) {
    const { children, getContent, classNames } = this.props;
    return (
      getContent !== nextProps.getContent || children !== nextProps.children || classNames !== nextProps.classNames
    );
  }

  render() {
    const { classNames, id, tabId, children, getContent } = this.props;

    return (
      <div className={classNames} role="tabpanel" id={id} aria-labelledby={tabId} aria-hidden="false">
        {getContent && getContent()}
        {!getContent && children}
      </div>
    );
  }
}

export interface ITabPanelProps {
  getContent: () => React.ReactElement
  children: React.ReactChild,
  id: string,
  key: string,
  // generic props
  classNames: string,
  tabId: string
};

