import _ from "lodash";

const getServiceList = (apiFacets) => {
    return _.chain(apiFacets.serviceFacets)
        .map(e => { return { val: e.val, count: e.count } })
        .value();
}

const getIncomingAPIList = (apiFacets, service) => {
    if(service){
        const serviceObject = _.find(apiFacets.serviceFacets, { val: service })
        if (_.isEmpty(serviceObject)) {
            return [];
        }

        return _.chain(serviceObject.path_facets)
            .map(e => { return { val: e.val, count: e.count } })
            .value();
    }else{
        const apiPaths = [];
        apiFacets.serviceFacets.forEach( service => {
            service.path_facets.forEach(pathFacet => {
                if(!_.find(apiPaths, {val: pathFacet.val} )){
                    apiPaths.push({ val: pathFacet.val, count: pathFacet.count });
                }
            })
            
        });
        return apiPaths;
    }
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
    getDefaultTraceApiFilters,
    getLastApiTraceEndTimeFromApiTrace
};