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

//Following could be used globally, can be moved to a common utility file.
const isValidJSON = (jsonString) =>{
    try{
         JSON.parse(jsonString);
    }catch(e){
        return false;
    }
    return true;
}

const isRequestTypeGrpc = (selectedTraceTableReqTabId, currentSelectedTab, outgoingRequests) => {
    if(selectedTraceTableReqTabId === currentSelectedTab.id) {
        return currentSelectedTab.bodyType === "grpcData";
    } else {
        const outgoingRequestTab = outgoingRequests.find(req => req.id === selectedTraceTableReqTabId);
        return outgoingRequestTab.bodyType === "grpcData";
    }
};

const getDefaultGrpcData = (getDefaultFromGrpcSchema)=>{
    let packageName = "", serviceName = "", method = "";
    const packageList =  Object.keys(getDefaultFromGrpcSchema);
    if(packageList && packageList.length > 0){
        packageName = packageList[0];
        const serviceObj = getDefaultFromGrpcSchema[packageName];
        const serviceList =  Object.keys(serviceObj);
        if(serviceList && serviceList.length > 0){
            serviceName = serviceList[0];
            const service = serviceObj[serviceName];
            const methodList =  Object.keys(service);
            if(methodList && methodList.length > 0){
                method = methodList[0];
            }
        }
    }
    return{
        packageName,
        serviceName,
        method,
        servicePackageName: `${packageName}.${serviceName}`
    }
}

//Send getDefaultFromGrpcSchema param only if default value needed from packageServiceName
const parsePackageAndServiceName = (packageServiceName, getDefaultFromGrpcSchema = null) => {
    
    let serviceName = packageServiceName.substr(packageServiceName.lastIndexOf(".") + 1);
    let packageName = packageServiceName.substr(0, packageServiceName.lastIndexOf("."));
    if(getDefaultFromGrpcSchema && !packageName){
        const defaultValues =  getDefaultGrpcData(getDefaultFromGrpcSchema);
        serviceName = defaultValues.serviceName;
        packageName = defaultValues.packageName;
    }
    return {serviceName, packageName, servicePackageName: `${packageName}.${serviceName}`};
}


const getGrpcMethodsFromService = (appGrpcSchema, packageName, selectedService) => {
    if(appGrpcSchema[packageName]) {
        const services = Object.keys(appGrpcSchema[packageName]);

        if(services.length !== 0 && services.includes(selectedService)) {
            return Object.keys(appGrpcSchema[packageName][selectedService]);
        }

        
        if(services.length === 0) {
            return [];
        }
        
        // Basically if selectedService is empty then return the methods
        // based on the first item in service
        return Object.keys(appGrpcSchema[packageName][services[0]]);
    }

    return [];
};

const extractGrpcBody = (grpcData, grpcConnectionSchema) => {
    const { service, method } = grpcConnectionSchema;
    const {serviceName, packageName}  = parsePackageAndServiceName(service);

    const data = grpcData[packageName][serviceName][method]['data'];

    if(!isValidJSON(data)) {
        const errorMessage = "Grpc data should be valid JSON object";
        throw new Error(errorMessage);    
    }

    return JSON.parse(data);
};

const getGrpcDataForSelectedValues = (grpcData, packageName, selectedService, selectedMethod) => {
    if(grpcData[packageName]) {
        const services = Object.keys(grpcData[packageName]);

        if(services.length !== 0 && services.includes(selectedService)) {
            const methods = Object.keys(grpcData[packageName][selectedService]);

            if(methods.length !== 0 && methods.includes(selectedMethod)) {
                const { data }  = grpcData[packageName][selectedService][selectedMethod];
                // data is already string
                return data || JSON.stringify({});
            }

        }
    }

    return JSON.stringify({});
};

const mergeApplicationProtoFiles = (protoData) => {
    const files = Object.keys(protoData);
    let mergedProtoData = {};

    if(files && files.length !== 0) {
        files.forEach(key => {
            const protoDataSchemas = protoData[key];
            
            // check if there are entries other than the package,
            // and that they also contain service/method schemas,
            // and only then add them
            let serviceMethodPresent = false; 
            Object.entries(protoDataSchemas).forEach(([k, v]) => {
                if (k !== "package" && v !== null && typeof(v) === "object") {
                    serviceMethodPresent = true
                }
            })
            if (!serviceMethodPresent) {
                return
            }

            const packageName = protoDataSchemas.package || "_"; //Some key value will be required in package name
            mergedProtoData[packageName] = { ...mergedProtoData[packageName], ...protoDataSchemas };
        });
    }
    return mergedProtoData;
}

const getMethodsAndDataFromService = (service, serviceObject, currentGrpcServices) => {
    const EMPTY_STRING = "";
    const EMPTY_OBJECT_STRINGIFIED = "{}";
    const methods = Object.keys(serviceObject[service]);

    const constructedObjectForService = {};

    if(methods.length === 0) {
        // if there are no methods
        // then return methods and data in current grpc data
        return currentGrpcServices;
    }

    const methodsInService = serviceObject[service];
    const methodsInCurrentData = currentGrpcServices ? currentGrpcServices[service] : '';;
    
    methods.forEach((method) => {
        constructedObjectForService[method] = {
          data: // If there is data already present return the same data or else return input schema
                methodsInCurrentData 
                && 
                methodsInCurrentData[method]
                &&
                (
                    methodsInCurrentData[method].data !== EMPTY_STRING 
                    || methodsInCurrentData[method].data !== EMPTY_OBJECT_STRINGIFIED
                )
                ? methodsInCurrentData[method].data
                : JSON.stringify(methodsInService[method].inputSchema, undefined, 4)
        };
    });

    return constructedObjectForService;
};

const setGrpcDataFromDescriptor = (data, currentGrpcData) => {
    const dataEntries = Object.entries(data);
    if(dataEntries.length == 0){
        return currentGrpcData;
    }

    const grpcDataObject = {}
    dataEntries.forEach(([packageName, serviceObject]) => {
        const constructedServiceObject = {};
        delete serviceObject['package']; // TODO: Update this later

        if (!packageName || !serviceObject) {
            // if packageName is undefined or service facets are undefined, skip
            return;
        }

        const services = Object.keys(serviceObject);
        services.forEach((service) => 
                constructedServiceObject[service] = getMethodsAndDataFromService(
                                                            service, 
                                                            serviceObject, 
                                                            currentGrpcData[packageName]
                                                            )
            );

        grpcDataObject[packageName] = constructedServiceObject
    });

    return _.isEmpty(grpcDataObject) ? currentGrpcData : grpcDataObject;
};


const getGrpcSchema = (data, currentGrpcSchema) => {
    const defaultValues =  getDefaultGrpcData(data);
    const endpoint = currentGrpcSchema?.endpoint || "";
    return{
        endpoint,
        service: currentGrpcSchema?.service || defaultValues.servicePackageName,
        method : currentGrpcSchema?.method || defaultValues.method
    }
}

const getGrpcSchemaFromApiPath = (apiPath) => {
    const lastSlash = apiPath.lastIndexOf("/");
    const servicePackageName = apiPath.substr(0, lastSlash);
    const method = apiPath.substr(lastSlash + 1);

    const lastDotIndex = servicePackageName.lastIndexOf(".");
    const serviceName = servicePackageName.substr(lastDotIndex + 1);
    const packageName = servicePackageName.substr(0, lastDotIndex);
    return {
        service: servicePackageName,
        method,
        endpoint: "",
        packageName,
        serviceName
    }
}
const getGrpcSchemaFromMetaData = (savedConnectionSchema) => {
    let {app, service, method, endpoint } = JSON.parse(savedConnectionSchema);
    let { packageName, serviceName } = parsePackageAndServiceName(service);
    if(packageName){
        return {
            service,
            method,
            endpoint,
            packageName,
            serviceName
        }
    }else{
        //For backward compatibility
        packageName = endpoint.substr(endpoint.lastIndexOf("/") + 1);
        endpoint = endpoint.substr(0, endpoint.lastIndexOf("/"));
        serviceName = service;
        service = `${packageName}.${service}`;
        return {
            service,
            method,
            endpoint,
            packageName,
            serviceName
        }
    }
}

const getConnectionSchemaFromMetadataOrApiPath = (metaData, apiPath) => {
    if(metaData) {
        return getGrpcSchemaFromMetaData(metaData);
    }
    else if(apiPath){
        return getGrpcSchemaFromApiPath(apiPath);
    }else{
        return {
            service: "",
            endpoint:"",
            method: ""
        };
    }
}

const applyGrpcDataToRequestObject = (grpcDataFromRequest, metaData, apiPath = "") => {
    const schemaValues = getConnectionSchemaFromMetadataOrApiPath(metaData, apiPath);
    if(schemaValues.packageName){
        const grpcData = {
            [schemaValues.packageName]: {
                [schemaValues.serviceName]: {
                    [schemaValues.method]: {
                        data: grpcDataFromRequest
                    }
                }
            }
        };
        return grpcData;
    }
    
    return {};
    
};

const getRequestUrlFromSchema = (grpcConnectionSchema) => {
    const { endpoint, service, method } = grpcConnectionSchema;
    return `${endpoint}/${service}/${method}`;
}; 

export {
    isValidJSON,
    isRequestTypeGrpc,
    getGrpcMethodsFromService,
    applyGrpcDataToRequestObject,
    extractGrpcBody,
    mergeApplicationProtoFiles,
    setGrpcDataFromDescriptor,
    getGrpcDataForSelectedValues,
    getRequestUrlFromSchema,
    parsePackageAndServiceName,
    getGrpcSchema,
    getGrpcSchemaFromMetaData,
    getConnectionSchemaFromMetadataOrApiPath
};