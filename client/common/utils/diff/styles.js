"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) if (e.indexOf(p[i]) < 0)
            t[p[i]] = s[p[i]];
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
var emotion_1 = require("emotion");
exports.default = (function (styleOverride) {
    var _a, _b;
    var overrideVariables = styleOverride.variables, styles = __rest(styleOverride, ["variables"]);
    var variables = __assign({
        addedBackground: '#e6ffed',
        addedColor: '#24292e',
        removedBackground: '#ffeef0',
        removedColor: '#24292e',
        wordAddedBackground: '#acf2bd',
        wordRemovedBackground: '#fdb8c0',
        addedGutterBackground: '#cdffd8',
        removedGutterBackground: '#ffdce0',
        gutterBackground: '#f7f7f7',
        gutterBackgroundDark: '#f3f1f1',
        highlightBackground: '#fffbdd',
        highlightGutterBackground: '#fff5b1',
        // author raj.maddireddy@cubecorp.io
        addedMinWidth: '450px',
        addedMaxWidth: '450px',
        removedMinWidth: '450px',
        removedMaxWidth: '450px',
        backgroundColor: 'transparent',
    }, overrideVariables);
    var diffContainer = emotion_1.css({
        width: '100%',
        'pre': {
            margin: 0,
            whiteSpace: 'pre-wrap',
            lineHeight: '25px',
        },
        'tbody': {
            tr: {
                '&:first-child': {
                    td: {
                        paddingTop: 15,
                    },
                },
                '&:last-child': {
                    td: {
                        paddingBottom: 15,
                    },
                },
            },
        },
        label: 'diff-container',
        border: '1px solid #eee'
    });
    var diffRemoved = emotion_1.css({
        background: variables.removedBackground,
        color: variables.removedColor,
        pre: {
            color: variables.removedColor,
            // author raj.maddireddy@cubecorp.io
            backgroundColor: variables.backgroundColor
        },
        label: 'diff-removed',
        // author raj.maddireddy@cubecorp.io
        minWidth: variables.removedMinWidth,
        maxWidth: variables.removedMaxWidth,
    });
    var diffAdded = emotion_1.css({
        background: variables.addedBackground,
        color: variables.addedColor,
        pre: {
            color: variables.addedColor,
            // author raj.maddireddy@cubecorp.io
            backgroundColor: variables.backgroundColor
        },
        label: 'diff-added',
        // author raj.maddireddy@cubecorp.io
        minWidth: variables.addedMinWidth,
        maxWidth: variables.addedMaxWidth,
    });
    var marker = emotion_1.css((_a = {
            minWidth: 50,
            paddingLeft: 10,
            userSelect: 'none',
            label: 'marker'
        },
        _a["&." + diffAdded] = {
            pre: {
                color: variables.addedColor,
            }
        },
        _a["&." + diffRemoved] = {
            pre: {
                color: variables.removedColor,
            }
        },
        _a));
    var hightlightedLine = emotion_1.css({
        background: variables.highlightBackground,
        label: 'hightlighted-line',
    });
    var hightlightedGutter = emotion_1.css({
        label: 'hightlighted-gutter',
    });
    var gutter = emotion_1.css((_b = {
            userSelect: 'none',
            minWidth: 40,
            padding: '0 10px',
            label: 'gutter',
            cursor: 'pointer',
            textAlign: 'right',
            background: variables.gutterBackground,
            '&:hover': {
                background: variables.gutterBackgroundDark,
                pre: {
                    opacity: 1,
                },
            },
            pre: {
                opacity: 0.5,
            }
        },
        _b["&." + diffAdded] = {
            background: variables.addedGutterBackground,
        },
        _b["&." + diffRemoved] = {
            background: variables.removedGutterBackground,
        },
        _b["&." + hightlightedGutter] = {
            background: variables.highlightGutterBackground,
            '&:hover': {
                background: variables.highlightGutterBackground,
            },
        },
        _b));
    var line = emotion_1.css({
        verticalAlign: 'baseline',
        label: 'line',
    });
    var wordDiff = emotion_1.css({
        padding: 2,
        display: 'inline-flex',
        borderRadius: 1,
        label: 'word-diff',
        // author raj.maddireddy@cubecorp.io
        backgroundColor: variables.backgroundColor
    });
    var wordAdded = emotion_1.css({
        background: variables.wordAddedBackground,
        label: 'word-added',
    });
    var wordRemoved = emotion_1.css({
        background: variables.wordRemovedBackground,
        label: 'word-removed',
    });
    var leftGutter = emotion_1.css({
        label: 'left-gutter',
    });
    var rightGutter = emotion_1.css({
        label: 'right-gutter',
    });
    var defaultStyles = {
        diffContainer: diffContainer,
        diffRemoved: diffRemoved,
        diffAdded: diffAdded,
        marker: marker,
        hightlightedGutter: hightlightedGutter,
        hightlightedLine: hightlightedLine,
        gutter: gutter,
        line: line,
        wordDiff: wordDiff,
        wordAdded: wordAdded,
        wordRemoved: wordRemoved,
        leftGutter: leftGutter,
        rightGutter: rightGutter,
    };
    var computerOverrideStyles = Object.keys(styles)
        .reduce(function (acc, key) {
        var _a;
        return (__assign({}, acc, (_a = {},
            _a[key] = emotion_1.css(styles[key]),
            _a)));
    }, {});
    return Object.keys(defaultStyles)
        .reduce(function (acc, key) {
        var _a;
        return (__assign({}, acc, (_a = {},
            _a[key] = computerOverrideStyles[key]
                ? emotion_1.cx(defaultStyles[key], computerOverrideStyles[key])
                : defaultStyles[key],
            _a)));
    }, {});
});
