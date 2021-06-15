import React, { ChangeEvent, useCallback, useEffect, useState } from 'react';
import { Button, FormControl } from 'react-bootstrap';
import { v4 as uuid } from 'uuid';
import _ from 'lodash';
import { IValueConfig } from './EditTestConfig';


interface IMultiLineInputComponentProps {
    value: IValueConfig[],
    onChange: (value: IValueConfig[]) => void,
    name: string,
    listId?: string
}


export function MultiLineInputComponent(props: IMultiLineInputComponentProps) {

    const onChange = useCallback(
        (event: ChangeEvent<HTMLInputElement & FormControl>) => {
            const uniqueId = event.target.getAttribute("data-key");
            const updatedValue = props.value.map(item => item.uniqueId == uniqueId ? { uniqueId: item.uniqueId, value: event.target.value } : item
            );
            props.onChange(updatedValue);
        },
        [props.value]
    );
    const onDelete = useCallback(
        (event: React.MouseEvent<HTMLElement>) => {
            const uniqueId = (event.target as HTMLElement).getAttribute("data-key");
            _.remove(props.value, (itemValue) => itemValue.uniqueId == uniqueId);
            props.onChange([...props.value]);
        },
        [props.value]
    );
    const onAdd = useCallback(
        (event: React.MouseEvent<Button>) => {
            const uniqueId = uuid();
            const updatedValue = [...props.value, { uniqueId, value: "" }];
            props.onChange(updatedValue);
        },
        [props.value]
    );
    return (
        <div>
            {props.value.map((item, index) => <div className="inline-action margin-bottom-10" key={index}>

                <FormControl as="input" name={"item" + index} data-key={item.uniqueId} value={item.value} onChange={onChange} {...(props.listId ? ({ list: props.listId }) : null)} />

                <span style={{ width: '20px' }}><i className="fas fa-trash pointer" data-key={item.uniqueId} onClick={onDelete} /></span>
            </div>
            )}

            <Button type="button" bsSize="sm" bsStyle="info" onClick={onAdd} className="add-item pointer"><i className="fas fa-plus" /> Add</Button>
        </div>
    );
}
