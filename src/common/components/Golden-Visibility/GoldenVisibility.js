import React, { useState, useEffect, Fragment } from 'react';
import { connect } from 'react-redux';

import "./GoldenVisibility.css";
import { goldenActions } from '../../actions/golden.actions';

const REQUEST_TABS = {
    PARAMS: "request::params",
    BODY: "request::body",
    EXAMPLES: "request::examples",
};

const RESPONSE_TABS = {
    BODY: "response::body",
    EXAMPLES: "response::examples",
};

const VIEW = {
    GOLDEN_SUMMARRY: "view::golden::summary"
};

// To simulate pagination. To be removed and pagination 
// functionality is finalized and implemented
const pageNumbers = [1,2,3,4,5];

const GoldenVisibility = (props) => {
    const { 
        golden: { 
            selectedService, 
            selectedApi,
            requestContract,
            responseContract,
            requestExamples,
            responseExamples
        },
        cube: { selectedGolden },
        getGoldenData
    } = props;

    const [selectedRequestTab, setSelectedRequestTab] = useState(REQUEST_TABS.PARAMS);

    const [selectedResponseTab, setSelectedResponseTab] = useState(RESPONSE_TABS.BODY);

    const [currentView, setCurrentView] = useState(VIEW.GOLDEN_SUMMARRY);

    const [selectedPageNumber, setSelectedPageNumber] = useState(1);

    const handleExampleClick = (exampleType) => setCurrentView(exampleType);

    const handleExamplesBackClick = () => {
        setCurrentView(VIEW.GOLDEN_SUMMARRY);
        setSelectedPageNumber(1);
    };

    useEffect(() => {
        getGoldenData(selectedGolden, selectedService, selectedApi, selectedPageNumber);
    }, [selectedGolden, selectedService, selectedApi, selectedPageNumber]);

    const renderRequestExamples = () => (
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
                    value={
                        selectedRequestTab === REQUEST_TABS.PARAMS 
                        ? requestExamples.params
                        : JSON.stringify(requestExamples.body, undefined, 4)
                    } 
                 readOnly className="gv-textarea" rows="30" cols="40"></textarea>
            </div>
        </Fragment>
    );

    const renderResponseExamples = () => (
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
                <textarea value={JSON.stringify(responseExamples.body, undefined, 4)} readOnly className="gv-textarea" rows="30" cols="40"></textarea>
            </div>
        </Fragment>
    );

    const renderGoldenContract = () => (
        <Fragment>
            <div className="gv-section-banner">
                <span>Request</span>
                <span onClick={() => handleExampleClick(REQUEST_TABS.EXAMPLES)} className="gv-link">Examples</span>
            </div>
            <div className="gv-section-details">
                <div className="gv-request-container">
                    <div className="gv-request-verb">
                        POST
                    </div>
                    <div className="gv-request-path">{`https://demo.dev.cubecorp.io/${selectedApi}`}</div>
                </div>
                <div className="gv-tab-container">
                    <div 
                        onClick={() => setSelectedRequestTab(REQUEST_TABS.PARAMS)} 
                        className={selectedRequestTab === REQUEST_TABS.PARAMS ? "gv-tab-button-selected" : "gv-tab-button"}
                    >
                        Params
                    </div>
                    <div 
                        onClick={() => setSelectedRequestTab(REQUEST_TABS.BODY)} 
                        className={selectedRequestTab === REQUEST_TABS.BODY ? "gv-tab-button-selected" : "gv-tab-button"}
                    >
                        Body
                    </div>
                </div>
                <div className="gv-textarea-container">
                    <textarea value={
                        selectedRequestTab === REQUEST_TABS.PARAMS 
                        ? JSON.stringify(requestContract.params, undefined, 4)
                        : JSON.stringify(requestContract.body, undefined, 4)
                        } 
                        readOnly 
                        className="gv-textarea" 
                        rows="8" 
                        cols="40"
                    ></textarea>
                </div>
            </div>
            <div className="gv-section-banner">
                <span>Response</span>
                <span onClick={() => handleExampleClick(RESPONSE_TABS.EXAMPLES)} className="gv-link">Examples</span>
            </div>
            <div className="gv-section-details">
                <div className="gv-tab-container">
                    <div className="gv-tab-button-selected gv-tab-single">Body</div>
                </div>
                <div className="gv-textarea-container">
                    <textarea value={JSON.stringify(responseContract.body, undefined, 4)} readOnly className="gv-textarea" rows="15" cols="40"></textarea>
                </div>
            </div>
        </Fragment>
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
            {(selectedService && selectedApi) && (
                <Fragment>
                    {currentView === VIEW.GOLDEN_SUMMARRY && renderGoldenContract()}
                    {currentView === REQUEST_TABS.EXAMPLES && renderRequestExamples()}
                    {currentView === RESPONSE_TABS.EXAMPLES && renderResponseExamples()}
                </Fragment>
            )}
        </div>
    )
}

const mapStateToProps = (state) => ({
    golden: state.golden,
    cube: state.cube
});

const mapDispatchToProps = (dispatch) => ({
    getGoldenData: (goldenId, service, api, selectedPageNumber) => { 
        dispatch(goldenActions.getGoldenData(goldenId, service, api, selectedPageNumber));
    }
});

export default connect(mapStateToProps, mapDispatchToProps)(GoldenVisibility);