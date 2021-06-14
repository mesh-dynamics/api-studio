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

import React, { useState, useEffect, Fragment } from 'react';
import { connect } from 'react-redux';

import GoldenContract from './GoldenContract';
import ContractExamples from './ContractExamples'
import {
    VIEW, 
    REQUEST_TABS, 
    RESPONSE_TABS,
} from "../../utils/enums/golden-visibility";
import './GoldenVisibility.css';

const pageNumbers = [1];

const GoldenVisibility = (props) => {
    const { 
        golden: { 
            isFetching,
            fetchComplete,
            selectedService, 
            selectedApi,
            requestContract,
            responseContract,
            requestExamples,
            responseExamples
        }
    } = props;

    const [currentView, setCurrentView] = useState(VIEW.GOLDEN_SUMMARRY);

    const [selectedPageNumber, setSelectedPageNumber] = useState(1);

    const handleExampleClick = (exampleType) => setCurrentView(exampleType);

    const handleExamplesBackClick = () => {
        setCurrentView(VIEW.GOLDEN_SUMMARRY);
        setSelectedPageNumber(1);
    };

    const renderContracts = () => ((selectedService && selectedApi) && (
        <Fragment>
            {
                requestContract && currentView === VIEW.GOLDEN_SUMMARRY &&
                    <GoldenContract
                        selectedApi={selectedApi}
                        requestExamples={requestExamples}
                        requestContract={requestContract}
                        responseContract={responseContract}
                        handleExampleClick={handleExampleClick}
                    />
            }
            {
                responseContract && currentView === REQUEST_TABS.EXAMPLES && 
                    <ContractExamples 
                        pageNumbers={pageNumbers}
                        examples={requestExamples}
                        selectedPageNumber={selectedPageNumber}
                        setSelectedPageNumber={setSelectedPageNumber}
                        handleExamplesBackClick={handleExamplesBackClick}
                    />
            }
            {
                currentView === RESPONSE_TABS.EXAMPLES && 
                    <ContractExamples 
                        pageNumbers={pageNumbers}
                        examples={responseExamples}
                        selectedPageNumber={selectedPageNumber}
                        setSelectedPageNumber={setSelectedPageNumber}
                        handleExamplesBackClick={handleExamplesBackClick}
                    />
            }
        </Fragment>
    ));

    const renderLoading = () => 
        (
            <div className="gv-insights-prompt">
                Loading...
            </div>
        );

    const renderInsightsFetchError = () => 
        (
            <div className="gv-insights-prompt">
                Failed to fetch data from server for the selected service and api path. 
                Please select a different service and api path or try again later.
            </div>
        );

    return (
        <div className="gv-parent">
            <div className="gv-header">
                {
                    (selectedService && selectedApi)
                    ? 
                    <Fragment>
                        <strong className="margin-right-10">Service:</strong> 
                        <span className="margin-right-10">{selectedService} </span>
                        <strong className="margin-right-10">API:</strong> 
                        <span className="margin-right-10">{selectedApi}</span>
                    </Fragment>
                    :
                    <strong className="margin-right-10">Select a Service and an API</strong> 
                }
            </div>
            {(!isFetching && !fetchComplete && null)}
            
            {(isFetching && !fetchComplete && renderLoading())}

            {
                (!isFetching && fetchComplete) 
                && (requestContract && responseContract) 
                && renderContracts()
            }

            {
                (!isFetching && fetchComplete) 
                && !(requestContract && responseContract) 
                && renderInsightsFetchError()
            }
        </div>
    )
}

const mapStateToProps = (state) => ({
    golden: state.golden
});

export default connect(mapStateToProps, null)(GoldenVisibility);