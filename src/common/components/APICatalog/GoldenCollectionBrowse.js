import React, { Component } from "react";
import { connect } from "react-redux";
import classNames from "classnames";
import Modal from "react-bootstrap/es/Modal";
import { apiCatalogActions } from "../../actions/api-catalog.actions";
import "./APICatalog.scss";

class GoldenCollectionBrowse extends Component {
  constructor(props) {
    super(props);
    this.state = {
      showBrowseGoldenCollectionModal: false,
      selectedGoldenCollectionFromModal: "",
      nameFilter: "",
      labelFilter: "",
      idFilter: "",
      createdByFilter: "",
    };
  }

  handleFilterChange = (metadata, value) => {
    const { dispatch } = this.props;
    dispatch(apiCatalogActions.handleFilterChange(metadata, value));
  };

  selectHighlightedGoldenCollectionFromModal = () => {
    const {
      apiCatalog: { selectedSource },
    } = this.props;

    if (selectedSource === "UserGolden") {
      this.handleFilterChange(
        "selectedCollection",
        this.state.selectedGoldenCollectionFromModal
      );
    } else if (selectedSource === "Golden") {
      this.handleFilterChange(
        "selectedGolden",
        this.state.selectedGoldenCollectionFromModal
      );
    }

    this.setState({ showBrowseGoldenCollectionModal: false });
  };

  renderCollectionDropdown = () => {
    const {
      apiCatalog: { collectionList, selectedCollection },
    } = this.props;
    const handleCollectionDropDownChange = (event) =>
      this.handleFilterChange("selectedCollection", event.target.value);

    const ddlClass = classNames("r-att form-control", {
      "select-indicator": !selectedCollection,
    });
    return (
      <div>
        <select
          className={ddlClass}
          placeholder="Select Collection"
          value={selectedCollection || "DEFAULT"}
          onChange={handleCollectionDropDownChange}
        >
          <option value="DEFAULT" disabled>
            Select Collection
          </option>
          {collectionList.map((item, index) => (
            <option
              key={item.collec + index}
              value={item.collec}
              hidden={index > 10 && selectedCollection != item.collec}
            >{`${item.name} ${item.label}`}</option>
          ))}
        </select>
      </div>
    );
  };

  renderGoldenDropdown = () => {
    const {
      apiCatalog: { goldenList, selectedGolden },
    } = this.props;
    const handleGoldenDropDownChange = (event) =>
      this.handleFilterChange("selectedGolden", event.target.value);

    const ddlClass = classNames("r-att form-control", {
      "select-indicator": !selectedGolden,
    });
    return (
      <div>
        <select
          className={ddlClass}
          placeholder="Select Golden"
          value={selectedGolden || "DEFAULT"}
          onChange={handleGoldenDropDownChange}
        >
          <option value="DEFAULT" disabled>
            Select Golden
          </option>
          {goldenList.map((item, index) => (
            <option
              key={item.collec + index}
              value={item.collec}
              hidden={index > 10 && selectedGolden != item.collec}
            >{`${item.name} ${item.label}`}</option>
          ))}
        </select>
      </div>
    );
  };

  showGoldenCollectionBrowseModal = () => {
    const { dispatch, cube: { selectedApp }, selectedSource } = this.props;

    dispatch(apiCatalogActions.fetchGoldenCollectionList(selectedApp, selectedSource));
    this.setState({ showBrowseGoldenCollectionModal: true });
  };

  hideGoldenCollectionModal = () => {
    this.setState({ showBrowseGoldenCollectionModal: false });
  };

  selectGoldenCollectionFromModal = (collec) => {
    this.setState({ selectedGoldenCollectionFromModal: collec });
  };

  filterGoldenCollectionList = (goldenCollectionList) => {
    const { nameFilter, labelFilter, idFilter, createdByFilter } = this.state;

    if (nameFilter) {
      goldenCollectionList = goldenCollectionList.filter((item) =>
        item.name.toLowerCase().includes(nameFilter.toLowerCase())
      );
    }

    if (labelFilter) {
      goldenCollectionList = goldenCollectionList.filter(
        (item) =>
          item.label &&
          item.label.toLowerCase().includes(labelFilter.toLowerCase())
      );
    }

    if (createdByFilter) {
      goldenCollectionList = goldenCollectionList.filter(
        (item) =>
          item.userId &&
          item.userId.toLowerCase().includes(createdByFilter.toLowerCase())
      );
    }

    if (idFilter) {
      goldenCollectionList = goldenCollectionList.filter((item) =>
        item.id.toLowerCase().includes(idFilter.toLowerCase())
      );
    }

    return goldenCollectionList;
  };

  renderGoldenCollectionTable() {
    const {
      apiCatalog: { selectedSource, collectionList, goldenList },
    } = this.props;

    const goldenCollectionList =
      selectedSource === "UserGolden" ? collectionList : goldenList;

    const filteredGoldenCollectionList = this.filterGoldenCollectionList(
      goldenCollectionList
    );

    if (
      !filteredGoldenCollectionList ||
      filteredGoldenCollectionList.length === 0
    ) {
      return (
        <tr>
          <td colSpan="2" className="text-center">
            NO DATA FOUND
          </td>
        </tr>
      );
    }

    return (
      <table className="table table-condensed table-hover table-striped">
        <thead>
          <tr>
            <th style={{ position: "sticky", minWidth: "100px" }}>Name</th>
            <th
              className="bold"
              style={{ position: "sticky", minWidth: "100px" }}
            >
              Label
            </th>
            <th
              className="bold"
              style={{ position: "sticky", minWidth: "125px" }}
            >
              ID
            </th>
            <th style={{ position: "sticky", minWidth: "100px" }}>
              Created at
            </th>
            <th style={{ position: "sticky", minWidth: "100px" }}>
              Created by
            </th>
          </tr>
        </thead>

        <tbody>
          {filteredGoldenCollectionList.map((item) => (
            <tr
              key={item.collec}
              value={item.collec}
              className={
                this.state.selectedGoldenCollectionFromModal === item.collec
                  ? "selected-g-row"
                  : ""
              }
              onClick={() => this.selectGoldenCollectionFromModal(item.collec)}
            >
              <td>{item.name}</td>
              <td>{item.label}</td>
              <td>{item.id}</td>
              <td>{new Date(item.timestmp * 1000).toLocaleString()}</td>
              <td>{item.userId}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  applyGoldenFilter = (filter, event) => {
    this.setState({ [filter]: event.target.value });
  };

  renderGoldenCollectionFilters = () => {
    return (
      <>
        <div className="row margin-bottom-10">
          <div className="col-md-5">
            <div className="label-n">NAME</div>
            <div className="value-n">
              <input
                onChange={(event) =>
                  this.applyGoldenFilter("nameFilter", event)
                }
                className="width-100 h-20px"
                type="text"
              />
            </div>
          </div>

          <div className="col-md-2"></div>

          <div className="col-md-5">
            <div className="label-n">LABEL</div>
            <div className="value-n">
              <input
                onChange={(event) =>
                  this.applyGoldenFilter("labelFilter", event)
                }
                className="width-100 h-20px"
                type="text"
              />
            </div>
          </div>
        </div>

        <div className="row margin-bottom-10">
          <div className="col-md-5">
            <div className="label-n">RECORDING ID</div>
            <div className="value-n">
              <input
                onChange={(event) => this.applyGoldenFilter("idFilter", event)}
                className="width-100 h-20px"
                type="text"
              />
            </div>
          </div>

          <div className="col-md-2"></div>

          <div className="col-md-5">
            <div className="label-n">CREATED BY</div>
            <div className="value-n">
              <input
                onChange={(event) =>
                  this.applyGoldenFilter("createdByFilter", event)
                }
                className="width-100 h-20px"
                type="text"
              />
            </div>
          </div>
        </div>
      </>
    );
  };

  renderBrowseModal() {
    const { selectedSource, apiCatalog: { goldenCollectionLoading }  } = this.props;

    return (
      <Modal show={this.state.showBrowseGoldenCollectionModal} size="xl">
        <Modal.Header>
          <Modal.Title>
            {`Browse ${
              selectedSource === "UserGolden" ? "Collections" : "Goldens"
            }`}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <div
            className="margin-bottom-10"
            style={{ padding: "10px 25px", border: "1px dashed #ddd" }}
          >
            {this.renderGoldenCollectionFilters()}
          </div>
          {
            goldenCollectionLoading 
            ? 
              (
                <div className="browse-golden-spinner-root">
                  <div className="browse-golden-spinner-inner">
                    <i className="fa fa-spinner fa-spin"></i>
                  </div>
                </div>
              )
            : 
              (
                <div className="browse-golden-list-container">
                  {this.renderGoldenCollectionTable()}
                </div>
              )
          }
          
        </Modal.Body>
        <Modal.Footer>
          <span
            onClick={this.selectHighlightedGoldenCollectionFromModal}
            className={
              this.state.selectedGoldenCollectionFromModal
                ? "cube-btn"
                : "disabled cube-btn"
            }
          >
            Select
          </span>
          &nbsp;&nbsp;
          <span onClick={this.hideGoldenCollectionModal} className="cube-btn">
            Cancel
          </span>
        </Modal.Footer>
      </Modal>
    );
  }

  render() {
    const { selectedSource } = this.props;
    return (
      <div className="margin-top-10">
        <div className="label-n">
          {selectedSource === "UserGolden" ? "COLLECTION" : "GOLDEN"}&nbsp;
          <i
            onClick={this.showGoldenCollectionBrowseModal}
            title={`Browse ${
              selectedSource === "UserGolden" ? "Collection" : "Golden"
            }`}
            className="link fas fa-folder-open pull-right font-15"
          ></i>
        </div>
        {selectedSource === "UserGolden"
          ? this.renderCollectionDropdown()
          : this.renderGoldenDropdown()}
        {this.renderBrowseModal()}
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  cube: state.cube,
  apiCatalog: state.apiCatalog,
});

export default connect(mapStateToProps)(GoldenCollectionBrowse);
