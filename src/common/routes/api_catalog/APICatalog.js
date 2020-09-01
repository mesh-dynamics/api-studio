import React, { Component, Fragment } from "react";
import {
  APICatalogFilter,
  APICatalogAPIView,
  APICatalogLanding,
  APICatalogSearchResults,
  CollapsibleTabs,
  APICatalogDiff,
} from "../../components/APICatalog/"
import {
  Route,
} from "react-router-dom";
import _ from "lodash";
import { connect } from "react-redux";
import { cubeActions } from "../../actions";
import { apiCatalogActions } from "../../actions/api-catalog.actions";
import { FormControl, FormGroup } from "react-bootstrap";
import SplitSlider from "../../components/SplitSlider";

class APICatalog extends Component {
  constructor(props) {
    super(props);

    this.state = {
      app: "",
      currentPage: "api",
    }
  }

  componentDidMount() {
    const { dispatch } = this.props;

    let urlParameters = _.chain(window.location.search)
      .replace('?', '')
      .split('&')
      .map(_.partial(_.split, _, '=', 2))
      .fromPairs()
      .value();

    const app = urlParameters["app"];
    dispatch(cubeActions.setSelectedApp(app));

    setTimeout(() => {
      const { cube } = this.props;

      this.setState({
        app: cube.selectedApp,
      })
    });
  }

  componentWillReceiveProps(nextProps) {

    const {dispatch} = this.props;

    if (this.props.cube.selectedApp
      && nextProps.cube.selectedApp
      && (this.props.cube.selectedApp !== nextProps.cube.selectedApp)
    ) {
      this.setState({
        app: nextProps.cube.selectedApp,
      });

      dispatch(apiCatalogActions.resetFilters());
    }
  }

  setCurrentPage = (page) => {
    this.setState({ currentPage: page })
  }

  render() {
    const {  currentPage } = this.state;
    const { cube } = this.props;
    return (
      <div className="h-100" style={{ display: "flex" }}>
        <div className="info-wrapper" ref={e=> (this.sliderTarget = e)}>
          <APICatalogFilter
            currentPage={currentPage}
          />
        </div>
        <SplitSlider slidingElement={this.sliderTarget}/>
        <div className="content-wrapper" style={{ flex: "1 1 0%", overflow: "scroll" }}>
          {/* <div>
          <div className="api-catalog-title-container">
            <div className="vertical-middle inline-block">
              <svg height="21" viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0" />
              </svg>
            </div>
            <div className="inline-block vertical-middle" style={{ fontWeight: "bold", position: "relative", bottom: "3px", opacity: "0.5", paddingLeft: "10px" }}>{"API CATALOG - " + (currentPage === "diff" ? "COMPARE" : "BROWSE")}</div>
          </div>

          {/* <div>
            <FormGroup>
              <FormControl style={{ marginBottom: "12px", marginTop: "10px" }}
                type="text"
                placeholder="Search"
              />
            </FormGroup>
          </div> */}

          <CollapsibleTabs currentPage={this.state.currentPage} />
        
          <Route exact path="/api_catalog"
            render={() => <APICatalogLanding
              setCurrentPage={this.setCurrentPage}
            />
            }
          />
          <Route exact path="/api_catalog/api"
            render={() => <APICatalogAPIView
              setCurrentPage={this.setCurrentPage}
              app={cube.selectedApp}
            />
            }
          />

          <Route exact path="/api_catalog/diff"
            render={() => <APICatalogDiff
              setCurrentPage={this.setCurrentPage}
            />
            }
          />
          <Route path="/api_catalog/search" render={() => <APICatalogSearchResults />} />
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  cube: state.cube,
  apiCatalog: state.apiCatalog
});

const connectedAPICatalog = connect(mapStateToProps)(APICatalog);

export default connectedAPICatalog;
export { connectedAPICatalog as APICatalog }