"use strict";

var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
var React = require("react");
var diff = require("diff");
var PropTypes = require("prop-types");
var classnames_1 = require("classnames");
var m = require('memoize-one');
var memoize = m.default || m;
var styles_1 = require("./styles");
var line_1 = require("./line");
var wordDiff = function (oldValue, newValue, hideType, styles, renderContent) {
    var charDiff = diff.diffWordsWithSpace(oldValue, newValue);
    return charDiff.map(function (obj, i) {
        var _a, _b;
        if (obj[hideType])
            return undefined;
        if (renderContent) {
            return React.createElement("span", { className: classnames_1.default(styles.wordDiff, (_a = {},
                    _a[styles.wordAdded] = obj.added,
                    _a[styles.wordRemoved] = obj.removed,
                    _a)), key: i }, renderContent(obj.value));
        }
        return React.createElement("pre", { className: classnames_1.default(styles.wordDiff, (_b = {},
                _b[styles.wordAdded] = obj.added,
                _b[styles.wordRemoved] = obj.removed,
                // author raj.maddireddy@cubecorp.io
                // \u200C (&zwnj;) used it to add an invisible character
                // http://htmlhelp.org/reference/html40/entities/special.html
                _b)), key: i }, obj.value.trim() ? obj.value : obj.value + "\u200C");
    });
};
var DiffViewer = /** @class */ (function (_super) {
    __extends(DiffViewer, _super);
    function DiffViewer() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.splitView = function (filterPaths, diffArray, styles, inputElementRef, showAll, searchFilterPath, disableOperationSet, handleCollapseLength, handleMaxLinesLength) {
            var leftLineNumber = 0;
            var rightLineNumber = 0;
            return function () { return diffArray.map(function (obj, i) {
                return React.createElement(React.Fragment, { key: i }, obj.value.split('\n')
                    .filter(function (ch) { return ch.length > 0; })
                    .map(function (ch, num) {
                    if (!obj.added && !obj.removed) {
                        rightLineNumber = rightLineNumber + 1;
                        leftLineNumber = leftLineNumber + 1;
                        // author raj.maddireddy@cubecorp.io
                        return React.createElement(line_1.DefaultLine, { styles: styles, hideLineNumbers: _this.props.hideLineNumbers, leftLineNumber: leftLineNumber, rightLineNumber: rightLineNumber, leftContent: ch, rightContent: ch, key: num, hightlightLines: _this.props.highlightLines, renderContent: _this.props.renderContent, onLineNumberClick: _this.props.onLineNumberClick, serverSideDiff: obj.serverSideDiff, jsonPath: obj.jsonPath, filterPaths: filterPaths, inputElementRef: inputElementRef, replayId: obj.replayId, recordingId: obj.recordingId,  apiPath: obj.apiPath, templateVersion: obj.templateVersion, showAll, searchFilterPath, disableOperationSet, hasChildren: obj.hasChildren, collapseChunk: obj.collapseChunk, drawChunk: obj.drawChunk, hasDiff: obj.hasDiff, showDiff: obj.showDiff, handleCollapseLength, recordReqId: obj.recordReqId, replayReqId: obj.replayReqId, eventType: obj.eventType, showMaxChunk: obj.showMaxChunk, handleMaxLinesLength, showMaxChunkToggle: obj.showMaxChunkToggle, });
                    }
                    var leftContent;
                    var rightContent;
                    var removed = obj.removed;
                    var added = obj.added;
                    // author: siddhant.mutha@meshdynamics.io
                    // using isLastRemoved to handle the alignment of two objects
                    // avoid skipping the first line of the right side (added) object
                    if (obj.added && diffArray[i - 1] && diffArray[i - 1].removed && !diffArray[i-1].isLastRemoved) {
                        var preValueCount = diffArray[i - 1].count;
                        if (num <= (preValueCount - 1))
                            return undefined;
                        rightLineNumber = rightLineNumber + 1;
                        rightContent = ch;
                    }
                    // author: siddhant.mutha@meshdynamics.io
                    // using isLastRemoved to handle the alignment of two objects
                    // show only the left side line if this is the last line of the first object 
                    else if (obj.removed && diffArray[i + 1] && (!diffArray[i + 1].added || obj.isLastRemoved)) {
                        leftLineNumber = leftLineNumber + 1;
                        leftContent = ch;
                    }
                    else if (obj.removed && diffArray[i + 1] && diffArray[i + 1].added) {
                        leftLineNumber = leftLineNumber + 1;
                        var nextVal = diffArray[i + 1].value
                            .split('\n')
                            .filter(Boolean)[num];
                        leftContent = (nextVal && !_this.props.disableWordDiff)
                            ? wordDiff(ch, nextVal, 'added', styles, _this.props.renderContent)
                            : ch;
                        rightContent = (nextVal && !_this.props.disableWordDiff)
                            ? wordDiff(ch, nextVal, 'removed', styles, _this.props.renderContent)
                            : nextVal;
                        if (nextVal) {
                            rightLineNumber = rightLineNumber + 1;
                            added = true;
                        }
                    }
                    else {
                        rightLineNumber = rightLineNumber + 1;
                        rightContent = ch;
                    }
                    // author raj.maddireddy@cubecorp.io
                    return React.createElement(line_1.DefaultLine, { styles: styles, leftLineNumber: !removed || leftLineNumber, rightLineNumber: !added || rightLineNumber, removed: removed, added: added, key: num, hideLineNumbers: _this.props.hideLineNumbers, hightlightLines: _this.props.highlightLines, renderContent: _this.props.renderContent, leftContent: leftContent, rightContent: rightContent, onLineNumberClick: _this.props.onLineNumberClick, serverSideDiff: obj.serverSideDiff, jsonPath: obj.jsonPath, filterPaths: filterPaths, inputElementRef: inputElementRef, replayId: obj.replayId, recordingId: obj.recordingId,  apiPath: obj.apiPath, templateVersion: obj.templateVersion, showAll, searchFilterPath, disableOperationSet, hasChildren: obj.hasChildren, collapseChunk: obj.collapseChunk, drawChunk: obj.drawChunk, hasDiff: obj.hasDiff, showDiff: obj.showDiff, handleCollapseLength, recordReqId: obj.recordReqId, replayReqId: obj.replayReqId, eventType: obj.eventType, showMaxChunk: obj.showMaxChunk, handleMaxLinesLength, showMaxChunkToggle: obj.showMaxChunkToggle, });
                }));
            }); };
        };
        _this.inlineView = function (filterPaths, diffArray, styles) {
            var leftLineNumber = 0;
            var rightLineNumber = 0;
            return function () {
                return diffArray.map(function (diffObj, i) {
                    return diffObj.value.split('\n')
                        .filter(function (ch) { return ch.length > 0; })
                        .map(function (ch, num) {
                        var content;
                        if (diffObj.added) {
                            rightLineNumber = rightLineNumber + 1;
                            if (diffArray[i - 1] && diffArray[i - 1].removed) {
                                var preValue = diffArray[i - 1].value
                                    .split('\n')
                                    .filter(Boolean)[num];
                                content = (preValue && !_this.props.disableWordDiff) ? wordDiff(preValue, ch, 'removed', styles, _this.props.renderContent) : ch;
                            }
                            else {
                                content = ch;
                            }
                        }
                        else if (diffObj.removed) {
                            leftLineNumber = leftLineNumber + 1;
                            if (diffArray[i + 1] && diffArray[i + 1].added) {
                                var nextVal = diffArray[i + 1].value
                                    .split('\n')
                                    .filter(Boolean)[num];
                                content = (nextVal && !_this.props.disableWordDiff) ? wordDiff(ch, nextVal, 'added', styles, _this.props.renderContent) : ch;
                            }
                            else {
                                content = ch;
                            }
                        }
                        else {
                            rightLineNumber = rightLineNumber + 1;
                            leftLineNumber = leftLineNumber + 1;
                            content = ch;
                        }
                        return React.createElement(line_1.InlineLine, { styles: styles, onLineNumberClick: _this.props.onLineNumberClick, key: num, hideLineNumbers: _this.props.hideLineNumbers, renderContent: _this.props.renderContent, removed: diffObj.removed, leftLineNumber: diffObj.added || leftLineNumber, rightLineNumber: diffObj.removed || rightLineNumber, content: content, hightlightLines: _this.props.highlightLines, added: diffObj.added, serverSideDiff: diffObj.serverSideDiff, jsonPath: diffObj.jsonPath, filterPaths: filterPaths });
                    });
                });
            };
        };
        _this.computeStyles = memoize(styles_1.default);
        _this.render = function () {
            var _a = _this.props, oldValue = _a.oldValue, newValue = _a.newValue, splitView = _a.splitView, 
            inputElementRef = _a.inputElementRef, disableOperationSet = _a.disableOperationSet, handleCollapseLength = _a.handleCollapseLength, handleMaxLinesLength = _a.handleMaxLinesLength;
            // author raj.maddireddy@cubecorp.io
            var diffArray = _a.diffArray, enableClientSideDiff = _a.enableClientSideDiff;
            if (typeof oldValue !== 'string' || typeof newValue !== 'string') {
                throw Error('"oldValue" and "newValue" should be strings');
            }
            var newStyles = _this.computeStyles(_this.props.styles);
            // author raj.maddireddy@cubecorp.io
            if(enableClientSideDiff && !diffArray) {
                diffArray = [];
                console.error("SERVER SIDE DIFF IS EMPTY");
            }
            if(!diffArray) {
                console.error("DOING CLIENT SIDE DIFF");
                diffArray = diff.diffLines(oldValue, newValue, {
                    newlineIsToken: false,
                    ignoreWhitespace: false,
                    ignoreCase: false,
                });
            }
            let filterPaths = _a.filterPaths;
            let showAll = _a.showAll;
            let searchFilterPath = _a.searchFilterPath;
            if(!filterPaths) filterPaths = [];
            var nodes = splitView
                ? _this.splitView(filterPaths, diffArray, newStyles, inputElementRef, showAll, searchFilterPath, disableOperationSet, handleCollapseLength, handleMaxLinesLength)()
                : _this.inlineView(filterPaths, diffArray, newStyles, inputElementRef)();
            return (React.createElement("table", { className: newStyles.diffContainer },
                React.createElement("tbody", null, nodes)));
        };
        return _this;
    }
    DiffViewer.defaultProps = {
        oldValue: '',
        newValue: '',
        splitView: true,
        highlightLines: [],
        disableWordDiff: false,
        styles: {},
        hideLineNumbers: false,
    };
    DiffViewer.propTypes = {
        oldValue: PropTypes.string.isRequired,
        newValue: PropTypes.string.isRequired,
        splitView: PropTypes.bool,
        disableWordDiff: PropTypes.bool,
        renderContent: PropTypes.func,
        onLineNumberClick: PropTypes.func,
        styles: PropTypes.object,
        hideLineNumbers: PropTypes.bool,
        highlightLines: PropTypes.arrayOf(PropTypes.string),
    };
    return DiffViewer;
}(React.Component));
exports.default = DiffViewer;
