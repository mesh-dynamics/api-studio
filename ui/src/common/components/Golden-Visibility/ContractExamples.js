import React, { Fragment } from "react";

const ContractExamples = (props) => {
    
    const { pageNumbers, handleExamplesBackClick, selectedPageNumber, setSelectedPageNumber, examples } = props;
    
    return (
        <Fragment>
            <div className="gv-section-banner">
                <span className="gv-link" onClick={handleExamplesBackClick}>Back</span>    
            </div>
            <div className="gv-pagination-control">
                {
                    pageNumbers.map(number => 
                        <button 
                            key={number} 
                            onClick={() => setSelectedPageNumber(number)}
                            className={
                                selectedPageNumber === number 
                                ? "gv-pagination-button-selected"
                                : "gv-pagination-button"}
                        >
                            {number}
                        </button>
                    )
                }
            </div>
            <div className="gv-textarea-container">
                <textarea 
                    readOnly 
                    rows="30" 
                    cols="40"
                    className="gv-textarea" 
                    value={JSON.stringify(examples, undefined, 4)} 
                ></textarea>
            </div>
        </Fragment>
    );
}

export default ContractExamples;