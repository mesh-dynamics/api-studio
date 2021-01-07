const url = require('url');
const logger = require('electron-log');

const { 
    extractRequestPayloadDetailsFromProxy, 
    extractResponsePayloadDetailsFromProxy 
} = require('./payload-formatter');

const constructEventDetails = (mockContext, traceDetails, service, apiPath, eventName) => {
    const {selectedApp, customerName, collectionId, runId} = mockContext;
    const parsedApiPath = url.parse(apiPath);
    const {traceId, spanId, parentSpanId} = traceDetails
    const event = {
        runId, // context
        traceId, 
        service, // picked from request
        spanId, 
        parentSpanId,
        apiPath: parsedApiPath.pathname, // picked from request
        app: selectedApp, // context
        collection: collectionId, // context
        customerId: customerName, // context
        eventType: eventName, // picked from params passed from event type Request or Response
        instanceId: 'devtool-proxy', // Hardcoded for live request
        metaData: {}, // Hardcoded for live request
        recordingType: 'History', // Hardcoded for live request
        reqId: 'NA', // Hardcoded for live request
        runType: 'DevToolProxy', // Hardcoded for live request 
        timestamp: new Date().valueOf(), // Calculated dynamically for each req/res
    }
    return event
};

const transformForCollection = (proxyRes, options, responseBody) => {

    const apiPath = proxyRes.req.path;

    const { mockContext, service, traceDetails } = options;

    const httpRequestEventDetails = constructEventDetails(mockContext, traceDetails, service, apiPath, 'HTTPRequest');

    const httpResponseEventDetails = constructEventDetails(mockContext, traceDetails, service, apiPath, 'HTTPResponse');

    const requestPayloadDetails = extractRequestPayloadDetailsFromProxy(proxyRes, apiPath, options);
    
    const responsePayloadDetails = extractResponsePayloadDetailsFromProxy(proxyRes, responseBody);

    const requestResponseFormattedData = {   
        request: {
            ...httpRequestEventDetails,
            payload: ["HTTPRequestPayload", requestPayloadDetails]
        },
        response: {
            ...httpResponseEventDetails,
            payload: ["HTTPResponsePayload", responsePayloadDetails]
        }
    }

    // Note wrapped in an array - intentional
    return [requestResponseFormattedData];
};

module.exports = {
    transformForCollection
};
