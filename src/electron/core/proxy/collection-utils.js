const url = require('url');
const logger = require('electron-log');

const { 
    extractRequestPayloadDetailsFromProxy, 
    extractResponsePayloadDetailsFromProxy 
} = require('./payload-formatter');
const cryptoRandomString = require('crypto-random-string');

const constructEventDetails = (mockContext, traceDetails, service, apiPath, eventName, requestId) => {
    const {selectedApp, customerName, collectionId, runId, strictMock, replayInstance, replayCollection} = mockContext;
    const parsedApiPath = url.parse(apiPath);
    const {traceIdDetails, spanId, parentSpanId} = traceDetails
    const {traceIdForEvent} = traceIdDetails

    const event = {
        runId, // context
        traceId: traceIdForEvent, 
        service, // picked from request
        spanId, 
        parentSpanId,
        apiPath: parsedApiPath.pathname, // picked from request
        app: selectedApp, // context
        collection: strictMock ? replayCollection : collectionId, // context
        customerId: customerName, // context
        eventType: eventName, // picked from params passed from event type Request or Response
        instanceId: strictMock ? replayInstance : 'devtool-proxy', // Hardcoded for live request
        metaData: {}, // Hardcoded for live request
        recordingType: strictMock ? 'Replay' : 'History', // Hardcoded for live request // todo: change to UserGolden if collection type isn't history
        reqId: requestId,//'NA', // Hardcoded for live request
        runType: 'DevToolProxy', // Hardcoded for live request 
        timestamp: Date.now() / 1000, // Calculated dynamically for each req/res
    }
    return event
};

const transformForCollection = (proxyRes, options, responseBody) => {


    const { mockContext, service, traceDetails, outgoingApiPath: apiPath } = options;

    const requestId = cryptoRandomString({length:32});
    const httpRequestEventDetails = constructEventDetails(mockContext, traceDetails, service, apiPath, 'HTTPRequest', requestId);

    const httpResponseEventDetails = constructEventDetails(mockContext, traceDetails, service, apiPath, 'HTTPResponse', requestId);

    const requestPayloadDetails = extractRequestPayloadDetailsFromProxy(proxyRes, apiPath, options);
    
    const responsePayloadDetails = extractResponsePayloadDetailsFromProxy(proxyRes, responseBody);

    const requestResponseFormattedData = [   
        // request
        {
            ...httpRequestEventDetails,
            payload: ["HTTPRequestPayload", requestPayloadDetails]
        },

        // response
        {
            ...httpResponseEventDetails,
            payload: ["HTTPResponsePayload", responsePayloadDetails]
        }
    ]

    // Note wrapped in an array - intentional
    return requestResponseFormattedData;
};

module.exports = {
    transformForCollection
};
