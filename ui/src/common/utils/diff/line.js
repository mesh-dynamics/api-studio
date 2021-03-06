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

"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var React = require("react");
var classnames_1 = require("classnames");
var rightLineNumberPrefix = 'R';
var leftLineNumberPrefix = 'L';
// author raj.maddireddy@cubecorp.io
var ReactBootstrap = require("react-bootstrap");
var Tooltip = ReactBootstrap.Tooltip;

var Actions = require("../../components/Actions/Actions.js").default;
var Resolutions = require("../../components/Resolutions.js").default;
var OperationSet = require("../../components/Golden/OperationSet.js").default;
var OperationSetLabel = require("../../components/Golden/OperationSetLabel.js").default;
var CollapsedChunkHandler = require("../../components/CollapsedChunkHandler.js").default;
var MaxChunkHandler = require("../../components/MaxChunkHandler.js").default;

var onLineNumberClickProxy = function (onLineNumberClick, id) {
    return function (e) { return onLineNumberClick(id, e); };
};
var LineNumber = function (_a) {
    var prefix = _a.prefix, lineNumber = _a.lineNumber;
    return React.createElement("pre", { id: prefix + "-" + lineNumber }, lineNumber);
};
exports.InlineLine = function (_a) {
    var leftLineNumber = _a.leftLineNumber, rightLineNumber = _a.rightLineNumber, added = _a.added, removed = _a.removed, content = _a.content, _b = _a.onLineNumberClick, onLineNumberClick = _b === void 0 ? function () { } : _b, renderContent = _a.renderContent, _c = _a.hightlightLines, hightlightLines = _c === void 0 ? [] : _c, styles = _a.styles, hideLineNumbers = _a.hideLineNumbers;
    // author raj.maddireddy@cubecorp.io 
    var jsonPath = _a.jsonPath, serverSideDiff = _a.serverSideDiff;
    var filterPath = _a.filterPath;
    var _d, _e, _f, _g;
    var hightlightLine = (leftLineNumber !== true || rightLineNumber !== true)
        && (hightlightLines.includes(leftLineNumberPrefix + "-" + leftLineNumber)
            || hightlightLines.includes(rightLineNumberPrefix + "-" + rightLineNumber));
    return !jsonPath || jsonPath.indexOf(filterPath) > -1 ? React.createElement("tr", { className: styles.line },
        !hideLineNumbers
            && React.createElement(React.Fragment, null,
                React.createElement("td", { className: classnames_1.default(styles.gutter, styles.leftGutter, (_d = {},
                        _d[styles.diffAdded] = added,
                        _d[styles.diffRemoved] = removed,
                        _d[styles.hightlightedGutter] = hightlightLine,
                        _d)), onClick: onLineNumberClickProxy(onLineNumberClick, leftLineNumberPrefix + "-" + leftLineNumber) }, leftLineNumber !== true
                    && React.createElement(LineNumber, { lineNumber: leftLineNumber, prefix: leftLineNumberPrefix })),
                React.createElement("td", { className: classnames_1.default(styles.gutter, styles.rightGutter, (_e = {},
                        _e[styles.diffAdded] = added,
                        _e[styles.diffRemoved] = removed,
                        _e[styles.hightlightedGutter] = hightlightLine,
                        _e)), onClick: onLineNumberClickProxy(onLineNumberClick, rightLineNumberPrefix + "-" + rightLineNumber) }, rightLineNumber !== true
                    && React.createElement(LineNumber, { lineNumber: rightLineNumber, prefix: rightLineNumberPrefix }))),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_f = {},
                _f[styles.diffAdded] = added,
                _f[styles.diffRemoved] = removed,
                _f[styles.hightlightedLine] = hightlightLine,
                _f)) },
            added && React.createElement("pre", null, "+"),
            removed && React.createElement("pre", null, "-")),
        React.createElement("td", { className: classnames_1.default((_g = {},
                _g[styles.diffAdded] = added,
                _g[styles.diffRemoved] = removed,
                _g[styles.hightlightedLine] = hightlightLine,
                _g)) }, renderContent && typeof content === 'string'
            ? renderContent(content)
            : React.createElement("pre", null, content))): React.createElement("tr", {style: {display: "none"}});
};
exports.DefaultLine = function (_a) {
    var leftLineNumber = _a.leftLineNumber, rightLineNumber = _a.rightLineNumber, onLineNumberClick = _a.onLineNumberClick, rightContent = _a.rightContent, leftContent = _a.leftContent, added = _a.added, removed = _a.removed, renderContent = _a.renderContent, _b = _a.hightlightLines, hightlightLines = _b === void 0 ? [] : _b, styles = _a.styles, hideLineNumbers = _a.hideLineNumbers,
    // author raj.maddireddy@cubecorp.io 
    jsonPath = _a.jsonPath, serverSideDiff = _a.serverSideDiff, apiPath = _a.apiPath, service = _a.service, app = _a.app, templateVersion = _a.templateVersion, replayId = _a.replayId, recordingId = _a.recordingId, recordReqId = _a.recordReqId, replayReqId = _a.replayReqId;
    var filterPaths = _a.filterPaths,
    inputElementRef = _a.inputElementRef,
    showAll = _a.showAll,
    searchFilterPath = _a.searchFilterPath,
    disableOperationSet = _a.disableOperationSet,
    hasChildren = _a.hasChildren,
    collapseChunk = _a.collapseChunk,
    drawChunk = _a.drawChunk,
    showMaxChunk = _a.showMaxChunk,
    showMaxChunkToggle = _a.showMaxChunkToggle,
    hasDiff = _a.hasDiff,
    showDiff = _a.showDiff,
    handleCollapseLength = _a.handleCollapseLength,
    handleMaxLinesLength = _a.handleMaxLinesLength,
    eventType = _a.eventType, method = _a.method;
    // author raj.maddireddy@cubecorp.io
    var _c, _d, _e, _f, _g, _h, _i, _j, _k, _l, _m, _n;
    var hightlightLeftLine = leftLineNumber !== true
        && hightlightLines.includes(leftLineNumberPrefix + "-" + leftLineNumber);
    var hightlightRightLine = rightLineNumber !== true
        && hightlightLines.includes(rightLineNumberPrefix + "-" + rightLineNumber);
    // author raj.maddireddy@cubecorp.io
    const tooltip = (
        React.createElement(Tooltip, {id: "tooltip"}, React.createElement("strong", {glyph: "align-left"}, "Holy! "), "Its working.")
    );
    let actionsWrapperElementRef = React.createRef();
    let leftOperationSetElementRef = React.createRef(),
        rightOperationSetElementRef = React.createRef();
    const showRefElement = function(ref) {
        let refElement = ref.current;
        if(refElement) refElement.style.visibility = "visible";
    }
    const hideRefElement = function(ref) {
        let refElement = ref.current;
        if(refElement) refElement.style.visibility = "hidden";
    }

    // prefix match filter paths
    let showPath = jsonPath ? filterPaths.some(path => (jsonPath.indexOf(path) > -1)) : showPath;
    
    return !jsonPath || (showAll || showPath) && (jsonPath.indexOf(searchFilterPath) > -1) ? 
    <React.Fragment>
        {showPath && !showAll && (
            <tr className={styles.line}>
                <td className={classnames_1(styles.gutter, styles.leftGutter, {[styles.diffRemoved]: removed, [styles.hightlightedGutter]: hightlightLeftLine})}/>
                <td className={classnames_1.default(styles.gutter, styles.leftGutter, (_k = {}, _k[styles.diffRemoved] = removed, _k[styles.hightlightedGutter] = hightlightLeftLine, _k))}>
                </td>
                <td className={classnames_1.default(styles.marker, (_l = {}, _l[styles.diffRemoved] = removed, _l[styles.hightlightedLine] = hightlightLeftLine, _l))}/>
                <td className={classnames_1.default(styles.marker, (_d = {}, _d[styles.diffRemoved] = removed, _d[styles.hightlightedLine] = hightlightLeftLine, _d))}>
                </td>
                <td className={classnames_1.default(styles.defaultTdClass, (_e = {}, _e[styles.diffRemoved] = removed, _e[styles.hightlightedLine] = hightlightLeftLine, _e))}>
                    <span style={{color:"#ab4d4d"}}>{jsonPath}</span>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_j = {}, _j[styles.hightlightedGutter] = hightlightRightLine, _j))}/>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_f = {}, _f[styles.diffAdded] = added, _f[styles.hightlightedGutter] = hightlightRightLine, _f))}/>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_m = {}, _m[styles.diffAdded] = added, _m[styles.hightlightedGutter] = hightlightRightLine, _m))}/>
                <td className={classnames_1.default(styles.marker, (_n = {}, _n[styles.diffAdded] = added, _n[styles.hightlightedLine] = hightlightRightLine, _n))}/>
                <td className={classnames_1.default(styles.marker, (_g = {}, _g[styles.diffAdded] = added, _g[styles.hightlightedLine] = hightlightRightLine, _g))}/>
                <td className={classnames_1.default(styles.defaultTdClass, (_h = {}, _h[styles.diffAdded] = added, _h[styles.hightlightedLine] = hightlightRightLine, _h))}/>
            </tr>
        )}
        {!showMaxChunk && !showMaxChunkToggle && drawChunk && (
            <tr className={styles.line} ref={(el) => {
                if (el) {
                    el.style.setProperty('background-color', "#f1f8ff", 'important');
                }
            }}>
                <td className={classnames_1(styles.gutter, styles.leftGutter, {[styles.diffRemoved]: removed, [styles.hightlightedGutter]: hightlightLeftLine})} style={{textAlign: "center", verticalAlign: "middle", paddingBottom: 0}} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>
                        <CollapsedChunkHandler added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} handleCollapseLength={handleCollapseLength} recordReqId={recordReqId} replayReqId={replayReqId}></CollapsedChunkHandler>
                    </pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.leftGutter, (_k = {}, _k[styles.diffRemoved] = removed, _k[styles.hightlightedGutter] = hightlightLeftLine, _k))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                </td>
                <td className={classnames_1.default(styles.marker, (_l = {}, _l[styles.diffRemoved] = removed, _l[styles.hightlightedLine] = hightlightLeftLine, _l))}/>
                <td className={classnames_1.default(styles.marker, (_d = {}, _d[styles.diffRemoved] = removed, _d[styles.hightlightedLine] = hightlightLeftLine, _d))}>
                </td>
                <td className={classnames_1.default(styles.defaultTdClass, (_e = {}, _e[styles.diffRemoved] = removed, _e[styles.hightlightedLine] = hightlightLeftLine, _e))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>@@ Collapsed @@</pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_j = {}, _j[styles.hightlightedGutter] = hightlightRightLine, _j))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_f = {}, _f[styles.diffAdded] = added, _f[styles.hightlightedGutter] = hightlightRightLine, _f))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>
                        <CollapsedChunkHandler added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} handleCollapseLength={handleCollapseLength} recordReqId={recordReqId} replayReqId={replayReqId}></CollapsedChunkHandler>
                    </pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_m = {}, _m[styles.diffAdded] = added, _m[styles.hightlightedGutter] = hightlightRightLine, _m))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}/>
                <td className={classnames_1.default(styles.marker, (_n = {}, _n[styles.diffAdded] = added, _n[styles.hightlightedLine] = hightlightRightLine, _n))}/>
                <td className={classnames_1.default(styles.marker, (_g = {}, _g[styles.diffAdded] = added, _g[styles.hightlightedLine] = hightlightRightLine, _g))}/>
                <td className={classnames_1.default(styles.defaultTdClass, (_h = {}, _h[styles.diffAdded] = added, _h[styles.hightlightedLine] = hightlightRightLine, _h))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>@@ Collapsed @@</pre>
                </td>
            </tr>
        )}
        {showMaxChunk === true && (
            <tr className={styles.line} ref={(el) => {
                if (el) {
                    el.style.setProperty('background-color', "#f1f8ff", 'important');
                }
            }}>
                <td className={classnames_1(styles.gutter, styles.leftGutter, {[styles.diffRemoved]: removed, [styles.hightlightedGutter]: hightlightLeftLine})} style={{textAlign: "center", verticalAlign: "middle", paddingBottom: 0}} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>
                        <MaxChunkHandler added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} handleMaxLinesLength={handleMaxLinesLength} recordReqId={recordReqId} replayReqId={replayReqId}></MaxChunkHandler>
                    </pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.leftGutter, (_k = {}, _k[styles.diffRemoved] = removed, _k[styles.hightlightedGutter] = hightlightLeftLine, _k))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                </td>
                <td className={classnames_1.default(styles.marker, (_l = {}, _l[styles.diffRemoved] = removed, _l[styles.hightlightedLine] = hightlightLeftLine, _l))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}/>
                <td className={classnames_1.default(styles.marker, (_d = {}, _d[styles.diffRemoved] = removed, _d[styles.hightlightedLine] = hightlightLeftLine, _d))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}>
                </td>
                <td className={classnames_1.default(styles.defaultTdClass, (_e = {}, _e[styles.diffRemoved] = removed, _e[styles.hightlightedLine] = hightlightLeftLine, _e))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>@@ Max. Lines @@</pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_j = {}, _j[styles.hightlightedGutter] = hightlightRightLine, _j))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_f = {}, _f[styles.diffAdded] = added, _f[styles.hightlightedGutter] = hightlightRightLine, _f))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>
                        <MaxChunkHandler added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} handleMaxLinesLength={handleMaxLinesLength} recordReqId={recordReqId} replayReqId={replayReqId}></MaxChunkHandler>
                    </pre>
                </td>
                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_m = {}, _m[styles.diffAdded] = added, _m[styles.hightlightedGutter] = hightlightRightLine, _m))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#dbedff", 'important');
                    }
                }}/>
                <td className={classnames_1.default(styles.marker, (_n = {}, _n[styles.diffAdded] = added, _n[styles.hightlightedLine] = hightlightRightLine, _n))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}/>
                <td className={classnames_1.default(styles.marker, (_g = {}, _g[styles.diffAdded] = added, _g[styles.hightlightedLine] = hightlightRightLine, _g))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}/>
                <td className={classnames_1.default(styles.defaultTdClass, (_h = {}, _h[styles.diffAdded] = added, _h[styles.hightlightedLine] = hightlightRightLine, _h))} ref={(el) => {
                    if (el) {
                        el.style.setProperty('background-color', "#f1f8ff", 'important');
                    }
                }}>
                    <pre style={{backgroundColor:"transparent"}}>@@ Max. Lines @@</pre>
                </td>
            </tr>
        )}
        {/* {(hasDiff || showDiff) && ( */}
        {!showMaxChunk && !showMaxChunkToggle && !collapseChunk && (
        
            <tr className={styles.line} onMouseOver = {() => showRefElement(actionsWrapperElementRef)} onMouseOut = {() => hideRefElement(actionsWrapperElementRef)} >
                {!hideLineNumbers && (
                    <td className = {classnames_1.default(styles.gutter, styles.leftGutter, (_c = {},
                        _c[styles.diffRemoved] = removed,
                        _c[styles.hightlightedGutter] = hightlightLeftLine,
                        _c))}  onClick = {onLineNumberClickProxy(onLineNumberClick, leftLineNumberPrefix + "-" + leftLineNumber)}>
                            {leftLineNumber && (
                                <LineNumber lineNumber={leftLineNumber} prefix={leftLineNumberPrefix}/>
                            )}
                    </td>
                )}

                <td className={classnames_1.default(styles.gutter, styles.leftGutter, (_k = {}, _k[styles.diffRemoved] = removed, _k[styles.hightlightedGutter] = hightlightLeftLine, _k))}>
                    {(!disableOperationSet || disableOperationSet !== true) && (<OperationSetLabel added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId}/>) }
                </td>

                <td className={classnames_1.default(styles.marker, (_l = {}, _l[styles.diffRemoved] = removed, _l[styles.hightlightedLine] = hightlightLeftLine, _l))}>
                    {(!disableOperationSet || disableOperationSet !== true) && (<OperationSet added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} elementRef={leftOperationSetElementRef} inputElementRef={inputElementRef} eventType={eventType} method={method}/>) }
                </td>

                <td className={classnames_1.default(styles.marker, (_d = {}, _d[styles.diffRemoved] = removed, _d[styles.hightlightedLine] = hightlightLeftLine, _d))}>
                    {removed && (
                        <pre>-</pre>
                    )}
                </td>

                <td className={classnames_1.default(styles.defaultTdClass, (_e = {}, _e[styles.diffRemoved] = removed, _e[styles.hightlightedLine] = hightlightLeftLine, _e))}>
                    {typeof leftContent === 'string' && (
                        renderContent 
                        ? renderContent(leftContent) 
                        : 
                        <pre style={{backgroundColor:"transparent"}}  onMouseOver= {() => showRefElement(leftOperationSetElementRef)} onMouseOut={() => hideRefElement(leftOperationSetElementRef)}>
                            {leftContent}
                            {/* <span>
                                <i className={hasChildren && hasDiff ? "far fa-minus-square" : hasChildren && showDiff ? "far fa-plus-square" : ""} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}}>{hasChildren && showDiff ? " " : ""}</i>
                            </span> */}
                        </pre> 
                    ) || leftContent}
                </td>

                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_j = {}, _j[styles.hightlightedGutter] = hightlightRightLine, _j))} >
                        { (added || removed) && (
                            <Resolutions className={classnames_1('')} added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId}/>
                        )}
                </td>
                
                {!hideLineNumbers && (
                    <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_f = {}, _f[styles.diffAdded] = added, _f[styles.hightlightedGutter] = hightlightRightLine, _f))} onClick={onLineNumberClickProxy(onLineNumberClick, rightLineNumberPrefix + "-" + rightLineNumber)}>
                        <LineNumber lineNumber={rightLineNumber} prefix={rightLineNumberPrefix}/>
                    </td>
                )}

                <td className={classnames_1.default(styles.gutter, styles.rightGutter, (_m = {}, _m[styles.diffAdded] = added, _m[styles.hightlightedGutter] = hightlightRightLine, _m))}>
                    {(!disableOperationSet || disableOperationSet !== true) && (<OperationSetLabel  added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} />) }
                </td>

                <td className={classnames_1.default(styles.marker, (_n = {}, _n[styles.diffAdded] = added, _n[styles.hightlightedLine] = hightlightRightLine, _n))}>
                    {(!disableOperationSet || disableOperationSet !== true) && (<OperationSet added={added} removed={removed} jsonPath={jsonPath} serverSideDiff={serverSideDiff} app={app} templateVersion={templateVersion} service={service} apiPath={apiPath} replayId={replayId} recordingId={recordingId} elementRef={rightOperationSetElementRef} inputElementRef={inputElementRef} eventType={eventType} method={method}/>) }
                </td>

                <td className={classnames_1.default(styles.marker, (_g = {}, _g[styles.diffAdded] = added, _g[styles.hightlightedLine] = hightlightRightLine, _g))}>
                    {added && (
                        <pre>+</pre>
                    )}
                </td>

                <td className={classnames_1.default(styles.defaultTdClass, (_h = {}, _h[styles.diffAdded] = added, _h[styles.hightlightedLine] = hightlightRightLine, _h))}>
                    {typeof rightContent === 'string'
                && (renderContent
                    ? renderContent(rightContent)
                    : <pre style={{backgroundColor:"transparent"}}  onMouseOver={() => showRefElement(rightOperationSetElementRef)} onMouseOut={() => hideRefElement(rightOperationSetElementRef)}>
                        {rightContent}
                            {/* <span>
                                <i className={hasChildren && hasDiff ? "far fa-minus-square" : hasChildren && showDiff ? "far fa-plus-square" : ""} style={{fontSize: "12px", marginRight: "12px", cursor: "pointer"}}>{hasChildren && showDiff ? " " : ""}</i>
                            </span> */}
                    </pre>
                ) || rightContent}
                </td>
            </tr>
        )}
        
    </React.Fragment>
    : <tr style={{display:"none"}}/>
};
