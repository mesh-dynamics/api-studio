import React, { Fragment } from 'react';
import "./Table.css";

const RequestTable = (props) => {
    const { value } = props;

    const header = () => (
        <div className="gv-table-row gv-table-header">
            <div className="gv-table-cell gv-table-cell-key">Key</div>
            <div className="gv-table-cell gv-table-cell-value">Type</div>
        </div>
    );

    const renderRows = () => (
        Object.keys(value).map(key => 
            <div className="gv-table-row" key={key}>
                <div className="gv-table-cell gv-table-cell-key">{key}</div>
                <div className="gv-table-cell gv-table-cell-value">{value[key]}</div>
            </div>
        )
    );

    const body = () => (
        <Fragment>
           {
            Object.keys(value).length !== 0 
            ? renderRows()
            : null
           }
        </Fragment>
    );

    return (
        <div className="gv-table-root">
            {header()}
            {body()}
        </div>
    )
}

export default RequestTable;
