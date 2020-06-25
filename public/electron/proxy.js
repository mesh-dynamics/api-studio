/**
 * PROXY SERVER CODE
 */
const httpProxy = require('http-proxy');
const logger = require('electron-log');

const mockApiPrefix = '/api/ms/mockWithCollection';
const rewriteMockPath = (resourcePath, collectionId, recordingId) => {
    // Pure function. Does not modify parameters
    
    logger.info('Intercepted Resource URI :', resourcePath);

    const path = `${mockApiPrefix}/${collectionId}/${recordingId}${resourcePath}`;

    logger.info('Updated Resource URI : ', path);

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
        const { collectionId, traceId, spanId, recordingId, service, apiPath } = mockContext;
        const { accessToken, tokenType } = user;
        const token = `${tokenType} ${accessToken}`;

        logger.info('Request Intercepted. Removing Header <Origin>');
        proxyReq.removeHeader('Origin');
        
        // Set mandatory custom headers
        logger.info('Setting custom header x-b3-traceid', traceId);
        proxyReq.setHeader('x-b3-traceid', traceId);

        logger.info('Setting custom header x-b3-spanid', spanId);
        proxyReq.setHeader('x-b3-spanid', spanId);

        // logger.info('Setting authorization header authorization:', token);
        // proxyReq.setHeader('authorization', token);

        // rewrite request url
        logger.info('Rewritting url...');
        proxyReq.path = rewriteMockPath(proxyReq.path, collectionId, recordingId);

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