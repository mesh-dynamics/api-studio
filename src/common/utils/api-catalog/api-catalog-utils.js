import _ from "lodash";

const getServiceList = (apiFacets) => {
    return _.chain(apiFacets.serviceFacets)
        .map(e => { return { val: e.val, count: e.count } })
        .value();
}

const getIncomingAPIList = (apiFacets, service) => {
    const serviceObject = _.find(apiFacets.serviceFacets, { val: service })
    if (_.isEmpty(serviceObject)) {
        return [];
    }

    return _.chain(serviceObject.path_facets)
        .map(e => { return { val: e.val, count: e.count } })
        .value()
}

const getInstanceList = (apiFacets, service, apiPath) => {
    const serviceObject = _.find(apiFacets.serviceFacets, { val: service })
    if (_.isEmpty(serviceObject)) {
        return [];
    }

    const pathObject = _.find(serviceObject.path_facets, { val: apiPath })
    if (_.isEmpty(pathObject)) {
        return [];
    }

    return _.chain(pathObject.instance_facets)
        .map(e => { return { val: e.val, count: e.count } })
        .value()
}

// get api count for the selected service, api, instance
const getAPICount = (apiFacets, selectedService, selectedApiPath, selectedInstance) => {
    const serviceObject = _.find(apiFacets.serviceFacets, { val: selectedService })
    if (_.isEmpty(serviceObject)) {
        return 0;
    }

    const pathObject = _.find(serviceObject.path_facets, { val: selectedApiPath })
    if (_.isEmpty(pathObject)) {
        return 0;
    }

    const instanceObject = _.find(pathObject.instance_facets, { val: selectedInstance })
    if (_.isEmpty(instanceObject)) {
        return 0;
    }

    return instanceObject.count;
}

export { 
    getServiceList, 
    getIncomingAPIList,
    getInstanceList,
    getAPICount
};