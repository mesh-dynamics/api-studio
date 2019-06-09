"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var React = require("react");
var classnames_1 = require("classnames");
var rightLineNumberPrefix = 'R';
var leftLineNumberPrefix = 'L';
var onLineNumberClickProxy = function (onLineNumberClick, id) {
    return function (e) { return onLineNumberClick(id, e); };
};
var LineNumber = function (_a) {
    var prefix = _a.prefix, lineNumber = _a.lineNumber;
    return React.createElement("pre", { id: prefix + "-" + lineNumber }, lineNumber);
};
exports.InlineLine = function (_a) {
    var leftLineNumber = _a.leftLineNumber, rightLineNumber = _a.rightLineNumber, added = _a.added, removed = _a.removed, content = _a.content, _b = _a.onLineNumberClick, onLineNumberClick = _b === void 0 ? function () { } : _b, renderContent = _a.renderContent, _c = _a.hightlightLines, hightlightLines = _c === void 0 ? [] : _c, styles = _a.styles, hideLineNumbers = _a.hideLineNumbers;
    var _d, _e, _f, _g;
    var hightlightLine = (leftLineNumber !== true || rightLineNumber !== true)
        && (hightlightLines.includes(leftLineNumberPrefix + "-" + leftLineNumber)
            || hightlightLines.includes(rightLineNumberPrefix + "-" + rightLineNumber));
    return React.createElement("tr", { className: styles.line },
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
            : React.createElement("pre", null, content)));
};
exports.DefaultLine = function (_a) {
    var leftLineNumber = _a.leftLineNumber, rightLineNumber = _a.rightLineNumber, onLineNumberClick = _a.onLineNumberClick, rightContent = _a.rightContent, leftContent = _a.leftContent, added = _a.added, removed = _a.removed, renderContent = _a.renderContent, _b = _a.hightlightLines, hightlightLines = _b === void 0 ? [] : _b, styles = _a.styles, hideLineNumbers = _a.hideLineNumbers;
    var _c, _d, _e, _f, _g, _h;
    var hightlightLeftLine = leftLineNumber !== true
        && hightlightLines.includes(leftLineNumberPrefix + "-" + leftLineNumber);
    var hightlightRightLine = rightLineNumber !== true
        && hightlightLines.includes(rightLineNumberPrefix + "-" + rightLineNumber);
    return React.createElement("tr", { className: styles.line },
        !hideLineNumbers
            && React.createElement("td", { className: classnames_1.default(styles.gutter, styles.leftGutter, (_c = {},
                    _c[styles.diffRemoved] = removed,
                    _c[styles.hightlightedGutter] = hightlightLeftLine,
                    _c)), onClick: onLineNumberClickProxy(onLineNumberClick, leftLineNumberPrefix + "-" + leftLineNumber) }, leftLineNumber
                && React.createElement(LineNumber, { lineNumber: leftLineNumber, prefix: leftLineNumberPrefix })),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_d = {},
                _d[styles.diffRemoved] = removed,
                _d[styles.hightlightedLine] = hightlightLeftLine,
                _d)) }, removed
            && React.createElement("pre", null, "-")),
        React.createElement("td", { className: classnames_1.default((_e = {},
                _e[styles.diffRemoved] = removed,
                _e[styles.hightlightedLine] = hightlightLeftLine,
                _e)) }, typeof leftContent === 'string'
            && (renderContent
                ? renderContent(leftContent)
                : React.createElement("pre", {style: {backgroundColor: "transparent"}}, leftContent))
            || leftContent),
        !hideLineNumbers
            && React.createElement("td", { className: classnames_1.default(styles.gutter, styles.rightGutter, (_f = {},
                    _f[styles.diffAdded] = added,
                    _f[styles.hightlightedGutter] = hightlightRightLine,
                    _f)), onClick: onLineNumberClickProxy(onLineNumberClick, rightLineNumberPrefix + "-" + rightLineNumber) },
                React.createElement(LineNumber, { lineNumber: rightLineNumber, prefix: rightLineNumberPrefix })),
        React.createElement("td", { className: classnames_1.default(styles.marker, (_g = {},
                _g[styles.diffAdded] = added,
                _g[styles.hightlightedLine] = hightlightRightLine,
                _g)) }, added
            && React.createElement("pre", null, "+")),
        React.createElement("td", { className: classnames_1.default((_h = {},
                _h[styles.diffAdded] = added,
                _h[styles.hightlightedLine] = hightlightRightLine,
                _h)) }, typeof rightContent === 'string'
            && (renderContent
                ? renderContent(rightContent)
                : React.createElement("pre", {style: {backgroundColor: "transparent"}}, rightContent))
            || rightContent),
        /*React.createElement("td", { className: classnames_1.default(styles.marker, (_g = {},
                _g[styles.diffAdded] = added,
                _g[styles.hightlightedLine] = hightlightRightLine,
                _g)) }, added
            && React.createElement("span", {className: classnames_1('tag', 'is-warning')}, "OK Optional")),*/
    );
};
