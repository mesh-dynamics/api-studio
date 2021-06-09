import React, { Component, Fragment } from "react";
import { connect } from "react-redux";
import ReactTable from "react-table";
import Modal from "react-bootstrap/es/Modal";
import moment from "moment";
import { cubeActions } from "../../actions";
import "./TestClusterStatus.css";
import { cubeService } from "../../services/cube.service";
import data from "./data.json";

class TestClusterStatus extends Component {
    constructor(props) {
        super(props);
        this.state = {
            clusters: [],
            errorMessage: "", //"An error has occured",
            stopStatusModal: false,
            confirmStopModalVisible: false,
            isHeaderCheckboxChecked: false,
            selectedOngoingReplays: [],
            selectedOngoingRecordings: []
        };
    }
    
    componentDidMount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideServiceGraph(true));
        this.getClusterData();
    }

    componentWillUnmount() {
        const { dispatch } = this.props;
        dispatch(cubeActions.hideServiceGraph(false));
    }

    getClusterData = async () => {
        try {
            const clusterData = await cubeService.fetchClusterList();

            this.setState({ clusters: clusterData });
        } catch (error) {
            this.setState({ errorMessage: error.response.data.message });
        }
    }

    isStopButtonDisabled = () => {
        const { selectedOngoingReplays, selectedOngoingRecordings } = this.state;

        return (selectedOngoingReplays.length === 0 && selectedOngoingRecordings.length === 0);
    }

    isCheckboxChecked = (cluster) => {
        const { selectedOngoingReplays, selectedOngoingRecordings } = this.state;

        if(cluster.status === "Replay") {
            return selectedOngoingReplays.includes(cluster.replayStatus.replayId);
        }

        if(cluster.status === "Recording") {
            return selectedOngoingRecordings.includes(cluster.recordStatus.id);
        }

        return false;
    };

    updateStateOfReplayList = (replayId) => {
        const { selectedOngoingReplays } = this.state;
        if(selectedOngoingReplays.includes(replayId)) {
            const updateReplayList = selectedOngoingReplays.filter(id => replayId !== id);
            this.setState({ selectedOngoingReplays: updateReplayList })
        } else {
            this.setState({ selectedOngoingReplays: [...selectedOngoingReplays, replayId]})
        }
    };

    updateStateOfRecordingsList = (recordingId) => {
        const { selectedOngoingRecordings } = this.state;
        if(selectedOngoingRecordings.includes(recordingId)) {
            const updateRecordingList = selectedOngoingRecordings.filter(id => recordingId !== id);
            this.setState({ selectedOngoingRecordings: updateRecordingList });
        } else {
            this.setState({ selectedOngoingRecordings: [...selectedOngoingRecordings, recordingId ]});
        }
    };

    handleStopClick = () => {
        this.setState({ stopStatusModal: true  });
        console.log("Trigger network call for stop");
    }

    handleDismissClick = () => this.setState({ stopStatusModal: false, confirmStopModalVisible: false });

    closeConfirmStopModal = () => this.setState({ confirmStopModalVisible: false });

    showConfirmStopModal = () => this.setState({ confirmStopModalVisible: true });

    handleHeaderCheckboxChange = (event) => {
        const { clusters, isHeaderCheckboxChecked } = this.state;
        const allReplayIds =  [];
        const allRecordingIds = [];

        if(isHeaderCheckboxChecked) {
            this.setState({
                isHeaderCheckboxChecked: false,
                selectedOngoingRecordings: [],
                selectedOngoingReplays: []
            })
        } else {

            clusters.forEach(cluster => {
                if(cluster.status === "Replay") {
                    allReplayIds.push(cluster.replayStatus.replayId);
                }
    
                if(cluster.status === "Recording") {
                    allRecordingIds.push(cluster.recordStatus.id);
                }
            })

            this.setState({ 
                isHeaderCheckboxChecked: true,
                selectedOngoingRecordings: allRecordingIds,
                selectedOngoingReplays: allReplayIds
            })
        }
    };

    handleRowCheckboxChange = (event) => {
        const { value: selectedClusterName } = event.target;
        const { clusters } = this.state;
        const cluster = clusters.find(({ clusterName }) => clusterName === selectedClusterName);

        if(cluster.status === "Replay") {
            this.updateStateOfReplayList(cluster.replayStatus.replayId);
        }

        if(cluster.status === "Recording") {
            this.updateStateOfRecordingsList(cluster.recordStatus.id);
        }
    };

    isHeaderCheckboxEnabled = () => {
        const { user: { roles }} = this.props;
        // Returns true if user is also an admin
        // Returns false if user us not an admin
        return roles.includes("ROLE_ADMIN");
    }

    isClusterCheckboxEnabled = (owner) => {
        const { user: { username, roles }} = this.props;
        
        if (owner && roles.includes("ROLE_ADMIN")) {
            return true;
        }
        // Returns true if username and owner are same
        // Returns false if username and owner are different
        return (username === owner);
    };

    renderHeaderCheckBox = () => {
        return (
            <div className="test-cluster-table-checkbox">
                <input 
                    type="checkbox" 
                    name="All" 
                    value="All" 
                    checked={this.isHeaderCheckboxChecked}
                    onChange={this.handleHeaderCheckboxChange} 
                    disabled={!this.isHeaderCheckboxEnabled()} 
                />
            </div>
        );
    };
    
    renderRowCheckbox = (row) => {
        const { value: instance } = row;
        const { clusters } = this.state;
        const cluster = clusters.find(({ clusterName }) => clusterName === instance);

        return (
                <div className="test-cluster-table-checkbox">
                    <input 
                        type="checkbox" 
                        name={row.value} 
                        value={row.value}
                        checked={this.isCheckboxChecked(cluster)}
                        onChange={this.handleRowCheckboxChange} 
                        disabled={!this.isClusterCheckboxEnabled(cluster.owner)} 
                    />
                </div>
        );
    };

    renderModals = () => {
        const { 
            confirmStopModalVisible, selectedOngoingRecordings, 
            selectedOngoingReplays, stopStatusModal, errorMessage 
        } = this.state;
        return (
            <Fragment>
                <Modal show={confirmStopModalVisible}>
                    <Modal.Header>
                        <Modal.Title>Confirm Stop</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <div style={{ display: "flex", flexDirection: "column" }}>
                            <span>The following ongoing recording(s) and replay(s) will stopped.</span>
                            {
                                selectedOngoingReplays.length !== 0 
                                && <span><b>Replay :</b>{` ${selectedOngoingReplays.join(", ")}`}</span>
                            }
                            {
                                selectedOngoingRecordings.length !== 0 
                                && <span><b>Recording :</b>{` ${selectedOngoingRecordings.join(", ")}`}</span>
                            }
                        </div>
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.handleStopClick} className="cube-btn margin-right-10">Continue</span>
                        <span onClick={this.closeConfirmStopModal} className="cube-btn">Cancel</span>
                    </Modal.Footer>
                </Modal>
                <Modal show={stopStatusModal}>
                    <Modal.Header>
                        <Modal.Title>Alert</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {
                            errorMessage 
                            ? <div>{`Error: ${errorMessage}`}</div>
                            : <div>The selected recording(s)/replay(s) have been stopped</div>
                        }
                    </Modal.Body>
                    <Modal.Footer>
                        <span onClick={this.handleDismissClick} className="cube-btn">Dismiss</span>
                    </Modal.Footer>
                </Modal>
            </Fragment>
        )
    };

    render() {
        const { clusters } = this.state;
        console.log(this.state);
        return (
            <div>
                {this.renderModals()}
                <div className="test-cluster-header">Test Cluster Status</div>
                <div className="test-cluster-table-container">
                    <ReactTable
                        data={clusters}
                        columns={[
                            {
                                Header: this.renderHeaderCheckBox(),
                                columns: [
                                            {
                                                Cell: (row) => this.renderRowCheckbox(row),
                                                width: 30,
                                                accessor: "clusterName",
                                            }
                                        ]
                            },
                            {
                                Header: <div className="test-cluster-table-column-header">Instance</div>,
                                accessor: "clusterName",
                                columns: [
                                            {
                                                accessor: "clusterName",
                                                width: 250,
                                                Cell: row => <div className="test-cluster-table-cell">{row.value}</div>
                                            }
                                        ]
                            },
                            {
                                Header: <div className="test-cluster-table-column-header">Status</div>,
                                accessor: "status",
                                columns: [
                                            {
                                                accessor: "status",
                                                Cell: row => <div className="test-cluster-table-cell">{row.value}</div>
                                            }
                                        ]
                            },
                            {
                                Header: <div className="test-cluster-table-column-header">Test Owner</div>,
                                accessor: "owner",
                                columns: [
                                            {
                                                accessor: "owner",
                                                Cell: row => <div className="test-cluster-table-cell">{row.value}</div>
                                            }
                                        ]
                            }, 
                            {
                                Header: <div className="test-cluster-table-column-header">Time Started</div>,
                                accessor: "startedAt",
                                columns: [
                                            {
                                                accessor: "startedAt",
                                                Cell: row => 
                                                    <div className="test-cluster-table-cell">
                                                        {row.value ? moment(row.value*1000).format("DD-MMM-YY hh:mm a"): ""}
                                                    </div>
                                            }
                                        ]
                            }, 
                            {
                                Header: <div className="test-cluster-table-column-header">Elapsed Time</div>,
                                accessor: "startedAt",
                                columns: [
                                            {
                                                accessor: "startedAt",
                                                Cell: row => 
                                                    <div className="test-cluster-table-cell">
                                                        {row.value ? moment.unix(row.value).fromNow() : ""}
                                                    </div>
                                            }
                                        ]
                            }, 
                        ]}
                        defaultPageSize={5}
                        className="-striped -highlight"
                    />
                </div>                
                <div style={{
                    display: "flex",
                    flex: "1",
                    justifyContent: "flex-end",
                    padding: "15px",
                    textShadow: "0 0 black"
                }}>
                    <span
                        onClick={this.showConfirmStopModal}
                        style={{ fontSize: "14px", padding: "5px 65px"}} 
                        className={this.isStopButtonDisabled() ? "cube-btn disabled" : "cube-btn"}>STOP</span>
                </div>
            </div>
        )
    }
}

const mapStateToProps = (state) => ({
    user: state.authentication.user
});

export default connect(mapStateToProps)(TestClusterStatus);