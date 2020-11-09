const url = require('url');
const logger = require('electron-log');
const cryptoRandomString = require('crypto-random-string');
const { 
    extractRequestPayloadDetailsFromProxy, 
    extractResponsePayloadDetailsFromProxy 
} = require('./payload-formatter');

const constructEventDetails = (mockContext, service, apiPath, eventName) => {
    const { traceId, selectedApp, customerName, collectionId, runId, spanId } = mockContext;
    const parsedApiPath = url.parse(apiPath);
    return {
        runId, // context
        traceId, // context
        service, // picked from request
        apiPath: parsedApiPath.pathname, // picked from request
        app: selectedApp, // context
        collection: collectionId, // context
        customerId: customerName, // context
        eventType: eventName, // picked from params passed from event type Request or Response
        parentSpanId: spanId, // context - putting span id as parent span id
        instanceId: 'devtool-proxy', // Hardcoded for live request
        metaData: {}, // Hardcoded for live request
        spanId: cryptoRandomString({ length: 16 }), // Randomly picked for live request
        recordingType: 'History', // Hardcoded for live request
        reqId: 'NA', // Hardcoded for live request
        runType: 'DevToolProxy', // Hardcoded for live request 
        timestamp: new Date().valueOf(), // Calculated dynamically for each req/res
    }
};

const transformForCollection = (proxyRes, options, responseBody) => {

    const apiPath = proxyRes.req.path;

    const { mockContext, service } = options;

    const httpRequestEventDetails = constructEventDetails(mockContext, service, apiPath, 'HTTPRequest');

    const httpResponseEventDetails = constructEventDetails(mockContext, service, apiPath, 'HTTPResponse');

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
