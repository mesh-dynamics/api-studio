import React, { Component, useEffect, useState } from 'react'

import { ListGroup, ListGroupItem } from 'react-bootstrap';
import classNames from 'classnames';

export interface IMultiselectProps{
    value: string;
    title: string;
    placeholder: string;
    options: any[];
    onChange: (value: string) => void;
}

export default function (props: IMultiselectProps) {

    const [value, setValue] = useState(props.value);
    const [options, setOptions] = useState(props.options);
    const [filterText, setFilterText] = useState('');

    useEffect(() => {
        setOptions(props.options);
        setFilterText('');
    }, [props.options]);
    useEffect(()=>{
        setValue(props.value);
    },[props.value]);
    const onItemClick = (event: React.MouseEvent<ListGroup, MouseEvent>) => {
        const target = (event.target as HTMLLIElement);
        if (!target.classList.contains('disabled')) {
            const value = target.getAttribute('value');
            setValue(value!);
            props.onChange && props.onChange(value!);
        }
    }

    const onFilterTextChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setFilterText(event.currentTarget.value);
        setOptions(props.options.filter(u => u.val.toLowerCase().indexOf(event.currentTarget.value) != -1));
    }
    const mutliSelectClass = classNames({
        'multi-select api-catalog-bordered-box': true,
      });

    return (<div className={mutliSelectClass}>

        <p className="api-catalog-box-title">{props.title}</p>
        <div className="input-group">
            <input type="text" value={filterText}
                className="filter form-control"
                onChange={onFilterTextChange}
                disabled={props.options.length == 0}
                placeholder={props.placeholder} />

        </div>
        <ListGroup onClick={onItemClick}>
            {props.options.length == 0 ? <>
                <ListGroupItem disabled>&nbsp;</ListGroupItem>
                <ListGroupItem disabled>&nbsp;</ListGroupItem>
                <ListGroupItem disabled>&nbsp;</ListGroupItem>
            </>
            :            
            <ListGroupItem key="All" value="" active={value == ""}>All</ListGroupItem>}
            {options.map(u =>
                <ListGroupItem key={u.val} value={u.val} active={u.val == value}>{u.val}</ListGroupItem>
            )}
        </ListGroup>
    </div>)


} 