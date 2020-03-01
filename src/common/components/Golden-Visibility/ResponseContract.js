import React, { Fragment, useState } from "react";
import PropTypes from "prop-types";
import ViewType from "./ViewType";
import Table from "./Table";
import { 
    VIEW_TYPE, 
    RESPONSE_TABS 
} from "../../utils/enums/golden-visibility";

const ResponseContract = (props) => {
    
    const [responseViewType, setResponseViewType] = useState(VIEW_TYPE.TABLE);

    const { handleExampleClick, responseContract: { body: { table, json }} } = props;

    return (
        <Fragment>
            <div className="gv-section-banner">
                <span>Response</span>
                <span onClick={() => handleExampleClick(RESPONSE_TABS.EXAMPLES)} className="gv-link">Examples</span>
            </div>
            <div className="gv-section-details">
                <div className="gv-tab-container">
                    <div className="gv-tab-button-selected gv-tab-single">Body</div>
                </div>
                <div className="gv-contract-container">
                    <ViewType viewType={responseViewType} setViewType={setResponseViewType} />
                    {
                        responseViewType === VIEW_TYPE.TABLE && 
                        <Table value={table} />
                    }
                    {
                        responseViewType === VIEW_TYPE.JSON &&
                        <div className="gv-textarea-container">
                            <textarea 
                                readOnly 
                                rows="15" 
                                cols="40"
                                className="gv-textarea" 
                                value={
                                    Object.keys(json).length !== 0 
                                    ? JSON.stringify(json, undefined, 4)
                                    : ""
                                } 
                            ></textarea>
                        </div>
                    }
                </div>                
            </div>
        </Fragment>
    );
}

ResponseContract.propTypes = {
    responseContract: PropTypes.object.isRequired,
    handleExampleClick: PropTypes.func.isRequired,
};


export default ResponseContract;
