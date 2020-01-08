import React, { Fragment } from 'react';
import "./Table.css";



const tableHeaders = {
    "keyName": "Key",
    "type": "Type",
    "assertions": "Assertions",
    "presence": "Is Required?",
    "transformation": "Transformation",
    "comparison":"Comparison"
};

const ResponseTable = (props) => {
    const { value } = props;

    const accessors = Object.keys(tableHeaders);

    const header = () => (
        <div className="gv-table-row">
            {accessors.map(header => <div key={header} className="gv-table-cell gv-table-cell-response gv-table-header">{tableHeaders[header]}</div>)}
        </div>
    );

    const renderColumns = (item) => {
        return accessors.map(accessor => <div className="gv-table-cell gv-table-cell-response">{item[accessor]}</div>)
    };

    const renderRows = () => (value.map(item => <div className="gv-table-row">{renderColumns(item)}</div>));

    const body = () => (
        <Fragment>
           {renderRows()}
        </Fragment>
    );

    return (
        <div className="gv-table-root">
            {header()}
            {body()}
        </div>
    )
}

export default ResponseTable;