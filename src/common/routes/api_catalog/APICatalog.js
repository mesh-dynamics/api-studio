import React, {Component} from "react";
import {
    APICatalogFilter, 
    APICatalogServiceView, 
    APICatalogAPIView, 
    APICatalogRoot, 
    APICatalogSearchResults
} from "../../components/APICatalog/"
import {
    Route,
} from "react-router-dom";
import { history } from '../../helpers';
import _ from "lodash";
import { cubeService } from "../../services";
import { connect } from "react-redux";
import { cubeActions } from "../../actions";

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
      });

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

    render() {
        const {selectedService, selectedApiPath, startTime, endTime, selectedInstance, services, apiPaths, instances, currentPage, apiCount, apiTrace} = this.state;
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
                        selectedInstance={selectedInstance}
                        services={services}
                        apiPaths={apiPaths}
                        instances={instances}
                    />
                </div>
                <div  style={{width:"100%"}}>
                    <Route path="/api_catalog" render={() => <APICatalogRoot/>} />
                    <Route path="/api_catalog/service" 
                        render={() => <APICatalogServiceView
                                        app={cube.selectedApp} 
                                        setCurrentPage={this.setCurrentPage}
                                        apiPaths={apiPaths}
                                        selectedService={selectedService}
                                        startTime={startTime}
                                        endTime={endTime}
                                      />
                                } 
                    />
                    <Route path="/api_catalog/api" 
                        render={() => <APICatalogAPIView
                                        setCurrentPage={this.setCurrentPage}
                                        selectedService={selectedService}
                                        selectedApiPath={selectedApiPath}
                                        apiCount={apiCount}
                                        apiTrace={apiTrace}
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