import React, { Component, useEffect, useState } from 'react'

import { ListGroup, Item, ListGroupItem } from 'react-bootstrap';
import classNames from 'classnames';

export default function (props) {

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
    const onItemClick = (e, a) => {
        if (!e.target.classList.contains('disabled')) {
            const value = e.target.getAttribute('value');
            setValue(value);
            props.onChange && props.onChange(value);
        }
    }

    const onFilterTextChange = (event) => {
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