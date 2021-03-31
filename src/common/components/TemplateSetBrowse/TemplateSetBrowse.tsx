import React, { Component } from "react";
import { Modal, ModalHeader } from "react-bootstrap";
import { connect } from "react-redux";
import { CubeButton } from "../../components/common/CubeButton";
import { cubeConstants } from "../../constants";
import {
  IStoreState,
  ITemplateSetNameLabel,
} from "../../reducers/state.types";
import { cubeService } from "../../services";
import "./TemplateSetBrowse.css";

export interface ITemplateSetBrowseProps {
  templateSetName: string;
  templateSetLabel: string;
  handleTemplateSetNameLabelSelect: (
    templateSetName: string,
    templateSetLabel: string
  ) => void;
  selectedApp: string;
  customerId: string;
  setListInRedux: (
    templateSetNameLabelList: ITemplateSetNameLabel[]
  ) => void;
}

export interface ITemplateSetBrowseState {
  selectedTemplateSetName: string;
  selectedTemplateSetLabel: string;
  showBrowseModal: boolean;
  loadingList: boolean;
  templateSetNameLabelsList: ITemplateSetNameLabel[];
  selectedRowIndex: number;
}

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
      selectedRowIndex: 0,
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
      setListInRedux,
    } = this.props;
    this.setState({ loadingList: true });
    const templateSetNameLabelsList = await cubeService.getTemplateSetNameLabels(
      customerId,
      selectedApp
    );
    setListInRedux(templateSetNameLabelsList);
    this.setState({ loadingList: false, templateSetNameLabelsList });
  };

  onBrowseClick = () => {
    this.setState({ showBrowseModal: true });
    this.loadList();
  };

  onHideBrowseModal = () => {
    this.setState({ showBrowseModal: false });
  };

  onSelectBtnClick = () => {
    const { templateSetNameLabelsList, selectedRowIndex } = this.state;
    const { name, label } = templateSetNameLabelsList[selectedRowIndex];
    this.props.handleTemplateSetNameLabelSelect(name, label);
    this.setState({ showBrowseModal: false });
  };

  onRowSelectClick = (rowIndex: number) => {
    this.setState({ selectedRowIndex: rowIndex });
  };

  onRowSelectDblClick = (rowIndex: number) => {
    this.setState({ selectedRowIndex: rowIndex }, this.onSelectBtnClick);
  };

  renderModals = () => {
    const { templateSetNameLabelsList, selectedRowIndex, loadingList } = this.state;
    return (
      <>
        <Modal
          show={this.state.showBrowseModal}
          onHide={this.onHideBrowseModal}
        >
          <Modal.Header closeButton>Browse Template Sets</Modal.Header>
          <Modal.Body>
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
                        ({ name, label, timestamp }, index) => (
                          <tr
                            onClick={() => this.onRowSelectClick(index)}
                            onDoubleClick={() => this.onRowSelectDblClick(index)}
                            className={
                              index === selectedRowIndex ? "selected-row" : ""
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
          <span>{selectedTemplateSetName} {selectedTemplateSetLabel}</span>
        </div>
        <CubeButton label="" onClick={this.onBrowseClick} faIcon="fa-folder-open" title="Browse Template Sets"></CubeButton>
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
  setListInRedux: (
    templateSetNameLabelsList: ITemplateSetNameLabel[]
  ) =>
    dispatch({
      type: cubeConstants.SET_TEMPLATE_SET_NAME_LABELS_LIST,
      data: templateSetNameLabelsList,
    }),
});

export default connect(mapStateToProps, mapDispatchToProps)(TemplateSetBrowse);
