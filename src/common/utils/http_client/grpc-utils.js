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

const getGrpcMethodsFromService = (appGrpcSchema, selectedApp, selectedService) => {
    if(appGrpcSchema[selectedApp]) {
        const services = Object.keys(appGrpcSchema[selectedApp]);

        if(services.length !== 0 && services.includes(selectedService)) {
            return Object.keys(appGrpcSchema[selectedApp][selectedService]);
        }

        
        if(services.length === 0) {
            return [];
        }
        
        // Basically if selectedService is empty then return the methods
        // based on the first item in service
        return Object.keys(appGrpcSchema[selectedApp][services[0]]);
    }

    return [];
};

const extractGrpcBody = (grpcData, grpcConnectionSchema) => {
    const { app, service, method } = grpcConnectionSchema;

    const data = grpcData[app][service][method]['data'];

    if(!isValidJSON(data)) {
        const errorMessage = "Grpc data should be valid JSON object";
        alert(errorMessage);
        throw new Error(errorMessage);    
    }

    return JSON.parse(data);
};

const getGrpcDataForSelectedValues = (grpcData, selectedApp, selectedService, selectedMethod) => {
    if(grpcData[selectedApp]) {
        const services = Object.keys(grpcData[selectedApp]);

        if(services.length !== 0 && services.includes(selectedService)) {
            const methods = Object.keys(grpcData[selectedApp][selectedService]);

            if(methods.length !== 0 && methods.includes(selectedMethod)) {
                const { data }  = grpcData[selectedApp][selectedService][selectedMethod];
                // data is already string
                if(data) {
                    return data; //JSON.stringify(data, undefined, 4);
                }

                return JSON.stringify({});
                
            }

        }
    }

    return JSON.stringify({});
};

const mergeApplicationProtoFiles = (protoData) => {
    const files = Object.keys(protoData);
    let mergedProtoData = {};

    if(files && files.length !== 0) {
        files.forEach(key => (mergedProtoData = { ...mergedProtoData, ...protoData[key]}));

        return mergedProtoData;
    }

    return {};
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
    const dataEntries = Object.entries(data)[0];

    const constructedServiceObject = {};

    const appName = dataEntries[0];

    const serviceObject = dataEntries[1];

    delete serviceObject['package']; // TODO: Update this later

    if (!appName || !serviceObject) {
        // if appName is undefined or service facets are undefined return currentGrpcData
        return currentGrpcData;
    }

    const services = Object.keys(serviceObject);

    services.forEach(
        (service) => 
            constructedServiceObject[service] = getMethodsAndDataFromService(
                                                        service, 
                                                        serviceObject, 
                                                        currentGrpcData[appName]
                                                        )
        );

    const grpcDataObject = {
        [appName]: constructedServiceObject
    };

    return grpcDataObject;
};

const applyGrpcDataToRequestObject = (grpcDataFromRequest, savedConnectionSchema) => {
    if(!savedConnectionSchema) {
        return {};
    }

    const { app, service, method } = JSON.parse(savedConnectionSchema);

    const grpcData = {
        [app]: {
            [service]: {
                [method]: {
                    data: grpcDataFromRequest
                }
            }
        }
    };
    
    return grpcData;
};

const getRequestUrlFromSchema = (grpcConnectionSchema) => {
    const { endpoint, service, method } = grpcConnectionSchema;
    return `${endpoint}.${service}/${method}`;
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
    getRequestUrlFromSchema
};