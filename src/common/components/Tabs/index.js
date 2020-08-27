import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import ResizeDetector from 'react-resize-detector';
import cs from 'classnames';
import throttle from 'lodash.throttle';
import PropTypes from 'prop-types';
import ShowMore from './components/ShowMore';
import Tab from './components/Tab';
import TabPanel from './components/TabPanel';
import InkBar from './components/InkBar';

const tabPrefix = 'tab-';
const panelPrefix = 'panel-';

export default class Tabs extends Component {
  
  constructor(props) {
    super(props);

    this.tabRefs = {};
    this.selectedTabKeyProp = props.selectedTabKey;

    this.state = {
      tabDimensions: {},
      blockWidth: 0,
      tabsTotalWidth: 0,
      showMoreWidth: 40,
      selectedTabKey: props.selectedTabKey,
      focusedTabKey: null
    };

    this.onResizeThrottled = throttle(this.onResize, props.resizeThrottle, { trailing: true });
  }

  componentDidMount() {
    this.setScrollPosition();
  }

  shouldComponentUpdate(nextProps, nextState) {
    const { selectedTabKey, blockWidth, showMoreWidth } = this.state;
    const { items, transform, showMore, showInkBar, allowRemove, removeActiveOnly } = this.props;

    return (
      items !== nextProps.items ||
      nextProps.transform !== transform ||
      nextProps.showMore !== showMore ||
      nextProps.showInkBar !== showInkBar ||
      nextProps.allowRemove !== allowRemove ||
      nextProps.removeActiveOnly !== removeActiveOnly ||
      nextState.blockWidth !== blockWidth ||
      nextState.showMoreWidth !== showMoreWidth ||
      nextProps.selectedTabKey !== this.selectedTabKeyProp ||
      nextState.selectedTabKey !== selectedTabKey
    );
  }

  componentDidUpdate(prevProps) {
    const { items, selectedTabKey } = this.props;

    if (this.selectedTabKeyProp !== selectedTabKey) {
      // eslint-disable-next-line react/no-did-update-set-state
      this.setState({ selectedTabKey });
    }

    if (items !== prevProps.items) {
      this.setScrollPosition();
    }

    this.selectedTabKeyProp = selectedTabKey;
  }

  onResize = () => {
    if (this.tabsWrapper) {
      const currentIsCollapsed = this.getIsCollapsed();
      this.setState({ blockWidth: this.tabsWrapper.offsetWidth }, () => {
        const { items } = this.props;
        const { selectedTabKey } = this.state;
        const nextIsCollapsed = this.getIsCollapsed();
        if (currentIsCollapsed && !nextIsCollapsed && selectedTabKey === -1 && items && items.length) {
          const firstTabKey = items[0].key || 0;
          this.setState({ selectedTabKey: firstTabKey });
        }
      });
    }
  };

  onChangeTab = nextTabKey => {
    const { onChange } = this.props;
    const { selectedTabKey } = this.state;
    const isCollapsed = this.getIsCollapsed();
    if (isCollapsed && selectedTabKey === nextTabKey) {
      // hide on mobile
      this.setState({ selectedTabKey: -1 });
    } else {
      // change active tab
      this.setState({ selectedTabKey: nextTabKey });
    }

    if (onChange) {
      onChange(nextTabKey);
    }
  };

  onFocusTab = focusedTabKey => () => this.setState({ focusedTabKey });

  onBlurTab = () => this.setState({ focusedTabKey: null });

  onKeyDown = event => {
    const { focusedTabKey } = this.state;
    if (event.keyCode === 13 && focusedTabKey !== null) {
      this.setState({ selectedTabKey: focusedTabKey });
    }
  };

  setScrollPosition =()=>{
    if(this.tabScrollRef){
      const refEle = this.tabScrollRef;
      if(refEle.offsetWidth < refEle.scrollWidth){
        this.scrollBtnRef.classList.remove('hide');
        this.addBtnRef.classList.add('hide');

        //Set current Tab in Visible View Part
        const selectedTab = this.tabScrollRef.querySelector('.RRT__tab--selected');
        if(selectedTab && selectedTab.offsetLeft + selectedTab.offsetWidth > refEle.scrollLeft + refEle.offsetWidth){
          refEle.scroll(selectedTab.offsetLeft + selectedTab.offsetWidth-  refEle.offsetWidth, 0);
        }
      }else if( !this.scrollBtnRef.classList.contains('hide')){
        this.scrollBtnRef.classList.add('hide');
        this.addBtnRef.classList.remove('hide');
      }
    }
  }

  getTabs = () => {
    //TODO: Need more cleanup here as we will not need to hide tabs.
    const { showMore, transform, transformWidth, items, allowRemove, removeActiveOnly, onRemove } = this.props;
    const { blockWidth, tabsTotalWidth, tabDimensions, showMoreWidth } = this.state;
    const selectedTabKey = this.getSelectedTabKey();
    const collapsed = blockWidth && transform && blockWidth < transformWidth;

    let tabIndex = 0;
    let availableWidth = blockWidth - (tabsTotalWidth > blockWidth ? showMoreWidth : 0);

    return items.reduce(
      (result, item, index) => {
        const { key = index, title, content, getContent, disabled, tabClassName, panelClassName } = item;

        const selected = selectedTabKey === key;
        const payload = { tabIndex, collapsed, selected, disabled, key };
        const tabPayload = {
          ...payload,
          title,
          onRemove: evt => {
            if (typeof onRemove === 'function') {
              onRemove(key, evt);
            }
          },
          allowRemove: allowRemove && (!removeActiveOnly || selected),
          className: tabClassName
        };

        const panelPayload = {
          ...payload,
          content,
          getContent,
          className: panelClassName
        };

        const tabWidth = tabDimensions[key] ? tabDimensions[key].width : 0;

        tabIndex += 1;
        result.tabsVisible.push(tabPayload);
        /* eslint-disable no-param-reassign */
        
        /* eslint-enable no-param-reassign */

        result.panels[key] = panelPayload; // eslint-disable-line no-param-reassign
        availableWidth -= tabWidth;

        return result;
      },
      { tabsVisible: [], tabsHidden: [], panels: {}, isSelectedTabHidden: false }
    );
  };

  getTabProps = ({ title, key, selected, collapsed, tabIndex, disabled, className, onRemove, allowRemove }) => ({
    selected,
    allowRemove,
    children: title,
    key: tabPrefix + key,
    id: tabPrefix + key,
    ref: e => (this.tabRefs[tabPrefix + key] = e),
    originalKey: key,
    onClick: this.onChangeTab,
    onFocus: this.onFocusTab,
    onBlur: this.onBlurTab,
    onRemove,
    panelId: panelPrefix + key,
    classNames: this.getClassNamesFor('tab', {
      selected,
      collapsed,
      tabIndex,
      disabled,
      className
    })
  });

  getPanelProps = ({ key, content, getContent, className }) => ({
    getContent,
    children: content,
    key: panelPrefix + key,
    id: panelPrefix + key,
    tabId: tabPrefix + key,
    classNames: this.getClassNamesFor('panel', { className })
  });

  getShowMoreProps = (isShown, isSelectedTabHidden, showMoreLabel) => ({
    onShowMoreChanged: this.showMoreChanged,
    isShown,
    label: showMoreLabel,
    hasChildSelected: isSelectedTabHidden
  });

  getClassNamesFor = (type, { selected, collapsed, tabIndex, disabled, className = '' }) => {
    const { tabClass, panelClass } = this.props;
    switch (type) {
      case 'tab':
        return cs('RRT__tab', className, tabClass, {
          'RRT__tab--first': !tabIndex,
          'RRT__tab--selected': selected,
          'RRT__tab--disabled': disabled,
          'RRT__tab--collapsed': collapsed
        });
      case 'panel':
        return cs('RRT__panel', className, panelClass);
      default:
        return '';
    }
  };

  getSelectedTabKey = () => {
    const { items } = this.props;
    const { selectedTabKey } = this.state;

    if (typeof selectedTabKey === 'undefined') {
      if (!items[0]) {
        return undefined;
      }

      return items[0].key || 0;
    }

    return selectedTabKey;
  };

  getIsCollapsed = () => {
    const { transform, transformWidth } = this.props;
    const { blockWidth } = this.state;
    return blockWidth && transform && blockWidth < transformWidth;
  };

  showMoreChanged = element => {
    if (!element) {
      return;
    }

    const { showMoreWidth } = this.state;
    const { offsetWidth } = element;
    if (showMoreWidth === offsetWidth) {
      return;
    }

    this.setState({
      showMoreWidth: offsetWidth
    });
  };
  
  onScrollLeft = ()=>{
    const refEle = this.tabScrollRef;
    let leftScroll = refEle.scrollLeft;
    leftScroll = Math.max(leftScroll -50, 0);
    refEle.scroll(leftScroll, 0);
    this.scrollTimeout = setInterval(()=>{
      leftScroll = Math.max(leftScroll -50, 0);
      refEle.scroll(leftScroll, 0);
    }, 100);
  };

  onScrollRight =()=>{
    const refEle = this.tabScrollRef;
    let leftScroll = refEle.scrollLeft;
    let maxScroll = refEle.scrollWidth - refEle.offsetWidth;
    leftScroll = Math.min(leftScroll + 50, maxScroll);
    refEle.scroll(leftScroll, 0);
    this.scrollTimeout = setInterval(()=>{
      leftScroll = Math.min(leftScroll + 50, maxScroll);
      refEle.scroll(leftScroll, 0);
    }, 100);
  };
  onScrollFinished = ()=>{
    if(this.scrollTimeout){
      clearInterval(this.scrollTimeout);
    }
  };

  render() {
    const { showInkBar, containerClass, tabsWrapperClass, showMore, transform, showMoreLabel, onAddClick } = this.props;
    const { tabDimensions } = this.state;
    const { tabsVisible, tabsHidden, panels, isSelectedTabHidden } = this.getTabs();
    const isCollapsed = this.getIsCollapsed();
    const selectedTabKey = this.getSelectedTabKey();
    const selectedTabDimensions = tabDimensions[selectedTabKey] || {};

    const containerClasses = cs('RRT__container', containerClass);
    const tabsClasses = cs('RRT__tabs', tabsWrapperClass, { RRT__accordion: isCollapsed });

    return (
      <div className={containerClasses} ref={e => (this.tabsWrapper = e)} onKeyDown={this.onKeyDown}>
        <div  className={tabsClasses} >
        <div className="tabContainer" ref={e=> (this.tabScrollRef=  e)}>        
          {tabsVisible.reduce((result, tab) => {
            result.push(<Tab {...this.getTabProps(tab)} />);

            if (isCollapsed && selectedTabKey === tab.key) {
              result.push(<TabPanel {...this.getPanelProps(panels[tab.key])} />);
            }
            return result;
          }, [])}

         
          <div className="RRT_add-icon-container"  ref={e=> (this.addBtnRef = e)} onClick={onAddClick}>
            <Glyphicon className="RRT__add-icon" className="" glyph="plus" />
          </div>
          </div>
          <div className="RRT_scrollButtons" ref={e=> (this.scrollBtnRef = e)}>
            

            <div className="RRT-icon-container" onClick={onAddClick}>
                <Glyphicon className="RRT__add-icon" className="" glyph="plus" />
              </div>
              <div className="RRT-icon-container"  onMouseDown={this.onScrollLeft} onMouseUp={this.onScrollFinished}>
                <Glyphicon className="RRT__left-icon" className="" glyph="chevron-left" />
              </div>
              <div className="RRT-icon-container" onMouseDown={this.onScrollRight} onMouseUp={this.onScrollFinished}>
                <Glyphicon className="RRT__right-icon" className="" glyph="chevron-right" />
              </div>
          </div>
        </div>
        
        {showInkBar && !isCollapsed && !isSelectedTabHidden && (
          <InkBar left={selectedTabDimensions.offset || 0} width={selectedTabDimensions.width || 0} />
        )}

        {!isCollapsed && panels[selectedTabKey] && <TabPanel {...this.getPanelProps(panels[selectedTabKey])} />}

        {(showMore || transform) && <ResizeDetector handleWidth onResize={this.onResizeThrottled} />}
      </div>
    );
  }
}

Tabs.propTypes = {
  /* eslint-disable react/no-unused-prop-types */
  // list of tabs
  items: PropTypes.oneOfType([PropTypes.array, PropTypes.object]),
  /* eslint-enable react/no-unused-prop-types */
  // selected tab key
  selectedTabKey: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  // show 'X' and remove tab
  allowRemove: PropTypes.bool,
  // show 'X' closing element only for active tab
  removeActiveOnly: PropTypes.bool,
  // move tabs to the special `Show more` tab if they don't fit into a screen
  showMore: PropTypes.bool,
  // materialUI-like rail under the selected tab
  showInkBar: PropTypes.bool,
  // transform to the accordion on small screens
  transform: PropTypes.bool,
  // tabs will be transformed to accodrion for screen sizes below `transformWidth`px
  transformWidth: PropTypes.number,
  // addTab callback,
  onAddClick: PropTypes.func,
  // onChange active tab callback
  onChange: PropTypes.func,
  // onRemove callback
  onRemove: PropTypes.func,
  // frequency of onResize recalculation fires
  resizeThrottle: PropTypes.number,
  // classnames
  containerClass: PropTypes.string,
  tabsWrapperClass: PropTypes.string,
  tabClass: PropTypes.string,
  panelClass: PropTypes.string,
  // labels
  showMoreLabel: PropTypes.oneOfType([PropTypes.string, PropTypes.node])
};

Tabs.defaultProps = {
  items: [],
  selectedTabKey: undefined,
  showMore: true,
  showInkBar: false,
  allowRemove: false,
  removeActiveOnly: false,
  transform: true,
  transformWidth: 800,
  resizeThrottle: 100,
  containerClass: undefined,
  tabsWrapperClass: undefined,
  tabClass: undefined,
  panelClass: undefined,
  showMoreLabel: '...',
  onChange: () => null,
  onRemove: () => null
};
