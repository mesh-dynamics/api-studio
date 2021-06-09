import React, { Fragment, useState, useEffect } from 'react';
import RequestContract from "./RequestContract";
import ResponseContract from "./ResponseContract";

const GoldenContract = (props) => {
    return (
        <Fragment>
            <RequestContract {...props} />
            <ResponseContract {...props} />
        </Fragment>
    );
}

export default GoldenContract;