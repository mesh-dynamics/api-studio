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

import _ from "lodash";
import { defaultCollectionItem } from '../../constants/gcBrowse.constants';

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

/**
 * 
 * @param {*} options 
 * selectedSource | <'UserGolden', 'Golden', 'Capture'>
 * selectedCollection | string
 * selectedGolden | string
 * userGoldens | object <recordings is the key to be used for source list>
 * actualGoldens | object <recordings is the key to be used for source list>
 */
const findGoldenOrCollectionInSource = (options) => {

    const {
        selectedSource, selectedCollection, selectedGolden, 
        userGoldens, actualGoldens 
    } = options;
    
    if(selectedSource && selectedSource === 'UserGolden' && userGoldens.recordings.length !== 0) {
        return userGoldens.recordings.find(item => item.collec === selectedCollection);
    }

    if(selectedSource && selectedSource === 'Golden' && actualGoldens.recordings.length !== 0) {
        return actualGoldens.recordings.find(item => item.collec === selectedGolden);
    }

    return defaultCollectionItem;
};

export { 
    getServiceList, 
    getIncomingAPIList,
    getInstanceList,
    getDefaultTraceApiFilters,
    findGoldenOrCollectionInSource,
    getLastApiTraceEndTimeFromApiTrace
};