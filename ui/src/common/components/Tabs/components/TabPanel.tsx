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

