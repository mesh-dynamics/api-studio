import React, {Component, Fragment} from "react";
import {
    APICatalogFilter, 
    APICatalogServiceView, 
    APICatalogAPIView, 
    APICatalogLanding, 
    APICatalogSearchResults,
    APICatalogServiceGraph,
    APICatalogDiff,
} from "../../components/APICatalog/"
import {
    Route,
} from "react-router-dom";
import { history } from '../../helpers';
import _ from "lodash";
import { cubeService } from "../../services";
import { connect } from "react-redux";
import { cubeActions } from "../../actions";
import {FormControl, FormGroup}  from "react-bootstrap";

class APICatalog extends Component {
    constructor (props) {
        super(props);

        this.state = {
            app: "",
            selectedService: "",
            selectedApiPath: "",
            selectedInstance: "",
            startTime: new Date(Date.now() - 86400 * 1000).toISOString(),
            endTime: new Date(Date.now()).toISOString(),
            prevDays: 1,

            apiFacets: {}, 
            services: [], 
            apiPaths: [], 
            instances: [], 
            apiTrace:{},
            apiCount: 0,

            currentPage: "landing",
        }
    }

    componentDidMount() {
      const {dispatch} = this.props;

      let urlParameters = _.chain(window.location.search)
      .replace('?', '')
      .split('&')
      .map(_.partial(_.split, _, '=', 2))
      .fromPairs()
      .value();
      
      const selectedService = urlParameters["selectedService"];
      const selectedApiPath = urlParameters["selectedApiPath"];
      const selectedInstance = urlParameters["selectedInstance"];
      
      let {startTime, endTime} = this.state;
      startTime = urlParameters["startTime"] || startTime;
      endTime = urlParameters["endTime"] || endTime;
      
      const app = urlParameters["app"];
      dispatch(cubeActions.setSelectedApp(app));
      
      setTimeout(() => {
        const {cube} = this.props;

        this.setState({
          app: cube.selectedApp,

          selectedService: selectedService,
          selectedApiPath: selectedApiPath,
          selectedInstance: selectedInstance,

          startTime: startTime,
          endTime: endTime,
        })
      
        // make facet api call and populate dropdowns
        this.setAPIFacets(cube.selectedApp, selectedService, selectedApiPath, selectedInstance, startTime, endTime);
        // update the requests table data
        this.updateResults();
      });

    }

    componentWillReceiveProps(nextProps){

      const { startTime, endTime } = this.state;

      if(this.props.cube.selectedApp 
        && nextProps.cube.selectedApp 
        && (this.props.cube.selectedApp !== nextProps.cube.selectedApp)
        ) {
        this.setState({
          app: nextProps.cube.selectedApp,
          selectedService: "",
          selectedApiPath: "",
          selectedInstance: "",
          startTime: new Date(Date.now() - 86400 * 1000).toISOString(),
          endTime: new Date(Date.now()).toISOString(),
          prevDays: 1,

          apiFacets: {}, 
          services: [], 
          apiPaths: [], 
          instances: [], 
          apiTrace:{},
          apiCount: 0,

          //currentPage: "landing",

        });

        this.setAPIFacets(nextProps.cube.selectedApp, "", "", "", startTime, endTime);

        // history.push({
        //   pathname: "/api_catalog",
        //   search: `?app=${nextProps.cube.selectedApp}`
        // });
      }
    }

    setAPIFacetsFromState = () => {
      const {selectedService, selectedApiPath, selectedInstance, startTime, endTime} = this.state;
      const {cube} = this.props;

      this.setAPIFacets(cube.selectedApp, selectedService, selectedApiPath, selectedInstance, startTime, endTime);
    }

    // fetch and set the API apiFacets, populate dropdowns
    setAPIFacets = (app, selectedService, selectedApiPath, selectedInstance, startTime, endTime) => {
      cubeService.fetchAPIFacetData(app, startTime, endTime)
        .then((result) => {
          const apiFacets = result;

          const services = this.getServiceList(apiFacets);
          const apiPaths = this.getIncomingAPIList(apiFacets, selectedService);
          const instances = this.getInstanceList(apiFacets, selectedService, selectedApiPath);
          const apiCount = this.getAPICount(apiFacets, selectedService, selectedApiPath, selectedInstance);

          this.setState({
            apiFacets: apiFacets,

            services: services,
            apiPaths: apiPaths,
            instances: instances,
            apiCount: apiCount,
          });
        })
    }
    
    // set states on filter change and make API calls
    handleFilterChange = (metadata, value) => {
        let {selectedService, selectedApiPath, selectedInstance, apiPaths, instances, startTime, endTime, apiFacets} = this.state;
        const {cube} = this.props;
        let startTimeISO, endTimeISO;

        switch (metadata) {
            case "selectedService":
                selectedService = value;
                break;
            case "selectedApiPath":
                selectedApiPath = value;
                break;
            case "selectedInstance":
                selectedInstance = value;
                break;
            case "startTime":
                startTime = value;
                // set and fetch API facets since the time range has changed
                startTimeISO = new Date(startTime).toISOString();
                endTimeISO = new Date(endTime).toISOString();
                this.setAPIFacets(cube.selectedApp, selectedService, selectedApiPath, selectedInstance, startTimeISO, endTimeISO);
                break;
            case "endTime":
                endTime = value;
                // set and fetch API facets since the time range has changed
                startTimeISO = new Date(startTime).toISOString();
                endTimeISO = new Date(endTime).toISOString();
                this.setAPIFacets(cube.selectedApp, selectedService, selectedApiPath, selectedInstance, startTimeISO, endTimeISO);
                break;    
            default:
                break;
        }

        switch (metadata) { // utilizing fallthroughs
            case "selectedService":
                apiPaths = this.getIncomingAPIList(apiFacets, selectedService)
                selectedApiPath = !_.isEmpty(apiPaths) ? apiPaths[0].val : "";
            case "selectedApiPath":
                instances = this.getInstanceList(apiFacets, selectedService, selectedApiPath)
                selectedInstance = !_.isEmpty(instances) ? instances[0].val : "";
            case "selectedInstance":
            default:
                break;
        }

        const apiCount = this.getAPICount(apiFacets, selectedService, selectedApiPath, selectedInstance);

        this.setState({
            selectedService,
            selectedApiPath,
            selectedInstance,
            apiPaths,
            instances,
            startTime,
            endTime,
            apiCount,
        }, () => {
          this.updateURL();
          this.updateResults();
        }); 

    }

    // sync url params with filters
    updateURL = () => {
      const {selectedService, selectedApiPath, selectedInstance, startTime, endTime, currentPage} = this.state;
      const {cube} = this.props;
      
      const startTimeISO = new Date(startTime).toISOString();
      const endTimeISO = new Date(endTime).toISOString();

      if (currentPage === "landing" || currentPage === "service") {
        history.push({
          pathname: "/api_catalog/service",
          search: `?app=${cube.selectedApp}&selectedService=${selectedService}&startTime=${startTimeISO}&endTime=${endTimeISO}`
        })
      } else if (currentPage === "api") {
        history.push({
          pathname: "/api_catalog/api",
          search: `?app=${cube.selectedApp}&selectedService=${selectedService}&selectedApiPath=${selectedApiPath}&selectedInstance=${selectedInstance}&startTime=${startTimeISO}&endTime=${endTimeISO}`
        })
      }
    }

    updateResults = () => {
        // make api call to fetch events list
        let {app, startTime, endTime, selectedService, selectedApiPath, selectedInstance} = this.state;
  
        const startTimeISO = new Date(startTime).toISOString();
        const endTimeISO = new Date(endTime).toISOString();

        cubeService.fetchAPITraceData(app, startTimeISO, endTimeISO, selectedService, selectedApiPath, selectedInstance)
        .then((result) => {
          const trace = result;
          this.setState({
            apiTrace:trace,
          });
        })

    }

    setCurrentPage = (page) => {
      this.setState({currentPage: page})
    }

    getServiceList = (apiFacets) => {
        return _.chain(apiFacets.serviceFacets)
            .map(e => {return {val: e.val, count: e.count}})
            .value();
    }

    getIncomingAPIList = (apiFacets, service) => {
        const serviceObject = _.find(apiFacets.serviceFacets, {val: service})
        if (_.isEmpty(serviceObject)) {
          return [];
        }
        
        return _.chain(serviceObject.path_facets)
                .map(e => {return {val: e.val, count: e.count}})
                .value()
    }

    getInstanceList = (apiFacets, service, apiPath) => {
        const serviceObject = _.find(apiFacets.serviceFacets, {val: service})
        if (_.isEmpty(serviceObject)) {
          return [];
        }

        const pathObject = _.find(serviceObject.path_facets, {val: apiPath})
        if (_.isEmpty(pathObject)) {
          return [];
        }

        return _.chain(pathObject.instance_facets)
                .map(e => {return {val: e.val, count: e.count}})
                .value()
    }

    // get api count for the selected service, api, instance
    getAPICount = (apiFacets, selectedService, selectedApiPath, selectedInstance) => {
      const serviceObject = _.find(apiFacets.serviceFacets, {val: selectedService})
      if (_.isEmpty(serviceObject)) {
        return 0;
      }

      const pathObject = _.find(serviceObject.path_facets, {val: selectedApiPath})
      if (_.isEmpty(pathObject)) {
        return 0;
      }

      const instanceObject = _.find(pathObject.instance_facets, {val: selectedInstance})
      if (_.isEmpty(instanceObject)) {
        return 0;
      }

      return instanceObject.count;

    }

    handlePrevDaysChange = (prevDays) => {
      const startTime = new Date(Date.now() - prevDays * 1000 * 86400).toISOString();
      this.setState({
        prevDays, 
        startTime,
        selectedService: "",
      }, 
      this.setAPIFacetsFromState);
    }

    render() {
        const {selectedService, selectedApiPath, startTime, endTime, selectedInstance, services, apiPaths, instances, currentPage, apiCount, apiTrace, prevDays} = this.state;
        const {cube} = this.props;
        return (
            <div className="h-100" style={{display: "flex"}}>
                <div className="info-wrapper">
                    <APICatalogFilter
                        app={cube.selectedApp}
                        handleFilterChange={this.handleFilterChange}
                        currentPage={currentPage}
                        selectedService={selectedService}
                        selectedApiPath={selectedApiPath}
                        startTime={startTime}
                        endTime={endTime}
                        prevDays={prevDays}
                        handlePrevDaysChange={this.handlePrevDaysChange}
                        selectedInstance={selectedInstance}
                        services={services}
                        apiPaths={apiPaths}
                        instances={instances}
                    />
                </div>
                <div className="content-wrapper" style={{width:"100%", overflow: "scroll"}}>
                    <div>
                        <div className="vertical-middle inline-block">
                            <svg height="21"  viewBox="0 0 22 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M14.6523 0.402344L8.25 4.14062V11.3594L14.6523 15.0977L21.0977 11.3594V4.14062L14.6523 0.402344ZM14.6523 2.55078L18.1328 4.52734L14.6523 6.54688L11.1719 4.52734L14.6523 2.55078ZM0 3.15234V5H6.40234V3.15234H0ZM10.0977 6.03125L13.75 8.13672V12.4336L10.0977 10.3281V6.03125ZM19.25 6.03125V10.3281L15.5977 12.4336V8.13672L19.25 6.03125ZM1.84766 6.84766V8.65234H6.40234V6.84766H1.84766ZM3.65234 10.5V12.3477H6.40234V10.5H3.65234Z" fill="#CCC6B0"/>
                            </svg>
                        </div>
                        <div className="inline-block vertical-middle" style={{fontWeight: "bold", position: "relative", bottom: "3px", opacity: "0.5", paddingLeft: "10px"}}>{"API CATALOG - " + (currentPage==="diff" ? "COMPARE" : "BROWSE")}</div>
                    </div>

                    <div>
                        <FormGroup>
                            <FormControl style={{marginBottom: "12px", marginTop: "10px"}}
                                type="text"
                                placeholder="Search"
                            />
                        </FormGroup>
                    </div>

                    <APICatalogServiceGraph/>
                    <Route exact path="/api_catalog" 
                        render={() => <APICatalogLanding
                                        setCurrentPage={this.setCurrentPage}
                                      />
                                }
                    />
                    <Route exact path="/api_catalog/api" 
                        render={() => <APICatalogAPIView
                                        setCurrentPage={this.setCurrentPage}
                                        selectedService={selectedService}
                                        selectedApiPath={selectedApiPath}
                                        apiCount={apiCount}
                                        apiTrace={apiTrace}
                                        app={cube.selectedApp} 
                                        selectedInstance={selectedInstance}
                                      />
                                } 
                    />

                    <Route exact path="/api_catalog/diff" 
                        render={() => <APICatalogDiff
                          setCurrentPage={this.setCurrentPage}
                        />
                        }
                    />
                    <Route path="/api_catalog/search" render={() => <APICatalogSearchResults/>} />
                </div>
            </div>
        );
    }
}

const mapStateToProps = (state) => ({
  cube: state.cube,
});

const connectedAPICatalog = connect(mapStateToProps)(APICatalog);

export default connectedAPICatalog;
export {connectedAPICatalog as APICatalog}