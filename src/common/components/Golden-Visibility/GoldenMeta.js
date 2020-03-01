import React, { useState, useEffect, Fragment } from 'react';
import { connect } from "react-redux";
import { generateServiceOptionsFromTimeLine, generateApiOptionsFromTimeLine } from '../../utils/lib/golden-utils';
import {goldenActions} from '../../actions/golden.actions'

const GoldenMeta = (props) => {

    const {
        golden: { 
            message, 
            selectedApi,
            selectedGolden, 
            selectedService, 
        },
        cube: { 
            timelineData: { 
                timelineResults 
            } 
        }, 
        handleBackToTestInfoClick, 
        setSelectedService, 
        setSelectedApiPath,
        updateGoldenMeta,
        getGoldenData,
    } = props;

    const { id, name, gitCommitId, timestmp, userId, branch, codeVersion, rootRcrdngId } = selectedGolden;

    const [editable, setEditable] = useState(false);

    const [goldenName, setGoldenName] = useState(name);

    const [branchName, setBranchName] = useState(branch);

    const [codeVersionNumber, setCodeVersionNumber] = useState(codeVersion);

    const [commitId, setCommitId] = useState(gitCommitId);

    const [apiPathOptions, setApiPathOptions] = useState([]);

    const timelineResult = timelineResults.find(item => item.recordingid === id);

    const serviceOptions = generateServiceOptionsFromTimeLine(timelineResult);

    const handleUpdateClick = () => {
        if(goldenName === name) {
            alert("Golden name cannot be the same.");   
        } else {
            setEditable(false);
            updateGoldenMeta({ id, goldenName, branchName, codeVersionNumber, commitId });
        }
    };


    const handleServiceChange = (e) => {
        setSelectedService(e.target.value);
        setApiPathOptions(generateApiOptionsFromTimeLine(timelineResult, e.target.value));
        setSelectedApiPath("");
    };

    const handleApiPathChange = (e) => setSelectedApiPath(e.target.value);

    useEffect(() => {
        setGoldenName(name);
        setBranchName(branch);
        setCodeVersionNumber(codeVersion);
        setCommitId(gitCommitId);
    }, [message])

    useEffect(() => {
        if(selectedApi !== "" && selectedService !== "" && selectedGolden.id) {
            getGoldenData(selectedGolden.id, selectedService, selectedApi);
        }
    }, [selectedGolden.id, selectedService, selectedApi])
    
    return (
        <div>
            <div className="margin-top-10">
                {!editable &&
                    <div className="gv-edit-icon-container">
                        <i class="fa fa-pencil-square-o pointer" aria-hidden="true" onClick={() => setEditable(true)}></i>
                    </div>
                }
                <span className="margin-right-10"><strong>Golden:</strong></span>
                {!editable && <span>{goldenName}</span>}
                {editable && 
                    <input 
                        style={{ width: "100%"}} 
                        name="name"
                        value={goldenName} 
                        onChange={(e) => setGoldenName(e.target.value)}
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
                {!editable && <span>{branchName}</span>}
                {editable && 
                    <input 
                        style={{ width: "100%"}} 
                        name="branchName"
                        value={branchName} 
                        onChange={(e) => setBranchName(e.target.value)} 
                    />
                }
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Version:</strong></span>
                {!editable && <span>{codeVersionNumber}</span>}
                {editable && 
                    <input 
                        style={{ width: "100%"}} 
                        name="codeVersion"
                        value={codeVersionNumber} 
                        onChange={(e) => setCodeVersionNumber(e.target.value)} 
                    />
                }
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Commit Id:</strong></span>
                {!editable && <span>{commitId}</span>}
                {editable && 
                    <input 
                        style={{ width: "100%"}} 
                        name="commitId"
                        value={commitId} 
                        onChange={(e) => setCommitId(e.target.value)} 
                    />
                }
            </div>
            {message && <div className="gv-message-text-generic">{message}</div>}
            {
                editable && 
                <Fragment>
                    <div 
                        className="margin-top-10 cube-btn width-100 text-center" 
                        onClick={handleUpdateClick}
                    >
                        Update
                    </div>
                    <div className="divider" />
                </Fragment>
            }
            <div className="margin-top-20">
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

const mapStateToProps = (state) => ({
    golden: state.golden,
    cube: state.cube
});

const mapDispatchToProps = (dispatch) => ({
    setSelectedService: (data) => { dispatch(goldenActions.setSelectedService(data)) },

    setSelectedApiPath: (data) => { dispatch(goldenActions.setSelectedApiPath(data)) },

    updateGoldenMeta: (data) => { dispatch(goldenActions.updateGoldenMeta(data))},

    getGoldenData: (goldenId, service, apiPath) => { 
        dispatch(goldenActions.getGoldenData(goldenId, service, apiPath));
    }
});

export default connect(mapStateToProps, mapDispatchToProps)(GoldenMeta);