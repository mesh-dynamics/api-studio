const url = require('url');
const logger = require('electron-log');

// Alt Mock With Collection API: /api/ms/mockWithCollection??
const mockApiPrefix = '/api/msc/mockWithRunIdTS';
const strictMockApiPrefix = '/api/ms'

/**
 * Rewrites outgoing proxy path with mock configuration
 * @param {*} resourcePath 
 * @param {*} mockContext 
 */
const rewriteMockPath = (resourcePath, mockContext, traceDetails, service, serviceConfigObject) => {
    // Pure function. Do not modify parameters // recordingId 
    const {
        runId,
        selectedApp,
        collectionId, 
        customerName,
        recordingCollectionId,
        strictMock,
        replayInstance,
    } = mockContext;
    const timestamp = new Date(Date.now()).toISOString();
    const {traceIdDetails: {traceIdForEvent}} = traceDetails;

    logger.info('Intercepted Resource URI :', resourcePath);

    // Path for mock with collection [Do Not Delete]
    // const path = `${mockApiPrefix}/${collectionId}/${recordingId}${resourcePath}`;

    // Path for mock
    let path = "", strippedResourcePath = resourcePath.substring(1); // remove leading slash

    // strip service name if no prefix is provided in service config OR if 'All services mocked' is selected
    if(serviceConfigObject && !serviceConfigObject.servicePrefix || !serviceConfigObject) {
        strippedResourcePath = strippedResourcePath.substring(`${service}/`.length)
    }
    logger.info("Stripped resource path: ", strippedResourcePath)

    if(strictMock) {
        path = `${strictMockApiPrefix}/${customerName}/${selectedApp}/${replayInstance}/${service}/${strippedResourcePath}`
    } else {
        path = `${mockApiPrefix}/${collectionId}/${recordingCollectionId}/${customerName}/${selectedApp}/${timestamp}/${traceIdForEvent}/${runId}/${service}/${strippedResourcePath}`;
    }

    logger.info('Updated Resource URI : ', path);
    logger.info("Detected RunId", runId);

    return path;
}

/**
 * Rewrites live outgoing prox ypath with additional config from config object
 * @param {*} serviceConfigObject Object - Service Config 
 * @param {*} receivedPathInProxy String - API Path 
 */
const rewriteLivePath = (serviceConfigObject, outgoingApiPath) => {
    const { url: configUrl } = serviceConfigObject;

    const parsedConfigUrl = url.parse(configUrl);

    logger.info('Parsed Config Url ', parsedConfigUrl);

    const constructedApiPath = parsedConfigUrl.path === '/' 
                                ? `/${outgoingApiPath}` // If path is just '/' return the outgoing proxy path 
                                : `${parsedConfigUrl.path}/${outgoingApiPath}`; // else, merge both paths

    logger.info('Updated API Path for proxy : ', constructedApiPath);

    return constructedApiPath;
}

/**
 * Request interceptor for mock service
 * @param {*} proxyReq 
 * @param {*} mockContext 
 * @param {*} user 
 */
const proxyRequestInterceptorMockService = (proxyReq, mockContext, user, traceDetails, service, serviceConfigObject) => {
    const { accessToken, tokenType } = user;
    const { selectedApp, strictMock  } = mockContext;
    const {traceKeys, spanId, traceIdDetails, parentSpanId} = traceDetails
    const token = `${tokenType} ${accessToken}`;
    const {traceId} = traceIdDetails
    
    logger.info('Mock Request Intercepted. Removing Header <Origin>');
    proxyReq.removeHeader('Origin');

    logger.info('Setting authorization header authorization:', token);
    proxyReq.setHeader('authorization', token);

    const {traceIdKey, spanIdKey, parentSpanIdKeys} = traceKeys;

    if (spanIdKey) {
        logger.info(`Setting spanId header (${spanIdKey}): `, spanId);
        proxyReq.setHeader(spanIdKey, spanId);
    }

    parentSpanIdKeys.forEach((key) => {
        logger.info(`Setting parentSpanId header (${key}): `, parentSpanId);
        proxyReq.setHeader(key, parentSpanId);
    })

    if (traceIdKey) {
        logger.info(`Setting traceId header (${traceIdKey}): `, traceId);
        proxyReq.setHeader(traceIdKey, traceId);
    }

    if (!strictMock) {
        logger.info('Setting dynamicInjectionConfigVersion', `Default${selectedApp}`);
        proxyReq.setHeader('dynamicInjectionConfigVersion', `Default${selectedApp}`);
    }

    // rewrite request url
    logger.info('Rewriting url...');
    proxyReq.path = rewriteMockPath(proxyReq.path, mockContext, traceDetails, service, serviceConfigObject);

    logger.info('Method intercepted in proxy request:', proxyReq.method);
    logger.info('Logging Request Headers\n', proxyReq._headers);
};

/**
 * Request Interceptor for live service
 * @param {*} proxyReq 
 * @param {*} serviceConfigObject 
 */
const proxyRequestInterceptorLiveService = (proxyReq, serviceConfigObject, mockContext, traceDetails, outgoingApiPath) => {
    const {traceKeys, traceIdDetails, spanId, parentSpanId} = traceDetails
    const {traceId} = traceIdDetails

    logger.info('Method intercepted in proxy request:', proxyReq.method);

    logger.info('Resource Url Path Recieved for Live Service', proxyReq.path);

    const {traceIdKey, spanIdKey, parentSpanIdKeys} = traceKeys;

    if (spanIdKey && !(spanIdKey in proxyReq._headers)) {
        logger.info(`Setting spanId header (${spanIdKey}): `, spanId);
        proxyReq.setHeader(spanIdKey, spanId);
    }

    parentSpanIdKeys.forEach((key) => {
        if(!(key in proxyReq._headers)) {
            logger.info(`Setting parentSpanId header (${key}): `, parentSpanId);
            proxyReq.setHeader(key, parentSpanId);
        }
    })

    if (traceIdKey && !(traceIdKey in proxyReq._headers)) {
        logger.info(`Setting traceId header (${traceIdKey}): `, traceId);
        proxyReq.setHeader(traceIdKey, traceId);
    }

    logger.info('Url received in config', serviceConfigObject.url);

    logger.info('Logging Request Headers for Live Service', proxyReq._headers);

    logger.info('Rewriting Live url path');
    proxyReq.path = rewriteLivePath(serviceConfigObject, outgoingApiPath);
};

module.exports = { 
    proxyRequestInterceptorMockService,
    proxyRequestInterceptorLiveService
};