import React, { Fragment } from 'react';
import classnames from "classnames";
import "./Table.css";



const tableHeaders = {
    "keyName": "Key",
    "type": "Type",
    "presence": "Is Required?",
    "transformation": "Transformation",
    "comparison":"Comparison"
};

const Table = (props) => {
    const { value } = props;

    const accessors = Object.keys(tableHeaders);

    const headerClassName = (header) => 
        classnames(
            "gv-table-cell", 
            "gv-table-cell-response", 
            "gv-table-header", 
            { "gv-table-key": header === "keyName"}
        );

    const columnClassName = (accessor) => classnames(
        "gv-table-cell",
        "gv-table-cell-response",
        { "gv-table-key": accessor === "keyName"}
    );

    const header = () => (
        <div className="gv-table-row">
            {accessors.map(
                header => 
                    <div key={header} className={headerClassName(header)}>
                        {tableHeaders[header]}
                    </div>
                )
            }
        </div>
    );

    const renderColumns = (item) => {
        return accessors.map(accessor => <div className={columnClassName(accessor)}>{item[accessor]}</div>)
    };

    const renderRows = () => (value.map(item => <div className="gv-table-row">{renderColumns(item)}</div>));

    const body = () => (
        <Fragment>
            {
                value.length === 0 
                ? <span className="gv-table-empty">No Insights Found</span> 
                : renderRows()
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

export default Table;