import React, { FC, useState, useEffect } from "react";

import "./EditableLabel.css";

declare type HandleEditComplete = (updatedLabelString: string) => void;

interface EditableLabelProps {
    label: string
    handleEditComplete: HandleEditComplete
}

const EMPTY_STRING: string = "";

const EditableLabel: FC<EditableLabelProps> = (props) => {
    let textInput: React.ReactHTMLElement<HTMLDivElement>;

    const { label, handleEditComplete } = props;

    const [allowEdit, setAllowEdit] = useState(false);

    const [labelString, setLabelString] = useState(label);

    const [showEditIcon, setShowEditIcon] = useState(false);

    const handleValueUpdate = () => {
        if (labelString === EMPTY_STRING) {
            setLabelString(label);
        } else {
            handleEditComplete(labelString);
        }
    }

    const handleBlur = () => {
        setAllowEdit(false);
        setShowEditIcon(false);
        handleValueUpdate();
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
        <div className="editable-label-root" onClick={() => setAllowEdit(true)}>
            {
                allowEdit
                    ?
                    <input
                        type="text"
                        value={labelString}
                        className="editable-input"
                        onBlur={handleBlur}
                        onKeyDown={handleKeyDown}
                        ref={ref => textInput = ref}
                        onChange={(event: React.KeyboardEvent<HTMLDivElement>) => setLabelString(event.target.value)}
                    />
                    :
                    <span
                        className="editable-label-text"
                        onMouseEnter={() => setShowEditIcon(true)}
                        onMouseLeave={() => setShowEditIcon(false)}
                    >
                        {label}
                        {showEditIcon && <i className="far fa-edit editable-label-icon"></i>}
                    </span>
            }
        </div>
    )
};

export default EditableLabel;
