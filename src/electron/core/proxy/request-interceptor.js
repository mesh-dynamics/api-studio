const url = require('url');
const logger = require('electron-log');
const cryptoRandomString = require('crypto-random-string');

// Alt Mock With Collection API: /api/ms/mockWithCollection??
const mockApiPrefix = '/api/msc/mockWithRunId';

/**
 * Exclude the service name from API path
 * @param {*} apiPath - api path that contains service name as first path variable
 */
const stripServiceNameFromOutgoingProxyPath = (apiPath) => {
    if(apiPath) {
        // Exclude the service part from url
        const apiPathParts = apiPath.split('/').filter(Boolean).slice(1);

        logger.info('Api Path Parts :', apiPathParts);
        
        const updatedPath = apiPathParts.join('/');

        return updatedPath;
    }

    return '';
}

/**
 * Rewrites outgoing proxy path with mock configuration
 * @param {*} resourcePath 
 * @param {*} mockContext 
 */
const rewriteMockPath = (resourcePath, mockContext) => {
    // Pure function. Do not modify parameters // recordingId 
    const {
        runId,
        traceId, 
        selectedApp,
        collectionId, 
        customerName,
        recordingCollectionId,
    } = mockContext;
    
    logger.info('Intercepted Resource URI :', resourcePath);

    // Path for mock with collection [Do Not Delete]
    // const path = `${mockApiPrefix}/${collectionId}/${recordingId}${resourcePath}`;

    // Path for mock
    const path = `${mockApiPrefix}/${collectionId}/${recordingCollectionId}/${customerName}/${selectedApp}/${traceId}/${runId}${resourcePath}`;

    logger.info('Updated Resource URI : ', path);
    logger.info("Detected RunId", runId);

    return path;
}

/**
 * Rewrites live outgoing prox ypath with additional config from config object
 * @param {*} serviceConfigObject Object - Service Config 
 * @param {*} receivedPathInProxy String - API Path 
 */
const rewriteLivePath = (serviceConfigObject, receivedPathInProxy) => {
    const { url: configUrl } = serviceConfigObject;

    const parsedConfigUrl = url.parse(configUrl);

    logger.info('Parsed Config Url ', parsedConfigUrl);

    const outgoingProxyApiPath = stripServiceNameFromOutgoingProxyPath(receivedPathInProxy);

    const constructedApiPath = parsedConfigUrl.path === '/' 
                                ? `/${outgoingProxyApiPath}` // If path is just '/' return the outgoing proxy path 
                                : `${parsedConfigUrl.path}/${outgoingProxyApiPath}`; // else, merge both paths

    logger.info('Updated API Path for proxy : ', constructedApiPath);

    return constructedApiPath;
}

/**
 * Request interceptor for mock service
 * @param {*} proxyReq 
 * @param {*} mockContext 
 * @param {*} user 
 */
const proxyRequestInterceptorMockService = (proxyReq, mockContext, user) => {
    const { accessToken, tokenType } = user;
    const { spanId, traceId } = mockContext;
    const randomSpanId = cryptoRandomString({length: 16});
    const token = `${tokenType} ${accessToken}`;

    logger.info('Mock Request Intercepted. Removing Header <Origin>');
    proxyReq.removeHeader('Origin');

    logger.info('Setting authorization header authorization:', token);
    proxyReq.setHeader('authorization', token);

    logger.info('Setting x-b3-spanid: ', randomSpanId);
    proxyReq.setHeader('x-b3-spanid', randomSpanId);

    logger.info('Setting x-b3-parentspanid: ', spanId);
    proxyReq.setHeader('x-b3-parentspanid', spanId);

    logger.info('Setting baggage-parent-span-id: ', spanId);
    proxyReq.setHeader('baggage-parent-span-id', spanId);

    logger.info('Setting x-b3-traceid: ', traceId);
    proxyReq.setHeader('x-b3-traceid', traceId);

    // rewrite request url
    logger.info('Rewriting url...');
    proxyReq.path = rewriteMockPath(proxyReq.path, mockContext);

    logger.info('Method intercepted in proxy request:', proxyReq.method);
    logger.info('Logging Request Headers\n', proxyReq._headers);
};

/**
 * Request Interceptor for live service
 * @param {*} proxyReq 
 * @param {*} serviceConfigObject 
 */
const proxyRequestInterceptorLiveService = (proxyReq, serviceConfigObject) => {

    logger.info('Method intercepted in proxy request:', proxyReq.method);

    logger.info('Resource Url Path Recieved for Live Service', proxyReq.path);

    logger.info('Logging Request Headers for Live Service', proxyReq._headers);

    logger.info('Url received in config', serviceConfigObject.url);

    logger.info('Rewriting Live url path');
    proxyReq.path = rewriteLivePath(serviceConfigObject, proxyReq.path);
};

module.exports = { 
    proxyRequestInterceptorMockService,
    proxyRequestInterceptorLiveService
};