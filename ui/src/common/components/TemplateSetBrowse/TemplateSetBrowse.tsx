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

import React, { Component } from "react";
import { Modal, Button } from "react-bootstrap";
import { connect } from "react-redux";
import { CubeButton } from "../../components/common/CubeButton";
import { cubeConstants } from "../../constants";
import {
  IStoreState,
  ITemplateSetNameLabel,
} from "../../reducers/state.types";
import { cubeService } from "../../services";
import "./TemplateSetBrowse.css";
import classNames from "classnames";

export interface ITemplateSetBrowseProps {
  templateSetName: string;
  templateSetLabel: string;
  handleTemplateSetNameLabelSelect: (
    templateSetName: string,
    templateSetLabel: string
  ) => void;
  selectedApp: string;
  customerId: string;
}

export interface ITemplateSetBrowseState {
  selectedTemplateSetName: string;
  selectedTemplateSetLabel: string;
  showBrowseModal: boolean;
  loadingList: boolean;
  templateSetNameLabelsList: ITemplateSetNameLabel[];
  selectedRowName: string;
  selectedRowLabel: string;
  start: number;
  totalNumResults: number;
  nameFilter: string;
  labelFilter: string;
}

const numResults = 20;

class TemplateSetBrowse extends Component<
  ITemplateSetBrowseProps,
  ITemplateSetBrowseState
> {
  constructor(props: ITemplateSetBrowseProps) {
    super(props);
    this.state = {
      selectedTemplateSetName: props.templateSetName,
      selectedTemplateSetLabel: props.templateSetLabel,
      showBrowseModal: false,
      loadingList: false,
      templateSetNameLabelsList: [],
      selectedRowName: props.templateSetName,
      selectedRowLabel: props.templateSetLabel,
      start: 0,
      totalNumResults: 0,
      nameFilter: "",
      labelFilter: "",
    };
  }

  static getDerivedStateFromProps(props: ITemplateSetBrowseProps) {
    const { templateSetName, templateSetLabel } = props;
    return {
      selectedTemplateSetName: templateSetName,
      selectedTemplateSetLabel: templateSetLabel,
    };
  }

  loadList = async () => {
    const {
      selectedApp,
      customerId,
    } = this.props;

    const {start, nameFilter, labelFilter} = this.state;

    this.setState({ loadingList: true });
    const {templateSetNameLabelsList, totalNumResults} = await cubeService.getTemplateSetNameLabels(
      customerId,
      selectedApp,
      start,
      numResults,
      nameFilter, 
      labelFilter
    );
    this.setState({ loadingList: false, templateSetNameLabelsList, totalNumResults });
  };

  onBrowseClick = () => {
    this.setState({ showBrowseModal: true });
    this.loadList();
  };

  onHideBrowseModal = () => {
    this.setState({ showBrowseModal: false, start: 0 }); 
  };

  onSelectBtnClick = () => {
    const { templateSetNameLabelsList, selectedRowName, selectedRowLabel } = this.state;
    this.props.handleTemplateSetNameLabelSelect(selectedRowName, selectedRowLabel);
    this.setState({ showBrowseModal: false });
  };

  onRowSelectClick = (name: string, label: string) => {
    this.setState({ selectedRowName: name, selectedRowLabel: label});
  };

  onRowSelectDblClick = (name: string, label: string) => {
    this.setState({ selectedRowName: name, selectedRowLabel: label}, this.onSelectBtnClick);
  };

  goToNextPage = () => {
    let {start} = this.state;
    start += numResults
    this.setState({start}, this.loadList)
  }

  goToPrevPage = () => {
    let {start} = this.state;
    start -= numResults
    this.setState({start}, this.loadList)
  }

  setNameFilter = (name: string) => {
    this.setState({nameFilter: name})
  }

  setLabelFilter = (label: string) => {
    this.setState({labelFilter: label})
  }

  onSearchClick = () => {
    this.setState({start: 0}, this.loadList)
  }

  renderModals = () => {
    const { templateSetNameLabelsList, selectedRowName, selectedRowLabel, loadingList, start, nameFilter, labelFilter, totalNumResults } = this.state;
    return (
      <>
        <Modal
          show={this.state.showBrowseModal}
          onHide={this.onHideBrowseModal}
        >
          <Modal.Header closeButton>Browse Comparison Rules</Modal.Header>
          <Modal.Body>
          <div className="margin-bottom-10 gcBrowse-modal-body-container">
              <div className="row margin-bottom-10">
                  <div className="col-md-4">
                      <div className="label-n">NAME</div>
                      <div className="value-n">
                          <input value={nameFilter} onChange={(event) => this.setNameFilter(event.target.value)} className="width-100 h-20px" type="text" />
                      </div>
                  </div>
                  {/* <div className="col-md-2"></div> */}
                  <div className="col-md-4">
                      <div className="label-n">LABEL</div>
                      <div className="value-n">
                          <input value={labelFilter} onChange={(event) => this.setLabelFilter(event.target.value)} className="width-100 h-20px" type="text" />
                      </div>
                  </div>
                  <div className="col-md-4"><CubeButton size="sm" label="Search" onClick={this.onSearchClick} className="margin-top-10"/></div>
              </div>
          </div>
            {
              loadingList ? 
                <div className="tsBrowse-spinner-root">
                  <div className="tsBrowse-spinner-inner">
                    <i className="fa fa-spinner fa-spin"></i>
                  </div>
                </div>
                :
                <div className="tsBrowse-modal-body-table-container">
                  <table className="table table-condensed table-hover table-striped">
                    <thead>
                      <th>Name</th>
                      <th>Label</th>
                      <th>Created on</th>
                    </thead>
                    <tbody>
                      {templateSetNameLabelsList.map(
                        ({ name, label, timestamp }) => (
                          <tr
                            onClick={() => this.onRowSelectClick(name, label)}
                            onDoubleClick={() => this.onRowSelectDblClick(name, label)}
                            className={
                              (name === selectedRowName && label === selectedRowLabel) ? "selected-row" : ""
                            }
                          >
                            <td>{name}</td>
                            <td>{label}</td>
                            <td>{new Date(timestamp).toLocaleString()}</td>
                          </tr>
                        )
                      )}
                    </tbody>
                  </table>
                </div>
              }
          </Modal.Body>
          <Modal.Footer>
            <div className="pull-left">
                <CubeButton 
                  faIcon="fa-caret-left" 
                  onClick={this.goToPrevPage} 
                  className={classNames({"disabled": start <= 0})} 
                  style={{marginRight: 0}}
                />
                <CubeButton 
                  faIcon="fa-caret-right" 
                  onClick={this.goToNextPage} 
                  className={classNames({"disabled": start + templateSetNameLabelsList.length >= totalNumResults})} 
                  style={{marginLeft: 0}}
                />
              <span>{loadingList ? "Loading..." : <>Displaying <strong>{start} - {start + templateSetNameLabelsList.length}</strong> of {totalNumResults}</>}</span>
            </div>
            <CubeButton label="Select" onClick={this.onSelectBtnClick} />
          </Modal.Footer>
        </Modal>
      </>
    );
  };

  render() {
    const { selectedTemplateSetName, selectedTemplateSetLabel } = this.state;
    return (
      <div style={{display: 'flex', justifyContent: "space-between"}}>
        <div style={{display: 'flex',  alignItems: 'center'}}>
          {selectedTemplateSetName ? <span>{selectedTemplateSetName} {selectedTemplateSetLabel}</span> : <span>No Comparison Rules selected</span>}
        </div>
        <CubeButton label="" onClick={this.onBrowseClick} faIcon="fa-folder-open" title="Browse Comparison Rules"></CubeButton>
        {this.renderModals()}
      </div>
    );
  }
}

const mapStateToProps = (state: IStoreState) => {
  const {
    cube: { selectedApp },
    authentication: {
      user: { customer_name: customerId },
    },
  } = state;
  return {
    selectedApp,
    customerId,
  };
};

const mapDispatchToProps = (dispatch: any) => ({
  
});

export default connect(mapStateToProps, mapDispatchToProps)(TemplateSetBrowse);
