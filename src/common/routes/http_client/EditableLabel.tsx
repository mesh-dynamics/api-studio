import Tippy from "@tippy.js/react";
import React, { FC, useState, useEffect, ChangeEvent } from "react";

import "./EditableLabel.css";

declare type HandleEditComplete = (updatedLabelString: string) => void;

interface EditableLabelProps {
    label: string
    handleEditComplete: HandleEditComplete
}

const EMPTY_STRING: string = "";

const EditableLabel: FC<EditableLabelProps> = (props) => {
    let textInput: HTMLDivElement | null = null;

    const { label, handleEditComplete } = props;

    const [allowEdit, setAllowEdit] = useState(false);

    const [labelString, setLabelString] = useState(label);

    const handleValueUpdate = () => {
        if (labelString === EMPTY_STRING || labelString === label) {
            setLabelString(label);
        } else {
            handleEditComplete(labelString);
        }
    }

    const handleBlur = () => {
        setAllowEdit(false);
        setLabelString(label);
    }

    const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
        if (event.key === 'Enter') {
            setAllowEdit(false);
            handleValueUpdate();
        }

        if (event.key === 'Escape') {
            setAllowEdit(false);
            setLabelString(label);
        }
    }

    useEffect(
        () => { textInput && textInput.focus() },
        [textInput, allowEdit]
    );

    return (
        <div className="editable-label-root">
            {
                allowEdit
                    ?
                    <><input
                        type="text"
                        value={labelString}
                        className="editable-input"
                        onBlur={handleBlur}
                        onKeyDown={handleKeyDown}
                        ref={ref => textInput = ref}
                        onChange={(event: ChangeEvent<HTMLInputElement>) => setLabelString((event.target as HTMLInputElement).value)}
                    /> 
                    <Tippy content="Press ENTER to save, ESC to reset" arrow={true} placement="bottom">
                        <span className="margin-right-15"><i className="fa fa-info-circle"></i></span>
                    </Tippy>
                    </>
                    :
                    <span
                        className="editable-label-text"
                    >
                        {label}
                        <i className="far fa-edit editable-label-icon"  onClick={() => setAllowEdit(true)}></i>
                    </span>
            }
        </div>
    )
};

export default EditableLabel;
