import React, { Component } from 'react';

export default class Tab extends Component<ITabProps> {
  static defaultProps = {
    children: undefined,
    onRemove: () => {},
    allowRemove: false,
    disabled: false
  };
  shouldComponentUpdate(nextProps) {
    const { children, selected, classNames } = this.props;
    return children !== nextProps.children || selected !== nextProps.selected || classNames !== nextProps.classNames;
  }

  onTabClick = () => {
    const { onClick, originalKey } = this.props;
    onClick(originalKey);
  };

  renderRemovableTab = () => {
    const { children, onRemove, hasTabChanged } = this.props;
    const title = hasTabChanged ? "Content has changed in this request. Save the changes before closing.": "";
    return (
      <div className="RRT__removable">
        <div className="RRT__removable-text">
          
          {children}
        </div>
        <div className="RRT__removable-icon" onClick={onRemove} title={title}>
          {hasTabChanged ? <i className="fa fa-circle fa-sm font-12 RRT__removable-icon-has-changed"></i> : <i className="fa fa-times fa-sm font-12"></i>}
        </div>
      </div>
    );
  };

  renderTab = () => {
    const { children, allowRemove } = this.props;

    if (allowRemove) {
      return this.renderRemovableTab();
    }

    return children;
  };

  render() {
    const { id, classNames, selected, disabled, panelId, onFocus, onBlur, originalKey } = this.props;

    return (
      <div
        role="tab"
        className={classNames}
        id={id}
        aria-selected={selected ? 'true' : 'false'}
        aria-expanded={selected ? 'true' : 'false'}
        aria-disabled={disabled ? 'true' : 'false'}
        aria-controls={panelId}
        tabIndex={0}
        onClick={this.onTabClick}
        onFocus={onFocus(originalKey)}
        onBlur={onBlur}
        onMouseDown={this.props.onMouseDown}
        data-tabid={this.props.tabId}
      >
        {this.renderTab()}
      </div>
    );
  }
}

export interface ITabProps {
  children: React.ReactChild,
  disabled: boolean,

  // generic props
  panelId: string,
  selected: boolean,
  onClick: Function,
  onRemove: React.MouseEventHandler<HTMLDivElement>,
  onFocus: Function,
  onBlur: React.FocusEventHandler<HTMLDivElement>,
  allowRemove: boolean,
  id: string,
  originalKey: string,
  classNames: string,
  key: string;
  tabId: string,
  hasTabChanged: boolean;
  onMouseDown: React.MouseEventHandler<HTMLDivElement>
};

