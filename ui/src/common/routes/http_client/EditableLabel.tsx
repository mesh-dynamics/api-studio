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

import Tippy from "@tippy.js/react";
import classNames from "classnames";
import React, { FC, useState, useEffect, ChangeEvent } from "react";

import "./EditableLabel.css";

declare type HandleEditComplete = (updatedLabelString: string) => void;

interface EditableLabelProps {
    label: string
    handleEditComplete: HandleEditComplete;
    onEditCanceled? : Function,
    /** To remotely control the edit mode set this else default is undefined */
    allowEdit?: boolean;
    textClassName?: string
}

const EMPTY_STRING: string = "";

const EditableLabel: FC<EditableLabelProps> = (props) => {
    let textInput: HTMLDivElement | null = null;

    const { label, handleEditComplete } = props;

    const [allowEdit, setAllowEdit] = useState(props.allowEdit || false);
    useEffect(()=>{
        if(props.allowEdit != undefined){
            setAllowEdit(props.allowEdit);
        }
    }, [props.allowEdit]);

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
        props.onEditCanceled && props.onEditCanceled();
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
                    /> &nbsp;
                    <Tippy content="Press ENTER to save, ESC to reset" arrow={true} placement="bottom">
                        <span className="margin-right-15"><i className="fa fa-info-circle"></i></span>
                    </Tippy>
                    </>
                    :
                    <>
                    <span
                        className={classNames("editable-label-text", props.textClassName)}
                    >
                        {label} 
                    </span>
                    <span>{props.allowEdit == undefined && <i className="far fa-edit editable-label-icon"  onClick={() => setAllowEdit(true)}></i>}</span></>
            }
        </div>
    )
};

export default EditableLabel;
