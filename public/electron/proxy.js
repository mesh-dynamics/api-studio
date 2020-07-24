/**
 * PROXY SERVER CODE
 */
const httpProxy = require('http-proxy');
const logger = require('electron-log');

// Alt Mock With Collection API: /api/ms/mockWithCollection??
const mockApiPrefix = '/api/msc/mockWithRunId';

const rewriteMockPath = (resourcePath, mockContext) => {
    // Pure function. Does not modify parameters // recordingId 
    const {
        traceId, 
        selectedApp,
        collectionId, 
        customerName,
        recordingCollectionId,
        runId,
    } = mockContext;
    
    logger.info('Intercepted Resource URI :', resourcePath);

    // Path for mock with collection [Do Not Delete]
    // const path = `${mockApiPrefix}/${collectionId}/${recordingId}${resourcePath}`;

    // Path for mock
    const path = `${mockApiPrefix}/${collectionId}/${recordingCollectionId}/${customerName}/${selectedApp}/${traceId}/${runId}${resourcePath}`;

    logger.info('Updated Resource URI : ', path);
    logger.info("runId", runId);
    return path;
}

/**
 * This function will setup the proxy server
 * @param {*} proxyServerOptions 
 * @param {*} mockContext 
 * @param {*} user 
 */
const setupProxy = (proxyServerOptions, mockContext, user) => {
    /**
     * Listener for proxy request interceptor
     * @param {*} proxyReq 
     * @param {*} req 
     * @param {*} res 
     * @param {*} options 
     */
    const proxyRequestInterceptor = (proxyReq) => {
        const { accessToken, tokenType } = user;
        const token = `${tokenType} ${accessToken}`;

        logger.info('Request Intercepted. Removing Header <Origin>');
        proxyReq.removeHeader('Origin');

        logger.info('Setting authorization header authorization:', token);
        proxyReq.setHeader('authorization', token);

        // rewrite request url
        logger.info('Rewriting url...');
        proxyReq.path = rewriteMockPath(proxyReq.path, mockContext);

        logger.info('Logging Request Headers\n', proxyReq._headers);
    };

    /**
     * Listener for proxy response interceptor
     * @param {*} proxyRes 
     * @param {*} req 
     * @param {*} res 
     */
    /*

    const proxyResponseInterceptor = (proxyRes, req, res) => {
        logger.info('RAW Response from the target***********');
        logger.info('Proxy Res *******\n', proxyRes);
        logger.info('Req--------\n', req);
        logger.info('Res--------\n', res);
    };

    */

    const proxy = httpProxy.createProxyServer(proxyServerOptions);

    /**
     * Proxy Event Listeners
     */
    // Request Listener
    proxy.on('proxyReq', proxyRequestInterceptor);

    // Response Listener
    // proxy.on('proxyReq', proxyResponseInterceptor);

    /**
     * Setup Proxy Listening
     */
    proxy.listen(9000);
};

module.exports = {
    setupProxy
};