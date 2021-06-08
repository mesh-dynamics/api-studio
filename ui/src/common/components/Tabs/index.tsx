import React, { Component, MouseEvent } from "react";
import { Glyphicon, Overlay, Popover } from "react-bootstrap";
import cs from "classnames";
import Tab, { ITabProps } from "./components/Tab";
import TabPanel, { ITabPanelProps } from "./components/TabPanel";

import Shortcuts from "../../utils/Shortcuts";
import "./components/Tabs.scss";
const tabPrefix = "tab-";
const panelPrefix = "panel-";

export default class Tabs extends Component<ITabsProps, ITabsState> {
  private selectedTabKeyProp: string;
  private isMouseDown: boolean = false;
  private tabScrollRef: HTMLDivElement;
  private scrollBtnRef: HTMLDivElement;
  private draggableTab: HTMLDivElement;
  private addBtnRef: HTMLDivElement;
  private mousePositionDiff: number = 0;
  private tabWidth: number = 0;
  private originalTabIndex: number = 0;
  private widthOfTabs: number[] = [];
  private scrollTimeout: NodeJS.Timeout;
  private setDragPropsOnMove: any = {};

  constructor(props:ITabsProps) {
    super(props);

    this.selectedTabKeyProp = props.selectedTabKey;
    this.state = {
      selectedTabKey: props.selectedTabKey,
      focusedTabKey: null,
      draggingTabId: "",
      currentTabIndex: -1,
      showOverlayTabMenu: false,
      showOverlayTarget: null
    };
  }

  static defaultProps = {
    items: [],
    selectedTabKey: undefined,
    allowRemove: false,
    removeActiveOnly: false,
    containerClass: undefined,
    tabsWrapperClass: undefined,
    tabClass: undefined,
    panelClass: undefined,
    onChange: () => null,
    onRemove: () => null,
  };

  componentDidMount() {
    this.setScrollPosition();
    Shortcuts.register("ctrl+n", this.props.onAddClick);
    document.body.addEventListener("mousedown", this.handleOutsideClick);
    // document.body.addEventListener("mouseleave", this.handleOverlayLeave);
  }

  componentWillUnmount() {
    Shortcuts.unregister("ctrl+n");
    document.body.removeEventListener("mousedown", this.handleOutsideClick);
    // document.body.removeEventListener("mouseleave", this.handleOverlayLeave);
  }

  handleOutsideClick = (event) => {
    const targetElement = event.target as HTMLDivElement;

    if(targetElement.className !== "RRT__popover-menu-item") {
      this.setState({ showOverlayTabMenu: false, showOverlayTarget: null });
    }
  }

  handleOverlayLeave = (event) => {

    console.log("EEEEEEEEE", event.target);
    const targetElement = event.target as HTMLDivElement;

    if(targetElement.id === "menuItemOverlay") {
      this.setState({ showOverlayTabMenu: false, showOverlayTarget: null }); 
    }
  }

  static getDerivedStateFromProps(props: ITabsProps, state: ITabsState) {
    if (props.selectedTabKey != state.selectedTabKey) {
      return { selectedTabKey: props.selectedTabKey };
    }
    return null;
  }

  shouldComponentUpdate(nextProps: ITabsProps, nextState: ITabsState) {
    const { selectedTabKey } = this.state;
    const { items, allowRemove, removeActiveOnly } = this.props;

    return (
      items !== nextProps.items ||
      nextProps.allowRemove !== allowRemove ||
      nextProps.removeActiveOnly !== removeActiveOnly ||
      nextProps.selectedTabKey !== this.selectedTabKeyProp ||
      nextState.selectedTabKey !== selectedTabKey ||
      nextState.draggingTabId != this.state.draggingTabId ||
      nextState.currentTabIndex != this.state.currentTabIndex ||
      nextState.showOverlayTabMenu != this.state.showOverlayTabMenu
    );
  }

  componentDidUpdate(prevProps: ITabsProps) {
    const { items, selectedTabKey } = this.props;

    if (this.selectedTabKeyProp !== selectedTabKey) {
      this.setState({ selectedTabKey });
    }

    if (items !== prevProps.items) {
      this.setScrollPosition();
    }

    this.selectedTabKeyProp = selectedTabKey;
  }

  onChangeTab = (nextTabKey: string) => {
    const { onChange } = this.props;

    // change active tab
    this.setState({ selectedTabKey: nextTabKey });

    if (onChange && this.props.selectedTabKey != nextTabKey) {
      onChange(nextTabKey);
    }
  };

  onFocusTab = (focusedTabKey: string) => () => this.setState({ focusedTabKey });

  onBlurTab = () => this.setState({ focusedTabKey: null });

  onKeyDown = (event) => {
    const { focusedTabKey } = this.state;
    if (event.keyCode === 13 && focusedTabKey !== null) {
      this.setState({ selectedTabKey: focusedTabKey });
    }
  };

  setScrollPosition = () => {
    if (this.tabScrollRef) {
      const refEle = this.tabScrollRef;
      if (refEle.offsetWidth < refEle.scrollWidth) {
        this.scrollBtnRef.classList.remove("hide");
        this.addBtnRef.classList.add("hide");

        //Set current Tab in Visible View Part
        const selectedTab = this.tabScrollRef.querySelector(
          ".RRT__tab--selected"
        ) as HTMLDivElement;
        if (
          selectedTab &&
          selectedTab.offsetLeft + selectedTab.offsetWidth >
            refEle.scrollLeft + refEle.offsetWidth
        ) {
          refEle.scroll(
            selectedTab.offsetLeft +
              selectedTab.offsetWidth -
              refEle.offsetWidth,
            0
          );
        }
      } else if (!this.scrollBtnRef.classList.contains("hide")) {
        this.scrollBtnRef.classList.add("hide");
        this.addBtnRef.classList.remove("hide");
      }
    }
  };

  getTabs = () => {
    const { items, allowRemove, removeActiveOnly, onRemove } = this.props;

    const selectedTabKey = this.getSelectedTabKey();
    const collapsed = false;

    let tabIndex = 0;

    const initialResultValue: ITabAccumulator = { tabsVisible: [], panels: {} };

    return items.reduce((result, item, index) => {
      const {
        key = index.toString(),
        title,
        content,
        getContent,
        disabled,
        tabClassName,
        panelClassName,
        hasTabChanged,
        isHighlighted,
      } = item;

      const selected = selectedTabKey === key;
      const payload = { tabIndex, collapsed, selected, disabled, key };
      const tabPayload = {
        ...payload,
        title,
        onRemove: (evt) => {
          if (typeof onRemove === "function") {
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
        className: panelClassName,
      };

      tabIndex += 1;
      result.tabsVisible.push(this.getTabProps(tabPayload));
      result.panels[key] = this.getPanelProps(panelPayload);

      return result;
    }, initialResultValue);
  };

  findParentWithClass = (currentElement: Element, selectorClass: string) => {
    while (currentElement) {
      if (currentElement.classList.contains(selectorClass)) {
        return currentElement;
      } else {
        currentElement = currentElement.parentElement!;
      }
    }
    return currentElement;
  };

  //Store width of each tab, so we can evaluate left or right movement of tab
  calculateTabWidth = (currentTabElement: Element) => {
    const tabContainer = this.findParentWithClass(
      currentTabElement,
      "tabContainer"
    );
    this.widthOfTabs = [];
    let currentTabIndex = 0;
    tabContainer.childNodes.forEach((tab) => {
      if (currentTabElement == tab) {
        currentTabIndex = this.widthOfTabs.length;
      }
      this.widthOfTabs.push((tab as HTMLDivElement).clientWidth);
    });
    return currentTabIndex;
  };

  //Next index at which placeholder for tab should be moved to indicate next position while dragging
  evaluateNextIndex = (nextLeftvalue: number) => {
    let currentIndex = this.originalTabIndex;
    const up = (currentIndex: number) => this.widthOfTabs[currentIndex - 1];
    const down = (currentIndex: number) => this.widthOfTabs[currentIndex + 1];
    if (nextLeftvalue < 0) {
      while (
        currentIndex > 0 &&
        -nextLeftvalue >up(currentIndex)
      ) {
        nextLeftvalue += up(currentIndex);
        currentIndex--;
      }
    } else if (nextLeftvalue > 0) {
      while (
        currentIndex < this.widthOfTabs.length -2 &&
        nextLeftvalue > down(currentIndex)
      ) {
        nextLeftvalue -= down(currentIndex);
        currentIndex++;
      }
    }
    return currentIndex;
  };

  onMouseMove = (event: MouseEvent) => {
    event.preventDefault(); //Restrict selection of text based on mouse move
    /*
    while moving mouse, set left of absolute positioned `draggableTab` to move the `tab`-div with mouse
    it will also set the currentIndex of placeholder-div
    */
    if (
      this.isMouseDown &&
      this.setDragPropsOnMove.draggingTabId != this.state.draggingTabId
    ) {
      this.setState({
        draggingTabId: this.setDragPropsOnMove.draggingTabId,
        currentTabIndex: this.setDragPropsOnMove.currentTabIndex,
      });
    }
    if (this.isMouseDown && this.draggableTab) {
      let nextLeftvalue = event.clientX - this.mousePositionDiff;
      const nextTabIndex = this.evaluateNextIndex(nextLeftvalue);
      if (nextTabIndex < this.originalTabIndex) {
        nextLeftvalue -= this.tabWidth;
      }
      this.draggableTab.style.left = nextLeftvalue + "px";
      this.setState({ currentTabIndex: nextTabIndex });
    } else {
      this.onMouseUp();
    }
  };

  onMouseDownCallback = (event: MouseEvent) => {
    /*
      Change the tab to current clicked one. We will start tab positionaing animation only after mouse is moved
    */
    const targetElement = event.target as HTMLDivElement;
    const tabElement =
      this.findParentWithClass(targetElement, "md-hc-tab") || targetElement;
    const tabId = tabElement.getAttribute("data-tabid");
    if (tabId) {
      const currentTabIndex = this.calculateTabWidth(tabElement);
      this.tabWidth = tabElement.clientWidth;
      this.originalTabIndex = currentTabIndex;
      this.setDragPropsOnMove = { draggingTabId: tabId, currentTabIndex };
      this.isMouseDown = true;
      this.mousePositionDiff = event.clientX;
      this.onChangeTab(tabId);
      document.body.addEventListener("mousemove", this.onMouseMove);
      document.body.addEventListener("mouseup", this.onMouseUp);
    }
  };

  onMouseUp = () => {
    /**
     * Update redux state for any index change
     */
    this.isMouseDown = false;
    document.body.removeEventListener("mouseup", this.onMouseUp);
    document.body.removeEventListener("mousemove", this.onMouseMove);
    if (this.state.draggingTabId) {
      if (this.state.currentTabIndex != this.originalTabIndex) {
        this.props.onPositionChange(
          this.originalTabIndex,
          this.state.currentTabIndex
        );
      }
      this.setState({ draggingTabId: "", currentTabIndex: -1 });
    }
  };

  getTabProps = ({
    title,
    key,
    selected,
    collapsed,
    tabIndex,
    disabled,
    className,
    onRemove,
    allowRemove,
    hasTabChanged,
    isHighlighted,
  }): ITabProps => ({
    selected,
    allowRemove,
    children: title,
    key: tabPrefix + key,
    id: tabPrefix + key,
    originalKey: key,
    onClick: this.onChangeTab,
    onFocus: this.onFocusTab,
    onBlur: this.onBlurTab,
    onMouseDown: this.onMouseDownCallback,
    onRemove,
    panelId: panelPrefix + key,
    classNames: this.getClassNamesForTab({
      selected,
      collapsed,
      tabIndex,
      disabled,
      className,
      isHighlighted,
    }),
    hasTabChanged: hasTabChanged,
    tabId: key,
    disabled,
  });

  getPanelProps = ({
    key,
    content,
    getContent,
    className,
  }): ITabPanelProps => ({
    getContent,
    children: content,
    key: panelPrefix + key,
    id: panelPrefix + key,
    tabId: tabPrefix + key,
    classNames: this.getClassNamesForPanel(className),
  });

  getClassNamesForPanel = (className: string) => {
    const { panelClass } = this.props;
    return cs("RRT__panel", className, panelClass);
  };
  getClassNamesForTab = ({
    selected,
    collapsed,
    tabIndex,
    disabled,
    className = "",
    isHighlighted = false,
  }) => {
    const { tabClass } = this.props;
    return cs("RRT__tab", className, tabClass, {
      "RRT__tab--first": !tabIndex,
      "RRT__tab--selected": selected,
      "RRT__tab--disabled": disabled,
      "RRT__tab--collapsed": collapsed,
      "RRT__tab--highlighted": isHighlighted,
    });
  };

  getSelectedTabKey = () => {
    const { items } = this.props;
    const { selectedTabKey } = this.state;

    if (typeof selectedTabKey === "undefined") {
      if (!items[0]) {
        return undefined;
      }

      return items[0].key || 0;
    }

    return selectedTabKey;
  };

  onScrollLeft = () => {
    const refEle = this.tabScrollRef;
    let leftScroll = refEle.scrollLeft;
    leftScroll = Math.max(leftScroll - 50, 0);
    refEle.scroll(leftScroll, 0);
    this.scrollTimeout = setInterval(() => {
      leftScroll = Math.max(leftScroll - 50, 0);
      refEle.scroll(leftScroll, 0);
    }, 100);
  };

  onScrollRight = () => {
    const refEle = this.tabScrollRef;
    let leftScroll = refEle.scrollLeft;
    let maxScroll = refEle.scrollWidth - refEle.offsetWidth;
    leftScroll = Math.min(leftScroll + 50, maxScroll);
    refEle.scroll(leftScroll, 0);
    this.scrollTimeout = setInterval(() => {
      leftScroll = Math.min(leftScroll + 50, maxScroll);
      refEle.scroll(leftScroll, 0);
    }, 100);
  };
  onScrollFinished = () => {
    if (this.scrollTimeout) {
      clearInterval(this.scrollTimeout);
    }
  };

  handleAddIconMouseEnter = e => {
    this.setState({ showOverlayTabMenu: true, showOverlayTarget: e.target });
  }

  handleAddIconClick = e => {
    this.setState({ showOverlayTabMenu: !this.state.showOverlayTabMenu, showOverlayTarget: e.target });
  };

  handleAddRestTab = (event: React.MouseEvent) => {
    this.props.onAddClick(event);
    this.setState({ showOverlayTabMenu: false, showOverlayTarget: null });
  }

  handleAddGrpcTab = (event: React.MouseEvent) => {
    this.props.onGRPCAddClick();
    this.setState({ showOverlayTabMenu: false, showOverlayTarget: null });
  }

  renderTabs = (tabsVisible: ITabProps[], draggingTabId: string) => {
    const tabElements: JSX.Element[] = [];
    //Placeholder to show next position on TabContainer
    const nextPositionPlaceholder = (
      <div
        className="dragging-placeholder"
        key="dragDiv"
        style={{ minWidth: this.tabWidth + "px" }}
      ></div>
    );

    tabsVisible.forEach((tab, index) => {
      if (
        this.state.currentTabIndex == index &&
        this.state.currentTabIndex < this.originalTabIndex
      ) {
        tabElements.push(nextPositionPlaceholder);
      }
      if (tab.tabId == draggingTabId) {
        //Wrapping draggable tab under a fixed container "current-tab-place"(relative-positioned) so that `tab` can be (absolute) positioned
        tabElements.push(
          <div className="current-tab-place" key={tab.tabId}>
            <div
              ref={(element) => (this.draggableTab = element!)}
              style={{ width: this.tabWidth + "px" }}
              className="dragable-tab"
            >
              <Tab {...tab} />
            </div>
          </div>
        );
      } else {
        tabElements.push(<Tab {...tab} />);
      }
      if (
        this.state.currentTabIndex == index &&
        this.state.currentTabIndex >= this.originalTabIndex
      ) {
        tabElements.push(nextPositionPlaceholder);
      }
    });

    return tabElements;
  };

  render() {
    const { containerClass, tabsWrapperClass } = this.props;
    const { showOverlayTabMenu, showOverlayTarget } = this.state;
    const { tabsVisible, panels } = this.getTabs();
    const isCollapsed = false;
    const selectedTabKey = this.getSelectedTabKey();

    const containerClasses = cs("RRT__container", containerClass);
    const tabsClasses = cs("RRT__tabs", tabsWrapperClass);

    return (
      <div className={containerClasses} onKeyDown={this.onKeyDown}>
        <div className={tabsClasses}>
          <div className="tabContainer" ref={(e) => (this.tabScrollRef = e!)}>
            {this.renderTabs(tabsVisible, this.state.draggingTabId)}
            <div
              className="RRT_add-icon-container"
              onMouseEnter={this.handleAddIconMouseEnter}
              onClick={this.handleAddIconClick}
              ref={(e) => (this.addBtnRef = e!)}
            >
              <div className="RRT_add-icon">
                <Glyphicon glyph="plus" />
              </div>
              {
                showOverlayTabMenu &&
                <Overlay
                  id="menuItemOverlay"
                  show={showOverlayTabMenu}
                  onHide={() =>{ /** Required prop. Don't need to do anything */}}
                  target={showOverlayTarget}
                  placement="bottom"
                  container={this}
                  containerPadding={20}
                  rootClose
                >
                  <Popover 
                    id="popover-contained"
                    style={{ 
                      marginLeft: "-35px",
                      borderRadius: "2px",
                      marginTop: "-10px"
                    }}
                  >
                      <div className="RRT__popover-menu-container">
                        <span 
                          className="RRT__popover-menu-item"
                          onClick={this.handleAddRestTab}
                        >REST</span>
                        <span
                          className="RRT__popover-menu-item"
                          onClick={this.handleAddGrpcTab}
                        >gRPC</span>
                      </div>
                  </Popover>
                </Overlay>
              }  
            </div>
          </div>
          <div
            className="RRT__scrollButtons"
            ref={(e) => (this.scrollBtnRef = e!)}
          >
            <div 
              className="RRT-icon-container" 
              onMouseEnter={this.handleAddIconMouseEnter}
              onClick={this.handleAddIconClick}
            >
              <Glyphicon className="RRT__add-icon" glyph="plus" />
            </div>
            <div
              className="RRT-icon-container"
              onMouseDown={this.onScrollLeft}
              onMouseUp={this.onScrollFinished}
            >
              <Glyphicon className="RRT__left-icon" glyph="chevron-left" />
            </div>
            <div
              className="RRT-icon-container"
              onMouseDown={this.onScrollRight}
              onMouseUp={this.onScrollFinished}
            >
              <Glyphicon className="RRT__right-icon" glyph="chevron-right" />
            </div>
          </div>
        </div>

        {!isCollapsed && selectedTabKey!= undefined && panels[selectedTabKey] && (
          <TabPanel {...panels[selectedTabKey]} />
        )}
      </div>
    );
  }
}

export interface ITabsState {
  selectedTabKey: string;
  focusedTabKey?: string;
  draggingTabId: string;
  currentTabIndex: number;
  showOverlayTabMenu: boolean;
  showOverlayTarget: any; // TODO: figure this out later
}
interface ITabAccumulator {
  tabsVisible: ITabProps[];
  panels: {
    [key: string]: ITabPanelProps;
  };
}
export interface ITabDescription {
  //This is prop type, which should be as returned by `getTabs` function in HTTPClientTabs.js
  title: React.ReactElement;
  getContent: () => React.ReactElement;
  key: string;
  tabClassName: string;
  panelClassName: string;
  hasTabChanged: boolean;
  isHighlighted: boolean;
  content?: React.ReactElement;
  disabled: boolean;
}

export interface ITabsProps {
  // list of tabs
  items: ITabDescription[];
  selectedTabKey: string;
  // show 'X' and remove tab
  allowRemove: boolean;
  // show 'X' closing element only for active tab
  removeActiveOnly: boolean;
  onAddClick: React.MouseEventHandler;
  onGRPCAddClick: React.MouseEventHandler;
  onChange: Function;
  onRemove: Function;
  onPositionChange: (fromPos: number, toPos: number) => void;
  containerClass: string;
  tabsWrapperClass: string;
  tabClass: string;
  panelClass: string;
}
