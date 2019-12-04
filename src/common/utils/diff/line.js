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
    jsonPath = _a.jsonPath, serverSideDiff = _a.serverSideDiff, apiPath = _a.apiPath, service = _a.service, app = _a.app, templateVersion = _a.templateVersion, replayId = _a.replayId, recordingId = _a.recordingId;
    var filterPath = _a.filterPath,
    inputElementRef = _a.inputElementRef,
    showAll = _a.showAll,
    searchFilterPath = _a.searchFilterPath;
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

    return !jsonPath || (showAll || (filterPath.indexOf(jsonPath) > -1)) && (jsonPath.indexOf(searchFilterPath) > -1) ? React.createElement("tr", { className: styles.line, onMouseOver: () => showRefElement(actionsWrapperElementRef), onMouseOut: () => hideRefElement(actionsWrapperElementRef) },
        !hideLineNumbers
            && React.createElement("td", { className: classnames_1.default(styles.gutter, styles.leftGutter, (_c = {},
                    _c[styles.diffRemoved] = removed,
                    _c[styles.hightlightedGutter] = hightlightLeftLine,
                    _c)), onClick: onLineNumberClickProxy(onLineNumberClick, leftLineNumberPrefix + "-" + leftLineNumber) }, leftLineNumber
                && React.createElement(LineNumber, { lineNumber: leftLineNumber, prefix: leftLineNumberPrefix })),
        React.createElement("td", { className: classnames_1.default(styles.gutter, styles.leftGutter, (_k = {},
            _k[styles.diffRemoved] = removed,
             _k[styles.hightlightedGutter] = hightlightLeftLine,
            _k)) }, React.createElement(OperationSetLabel, {added, removed, jsonPath, serverSideDiff, app, templateVersion, service, apiPath, replayId, recordingId}, "")),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_l = {},
            _l[styles.diffRemoved] = removed,
            _l[styles.hightlightedLine] = hightlightLeftLine,
            _l)) }, React.createElement(OperationSet, {added, removed, jsonPath, serverSideDiff, app, templateVersion, service, apiPath, elementRef: leftOperationSetElementRef, inputElementRef: inputElementRef, replayId, recordingId}, "")),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_d = {},
                _d[styles.diffRemoved] = removed,
                _d[styles.hightlightedLine] = hightlightLeftLine,
                _d)) }, removed
            && React.createElement("pre", null, "-")),
        React.createElement("td", { className: classnames_1.default(styles.defaultTdClass, (_e = {},
                _e[styles.diffRemoved] = removed,
                _e[styles.hightlightedLine] = hightlightLeftLine,
                _e)) }, typeof leftContent === 'string'
            && (renderContent
                ? renderContent(leftContent)
                : React.createElement("pre", {style: {backgroundColor: "transparent"}, onMouseOver: () => showRefElement(leftOperationSetElementRef), onMouseOut: () => hideRefElement(leftOperationSetElementRef)}, leftContent))
            || leftContent),
        // author raj.maddireddy@cubecorp.io
        React.createElement("td", { className: classnames_1.default(styles.gutter, styles.rightGutter, (_j = {},
            _j[styles.hightlightedGutter] = hightlightRightLine,
            _j)) },
            (added || removed) && React.createElement(Resolutions, {className: classnames_1(''), added, removed, jsonPath, serverSideDiff, app, templateVersion, service, apiPath, replayId, recordingId}, ""
            )
        ),
        !hideLineNumbers
            && React.createElement("td", { className: classnames_1.default(styles.gutter, styles.rightGutter, (_f = {},
                    _f[styles.diffAdded] = added,
                    _f[styles.hightlightedGutter] = hightlightRightLine,
                    _f)), onClick: onLineNumberClickProxy(onLineNumberClick, rightLineNumberPrefix + "-" + rightLineNumber) },
                React.createElement(LineNumber, { lineNumber: rightLineNumber, prefix: rightLineNumberPrefix })),
        React.createElement("td", { className: classnames_1.default(styles.gutter, styles.rightGutter, (_m = {},
            _m[styles.diffAdded] = added,
            _m[styles.hightlightedGutter] = hightlightRightLine,
            _m)) }, React.createElement(OperationSetLabel, {added, removed, jsonPath, serverSideDiff, app, templateVersion, service, apiPath, replayId, recordingId}, "")),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_n = {},
            _n[styles.diffAdded] = added,
            _n[styles.hightlightedLine] = hightlightRightLine,
            _n)) }, React.createElement(OperationSet, {added, removed, jsonPath, serverSideDiff, app, templateVersion, service, apiPath, elementRef: rightOperationSetElementRef, inputElementRef: inputElementRef, replayId, recordingId}, "")),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_g = {},
                _g[styles.diffAdded] = added,
                _g[styles.hightlightedLine] = hightlightRightLine,
                _g)) }, added
            && React.createElement("pre", null, "+")),
        React.createElement("td", { className: classnames_1.default(styles.defaultTdClass, (_h = {},
                _h[styles.diffAdded] = added,
                _h[styles.hightlightedLine] = hightlightRightLine,
                _h))}, typeof rightContent === 'string'
            && (renderContent
                ? renderContent(rightContent)
                : React.createElement("pre", {style: {backgroundColor: "transparent"}, onMouseOver: () => showRefElement(rightOperationSetElementRef), onMouseOut: () => hideRefElement(rightOperationSetElementRef)}, rightContent))
            || rightContent),
        // author raj.maddireddy@cubecorp.io
        /* React.createElement("td", { className: classnames_1.default(styles.actions, (_i = {},
                _i[styles.diffAdded] = added,
                _i[styles.hightlightedLine] = hightlightRightLine,
                _i)) }, (leftContent || rightContent)
            && React.createElement(Actions, {added: added, removed: removed, jsonPath: jsonPath, serverSideDiff: serverSideDiff, elementRef: actionsWrapperElementRef}, "")
        ), */
    ) : React.createElement("tr", {style: {display: "none"}});
};
