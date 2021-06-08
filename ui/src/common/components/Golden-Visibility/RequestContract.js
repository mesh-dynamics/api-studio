import React, { Fragment, useState } from "react";
import PropTypes from "prop-types";
import RequestRules from "./RequestRules";
import {
    REQUEST_TABS,
    REQUEST_RULE
} from "../../utils/enums/golden-visibility";
import { resolveEndPoint } from "../../utils/lib/golden-utils";

const RequestContract = (props) => {
    const {
        selectedApi, 
        requestExamples: { method, hdrs },
        handleExampleClick,
        requestContract: { matchRules, compareRules },
    } = props;

    const endpoint = resolveEndPoint(hdrs, selectedApi);

    return (
        <Fragment>
            <div className="gv-section-banner">
                <span>Request</span>
                <span onClick={() => handleExampleClick(REQUEST_TABS.EXAMPLES)} className="gv-link">Examples</span>
            </div>
            <div className="gv-section-details">
                <div className="gv-request-container">
                    <div className="gv-request-verb">
                        {method || "[METHOD]"}
                    </div>
                    <div className="gv-request-path">
                        {endpoint}
                    </div>
                </div>
                {
                    matchRules && 
                    <RequestRules 
                        type={REQUEST_RULE.MATCH} 
                        rules={matchRules} 
                        
                    />
                }
                {
                    compareRules && 
                    <RequestRules 
                        type={REQUEST_RULE.COMPARE} 
                        rules={compareRules} 
                    />
                }
            </div>
        </Fragment>
    )
}

RequestContract.propTypes = {
    requestExamples: PropTypes.object.isRequired,
    requestContract: PropTypes.object.isRequired,
    handleExampleClick: PropTypes.func.isRequired,
};

export default RequestContract;
