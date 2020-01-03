import React, { useState, Fragment } from 'react';
import { connect } from "react-redux";
import { generateServiceOptionsFromTimeLine, generateApiOptionsFromTimeLine } from '../../utils/lib/golden-utils';
import {goldenActions} from '../../actions/golden.actions'

const GoldenMeta = (props) => {
    const { 
        testIds, 
        selectedTestId, 
        timelineData: { timelineResults }, 
        handleBackToTestInfoClick, 
        setSelectedService, 
        setSelectedApiPath 
    } = props;

    const selectedTestIdItem = testIds.find(items => items.collec === selectedTestId);

    const { id, name, gitCommitId, timestmp, userId, branch, codeVersion, rootRcrdngId } = selectedTestIdItem;
    
    const [shouldEditName, setShouldEditName] = useState(false);

    const [selectedGoldenName, setSelectedGoldenName] = useState(name);

    const [apiPathOptions, setApiPathOptions] = useState([]);

    const timelineResult = timelineResults.find(item => item.recordingid === id);

    const serviceOptions = generateServiceOptionsFromTimeLine(timelineResult);

    const handleKeyPress = (event) => (event.key === "Enter" && setShouldEditName(false));

    const handleServiceChange = (e) => {
        setSelectedService(e.target.value);
        setApiPathOptions(generateApiOptionsFromTimeLine(timelineResult, e.target.value));
        setSelectedApiPath("");
    };

    const handleApiPathChange = (e) => setSelectedApiPath(e.target.value);

    return (
        <div>
            <div className="margin-top-10">
                <p><strong>Golden:</strong></p>
                {!shouldEditName && <span style={{ width: "100%"}} onClick={() => setShouldEditName(true)}>{selectedGoldenName}</span>}
                {shouldEditName && 
                    <input 
                        style={{ width: "100%"}} 
                        value={selectedGoldenName} 
                        onChange={(e) => setSelectedGoldenName(e.target.value)} 
                        onKeyPress={(e) => handleKeyPress(e)}
                    />
                }
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Date Created:</strong></span>
                <span>{timestmp}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>ID:</strong></span>
                <span>{id}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Source:</strong></span>
                <span>{rootRcrdngId}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Created By:</strong></span>
                <span>{userId}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Branch:</strong></span>
                <span>{branch}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Version:</strong></span>
                <span>{codeVersion}</span>
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Commit Id:</strong></span>
                <span>{gitCommitId}</span>
            </div>
            <div className="margin-top-10">
                <strong>Service</strong>
                <select className="r-att" placeholder={'Select...'} onChange={handleServiceChange}>
                    <option value="">Select Service</option>
                    {serviceOptions.map(option => <option key={option} value={option}>{option}</option>)}
                </select>
            </div>
            <div className="margin-top-10">
                <strong>API</strong>
                <select className="r-att" placeholder={'Select...'} onChange={handleApiPathChange}>
                    <Fragment>
                        <option value="">Select API</option>
                        {apiPathOptions.map(option => <option key={option} value={option}>{option}</option>)}
                    </Fragment>
                </select>
            </div>
            <div onClick={handleBackToTestInfoClick} className="margin-top-10 cube-btn width-100 text-center">Back</div>
        </div>
    )
}

const mapDispatchToProps = (dispatch) => ({
    setSelectedService: (data) => { dispatch(goldenActions.setSelectedService(data)) },

    setSelectedApiPath: (data) => { dispatch(goldenActions.setSelectedApiPath(data)) }
});

export default connect(null, mapDispatchToProps)(GoldenMeta);