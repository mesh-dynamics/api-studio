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

// get api count for the selected service, api, instance (if present)
const getAPICount = (apiFacets, selectedService, selectedApiPath, selectedInstance) => {
    const serviceObject = _.find(apiFacets.serviceFacets, { val: selectedService })
    if (_.isEmpty(serviceObject)) {
        return 0;
    }

    const pathObject = _.find(serviceObject.path_facets, { val: selectedApiPath })
    if (_.isEmpty(pathObject)) {
        return 0;
    }

    if (!selectedInstance) {
        return pathObject.count
    }

    const instanceObject = _.find(pathObject.instance_facets, { val: selectedInstance })
    if (_.isEmpty(instanceObject)) {
        return 0;
    }

    return instanceObject.count;
}


const getDefaultTraceApiFilters = ()=> {
    //TODO: This should be an interface (typescript) and initial value, rather then a function
    return {
        app: "", startTime: null, 
        endTime: null, service: null, 
        apiPath: null, instance: null,
        recordingType: null, collectionName: null,
        depth: 2, numResults: null
    }
}

const getLastApiTraceEndTimeFromApiTrace = (apiTraces)=>{
    let currentEndTime = null;
    if(apiTraces && apiTraces.length > 0){
        const lastApiTrace =apiTraces[apiTraces.length -1];
        if(lastApiTrace.res && lastApiTrace.res.length > 0){
            const timestamp = lastApiTrace.res[lastApiTrace.res.length -  1].reqTimestamp;
            currentEndTime = new Date(timestamp*1000).toISOString();
        }
    }
    return currentEndTime;
}

export { 
    getServiceList, 
    getIncomingAPIList,
    getInstanceList,
    getAPICount,
    getDefaultTraceApiFilters,
    getLastApiTraceEndTimeFromApiTrace
};