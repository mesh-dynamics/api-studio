import React, { Component, Fragment, createContext } from "react";
import EnvVar from "./EnvVar";
import { connect } from "react-redux";
import { getCurrentEnvironment } from "../../utils/http_client/envvar";
import { httpClientActions } from "../../actions/httpClientActions";

//Remove unused Components later
import {
  FormControl,
  FormGroup,
  Tabs,
  Tab,
  Panel,
  Label,
  Modal,
  Button,
  ControlLabel,
  Glyphicon,
} from "react-bootstrap";

class EnvironmentSection extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showEnvVarModal: false,
      showSelectedEnvModal: false,
    };
  }

  renderEnvListDD = () => {
    const {
      httpClient: { environmentList, selectedEnvironment },
    } = this.props;
    return (
      <FormGroup bsSize="small" style={{ marginBottom: "0px" }}>
        <FormControl
          componentClass="select"
          placeholder="Environment"
          style={{ fontSize: "12px" }}
          value={selectedEnvironment}
          onChange={this.handleEnvChange}
          className="btn-sm"
        >
          <option value="">No Environment</option>
          {environmentList.length &&
            environmentList.map((env) => (
              <option key={env.name} value={env.name}>
                {env.name}
              </option>
            ))}
        </FormControl>
      </FormGroup>
    );
  };

  handleEnvChange = (e) => {
    const { dispatch } = this.props;
    dispatch(httpClientActions.setSelectedEnvironment(e.target.value));
  };

  renderSelectedEnvModal = () => {
    const {
      httpClient: { environmentList, selectedEnvironment },
    } = this.props;
    
    const currentEnvironment = getCurrentEnvironment(
      environmentList,
      selectedEnvironment
    );

    return (
      <Fragment>
        <span
          title="Environment quick look"
          className="btn btn-sm cube-btn text-center"
          onClick={this.openSelectedEnvModal}
        >
          <i className="fas fa-eye" />
        </span>
        <Modal
          show={this.state.showSelectedEnvModal}
          onHide={this.closeSelectedEnvModal}
        >
          <Modal.Header closeButton>
            <Modal.Title>
              {selectedEnvironment || "No Environment Selected"}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div>
              {currentEnvironment && !_.isEmpty(currentEnvironment.vars) && (
                <table className="table table-bordered table-hover">
                  <thead>
                    <tr>
                      <th style={{ width: "20%" }}>Variable</th>
                      <th>Value</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentEnvironment.vars.map((varEntry) => (
                      <tr key={varEntry.key}>
                        <td>{varEntry.key}</td>
                        <td style={{ wordBreak: "break-all" }}>
                          {varEntry.value}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </Modal.Body>
        </Modal>
      </Fragment>
    );
  };

  openSelectedEnvModal = () => {
    this.setState({ showSelectedEnvModal: true });
  };

  closeSelectedEnvModal = () => {
    this.setState({ showSelectedEnvModal: false });
  };

  hideEnvModal = () => {
    this.setState({ showEnvVarModal: false });
  };
  render() {
    return (
      <>
        <div style={{ display: "inline-block", padding: 0 }} className="btn">
          {this.renderEnvListDD()}
        </div>
        <div style={{ display: "inline-block" }}>
          {this.renderSelectedEnvModal()}
        </div>
        <span
          className="btn btn-sm cube-btn text-center"
          onClick={() => {
            this.setState({ showEnvVarModal: true });
          }}
          title="Configure environments"
        >
          <i className="fas fa-cog" />{" "}
        </span>

        <Modal
          bsSize="large"
          show={this.state.showEnvVarModal}
          onHide={this.hideEnvModal}
          className="envModal"
        >
          <EnvVar hideModal={this.hideEnvModal} />
        </Modal>
      </>
    );
  }
}

function mapStateToProps(state) {
  const { cube, apiCatalog, httpClient } = state;
  return {
    cube,
    apiCatalog,
    httpClient,
  };
}

const connectedEnvironmentSection = connect(mapStateToProps)(
  EnvironmentSection
);

export default connectedEnvironmentSection;
