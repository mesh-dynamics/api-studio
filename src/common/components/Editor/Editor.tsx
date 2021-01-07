import React from "react";
import { Controlled as ControlledCodeMirror } from "react-codemirror2";
import "codemirror/lib/codemirror.css";
import "codemirror/theme/material.css";
import "codemirror/mode/javascript/javascript.js";
import "codemirror/mode/htmlmixed/htmlmixed.js";
import "codemirror/mode/xml/xml";
import "codemirror/addon/fold/foldgutter.js";
import "codemirror/addon/fold/foldgutter.css";
import "codemirror/addon/fold/brace-fold";
import "codemirror/addon/lint/json-lint";
import "codemirror/addon/lint/javascript-lint";
import "codemirror/addon/lint/lint";
import "codemirror/addon/lint/lint.css";
import jsonlint from "jsonlint-mod";
(window as any).jsonlint = jsonlint;

import "codemirror/addon/fold/markdown-fold";
import "codemirror/addon/fold/foldcode";
import "../../helpers/codemirror";

//optional edits:
import "codemirror/addon/merge/merge.js";
import "codemirror/addon/merge/merge.css";

export function getCodeMirrorMode(language: string) {
  switch (language) {
    case "json":
    case "javascript":
      return { name: "javascript", json: true };
    case "html":
      return "htmlmixed";
    case "xml":
      return "xml";
    default:
      return { name: "javascript", json: true };
  }
}

export interface IEditorProps {
  onChange?: (value: string) => void;
  value: string;
  language: string;
  readonly?: boolean;
  getEditorRef?: (editor: any) => void;
}

const Editor = function (props: IEditorProps) {
  const codeMirrorRef = React.useRef(null);
  const onBeforeChange = React.useCallback(
    (editor, data, value) => {
      !props.readonly && props.onChange && props.onChange(value);
    },
    [props.onChange]
  );

  if (props.getEditorRef && codeMirrorRef && codeMirrorRef.current) {
    const editor = (codeMirrorRef.current as any).editor;
    props.getEditorRef(editor);
  }

  return (
    <ControlledCodeMirror
      key="lhsData"
      value={props.value}
      onBeforeChange={onBeforeChange}
      options={{
        mode: getCodeMirrorMode(props.language),
        lineWrapping: true,
        foldGutter: true,
        
        gutters: [
          "CodeMirror-linenumbers",
          "CodeMirror-foldgutter",
          "CodeMirror-lint-markers",
        ],
        lint: true,
        lineNumbers: true,
      }}
      ref={codeMirrorRef}
    />
  );
};
export default Editor;
