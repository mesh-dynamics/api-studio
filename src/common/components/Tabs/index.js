import React, { Component } from 'react';
import { Glyphicon } from 'react-bootstrap';
import cs from 'classnames';
import PropTypes from 'prop-types';
// import ShowMore from './components/ShowMore';
import Tab from './components/Tab';
import TabPanel from './components/TabPanel';

const tabPrefix = 'tab-';
const panelPrefix = 'panel-';

export default class Tabs extends Component {
  
  constructor(props) {
    super(props);

    this.tabRefs = {};
    this.selectedTabKeyProp = props.selectedTabKey;

    this.state = {
      selectedTabKey: props.selectedTabKey,
      focusedTabKey: null
    };
  }

  componentDidMount() {
    this.setScrollPosition();
  }
  
  static getDerivedStateFromProps(props,state) {
    if (props.selectedTabKey != state.selectedTabKey) {
      return { selectedTabKey:  props.selectedTabKey }
    }
  } 

  shouldComponentUpdate(nextProps, nextState) {
    const { selectedTabKey } = this.state;
    const { items, transform, allowRemove, removeActiveOnly } = this.props;

    return (
      items !== nextProps.items ||
      nextProps.transform !== transform ||
      nextProps.allowRemove !== allowRemove ||
      nextProps.removeActiveOnly !== removeActiveOnly ||
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

  onChangeTab = nextTabKey => {
    const { onChange } = this.props;
   
      // change active tab
    this.setState({ selectedTabKey: nextTabKey });
    
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
    const { transform, transformWidth, items, allowRemove, removeActiveOnly, onRemove } = this.props;
   
    const selectedTabKey = this.getSelectedTabKey();
    const collapsed = false;

    let tabIndex = 0;

    return items.reduce(
      (result, item, index) => {
        const { key = index, title, content, getContent, disabled, tabClassName, panelClassName, hasTabChanged, isHighlighted } = item;

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
          className: tabClassName,
          hasTabChanged,
          isHighlighted,
        };

        const panelPayload = {
          ...payload,
          content,
          getContent,
          className: panelClassName
        };

        tabIndex += 1;
        result.tabsVisible.push(tabPayload);
        /* eslint-disable no-param-reassign */
        
        /* eslint-enable no-param-reassign */

        result.panels[key] = panelPayload; // eslint-disable-line no-param-reassign
        // availableWidth -= tabWidth;

        return result;
      },
      { tabsVisible: [], panels: {}, isSelectedTabHidden: false }
    );
  };

  getTabProps = ({ title, key, selected, collapsed, tabIndex, disabled, className, onRemove, allowRemove, hasTabChanged, isHighlighted }) => ({
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
        className,
        isHighlighted,
      }),
      hasTabChanged: hasTabChanged,
  });

  getPanelProps = ({ key, content, getContent, className }) => ({
    getContent,
    children: content,
    key: panelPrefix + key,
    id: panelPrefix + key,
    tabId: tabPrefix + key,
    classNames: this.getClassNamesFor('panel', { className })
  });

  getClassNamesFor = (type, { selected, collapsed, tabIndex, disabled, className = '', isHighlighted=false }) => {
    const { tabClass, panelClass } = this.props;
    switch (type) {
      case 'tab':
        return cs('RRT__tab', className, tabClass, {
          'RRT__tab--first': !tabIndex,
          'RRT__tab--selected': selected,
          'RRT__tab--disabled': disabled,
          'RRT__tab--collapsed': collapsed,
          'RRT__tab--highlighted': isHighlighted,
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
    const { containerClass, tabsWrapperClass, onAddClick } = this.props;
    const { tabsVisible, panels } = this.getTabs();
    const isCollapsed = false;
    const selectedTabKey = this.getSelectedTabKey();

    const containerClasses = cs('RRT__container', containerClass);
    const tabsClasses = cs('RRT__tabs', tabsWrapperClass);

    return (
      <div className={containerClasses} ref={e => (this.tabsWrapper = e)} onKeyDown={this.onKeyDown}>
        <div  className={tabsClasses} >
        <div className="tabContainer" ref={e=> (this.tabScrollRef=  e)}>        
          {tabsVisible.map((tab) => {
            return (<Tab {...this.getTabProps(tab)} />);
          })}

         
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
        
        {!isCollapsed && panels[selectedTabKey] && <TabPanel {...this.getPanelProps(panels[selectedTabKey])} />}

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
  // classnames
  containerClass: PropTypes.string,
  tabsWrapperClass: PropTypes.string,
  tabClass: PropTypes.string,
  panelClass: PropTypes.string,
};

Tabs.defaultProps = {
  items: [],
  selectedTabKey: undefined,
  allowRemove: false,
  removeActiveOnly: false,
  transform: true,
  transformWidth: 800,
  // resizeThrottle: 100,
  containerClass: undefined,
  tabsWrapperClass: undefined,
  tabClass: undefined,
  panelClass: undefined,
  onChange: () => null,
  onRemove: () => null
};
