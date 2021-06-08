import React, { useState, useEffect, Fragment } from 'react';
import { connect } from "react-redux";
import _ from "lodash";
import moment from "moment";
import { Link, withRouter } from "react-router-dom";
import { 
    generateServiceOptionsFromFacets,
    generateApiOptionsFromFacets,
    validateGoldenName,
} from "../../utils/lib/golden-utils";
import { cubeActions } from "../../actions";
import { goldenActions } from "../../actions/golden.actions";
import { Modal } from "react-bootstrap";

const GoldenMeta = (props) => {

    const {
        golden: { 
            message, 
            selectedApi,
            selectedGolden, 
            selectedService, 
        },
        location: {
            search   
        },
        handleBackToTestInfoClick, 
        setSelectedService, 
        setSelectedApiPath,
        showServiceGraph,
        hideServiceGraph,
        updateGoldenMeta,
        getGoldenData,
        getGoldenMeta,
    } = props;

    const urlParameters = _.chain(search)
            .replace('?', '')
            .split('&')
            .map(_.partial(_.split, _, '=', 2))
            .fromPairs()
            .value();

    const recordingId = urlParameters["recordingId"] || "";

    const { id, name, label, gitCommitId, timestmp, userId, branch, codeVersion, rootRcrdngId, serviceFacets } = selectedGolden;

    const [editable, setEditable] = useState(false);

    const [goldenName, setGoldenName] = useState(name);

    const [labelName, setLabelName] = useState(label);

    const [branchName, setBranchName] = useState(branch);

    const [codeVersionNumber, setCodeVersionNumber] = useState(codeVersion);

    const [commitId, setCommitId] = useState(gitCommitId);

    const [apiPathOptions, setApiPathOptions] = useState([]);

    const [alertModalVisible, setAlertModalVisible] = useState(false);

    const [userAlertMessage, setUserAlertMessage] = useState({
      header: "",
      message: "",
    });

    const serviceOptions = generateServiceOptionsFromFacets(serviceFacets);

    const handleUpdateClick = () => {
        const { goldenNameIsValid, goldenNameErrorMessage } = validateGoldenName(goldenName);

        if(goldenNameIsValid) {
            if((goldenName === name) && (labelName === label)) {
                showAlertModal("Error", "Golden name and label combination cannot be the same.");
            } else {
                setEditable(false);
                updateGoldenMeta({ id, goldenName, labelName, branchName, codeVersionNumber, commitId });
            }
        } else {
            showAlertModal("Error", goldenNameErrorMessage);
        }
    };


    const handleServiceChange = (e) => {
        setSelectedService(e.target.value);
        setApiPathOptions(generateApiOptionsFromFacets(serviceFacets, e.target.value));
        setSelectedApiPath("");
    };

    const handleApiPathChange = (e) => setSelectedApiPath(e.target.value);

    useEffect(() => {
        getGoldenMeta(recordingId);
        hideServiceGraph();

        return () => { 
            showServiceGraph();
        };
    }, [getGoldenMeta, recordingId]);

    useEffect(() => {
        setGoldenName(name);
        setLabelName(label);
        setBranchName(branch);
        setCodeVersionNumber(codeVersion);
        setCommitId(gitCommitId);
    }, [message, selectedGolden]);

    useEffect(() => {
        if(selectedApi !== "" && selectedService !== "" && selectedGolden.id) {
            getGoldenData(selectedGolden.id, selectedService, selectedApi);
        }
    }, [selectedGolden.id, selectedService, selectedApi]);
    
    const renderAlertModal = (isVisible, dismissHandler) => {
        const {header, message} = userAlertMessage
        return (
            <Modal show={isVisible}>
                <Modal.Header>
                    <Modal.Title>{header}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>
                        {message}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <span onClick={dismissHandler} className="cube-btn">Dismiss</span>
                </Modal.Footer>
            </Modal>
        );
    };

    const handleAlertModalDismissClick = () => {
        setAlertModalVisible(false)
        setUserAlertMessage({
            header: "",
            message: ""
        })
    }
    
    const showAlertModal = (header, message) => {
        setAlertModalVisible(false)
        setUserAlertMessage({
            header,
            message,
        })
    }

    return (<>
        <div>
            <div className="margin-top-10">
                {!editable &&
                    <div className="gv-edit-icon-container">
                        <i className="fa fa-pencil-square-o pointer" aria-hidden="true" onClick={() => setEditable(true)}></i>
                    </div>
                }
                <span className="margin-right-10"><strong>Test suite:</strong></span>
                {!editable && <span>{goldenName}</span>}
                {editable && 
                    <input 
                        style={{ width: "100%"}} 
                        name="name"
                        value={goldenName} 
                        onChange={(e) => setGoldenName(e.target.value.replace(/  /g, " "))}
                    />
                }
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Label:</strong></span>
                {!editable && <span>{labelName}</span>}
                {editable &&
                <input
                    style={{ width: "100%"}}
                    name="label"
                    value={labelName}
                    onChange={(e) => setLabelName(e.target.value)}
                />
                }
            </div>
            <div className="margin-top-10">
                <span className="margin-right-10"><strong>Date Created:</strong></span>
                <span>{moment.unix(timestmp).format("DD-MMM-YYYY hh:mm a")}</span>
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
            <div className="margin-top-10 row">
                <div className="col-sm-12">
                    <Link to="/test_config_view">
                        <div onClick={handleBackToTestInfoClick} className="cube-btn width-100 text-center">Back</div>
                    </Link>
                </div>
            </div>
        </div>
        {alertModalVisible && renderAlertModal(alertModalVisible, handleAlertModalDismissClick)} 
    </>)
}

const mapStateToProps = (state) => ({
    golden: state.golden,
    cube: state.cube
});

const mapDispatchToProps = (dispatch) => ({
    setSelectedService: (data) => { dispatch(goldenActions.setSelectedService(data)) },

    setSelectedApiPath: (data) => { dispatch(goldenActions.setSelectedApiPath(data)) },

    updateGoldenMeta: (data) => { dispatch(goldenActions.updateGoldenMeta(data))},

    showServiceGraph: () => { dispatch(cubeActions.hideServiceGraph(false)) },
    
    hideServiceGraph: () => { dispatch(cubeActions.hideServiceGraph(true)) },

    getGoldenMeta: (recordingId) => { dispatch(goldenActions.getGoldenMeta(recordingId))},

    getGoldenData: (goldenId, service, apiPath) => { 
        dispatch(goldenActions.getGoldenData(goldenId, service, apiPath));
    },
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(GoldenMeta));
