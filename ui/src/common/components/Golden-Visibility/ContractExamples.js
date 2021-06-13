/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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