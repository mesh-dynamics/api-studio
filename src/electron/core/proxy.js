/**
 * PROXY SERVER CODE
 */
const httpProxy = require('http-proxy');
const find = require('find-process');
const logger = require('electron-log');
const { getApplicationConfig } = require('./fs-utils');

// Alt Mock With Collection API: /api/ms/mockWithCollection??
const mockApiPrefix = '/api/msc/mockWithRunId';

const rewriteMockPath = (resourcePath, mockContext) => {
    // Pure function. Do not modify parameters // recordingId 
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
 * @param {*} mockContext 
 * @param {*} user 
 */
const setupProxy = (mockContext, user) => {

    const { mock } = getApplicationConfig();

    const proxyServerOptions = {
        target: {
            protocol: mock.protocol, //`${mock.protocol}:`, // Do not forget the darn colon
            host: mock.host,
            port: mock.port,
        },
        changeOrigin: true,
    };

    const proxyErrorHandler = (error) => {
        logger.info('Error caught in proxy \n');
        logger.info('Proxy server options \n', proxyServerOptions);
        logger.info('Error : \n', error);
    }

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
    logger.info("Creating server with options: \n", proxyServerOptions);

    const proxy = httpProxy.createProxyServer(proxyServerOptions);

    /**
     * Proxy Event Listeners
     */

    // Error Listener
    proxy.on('error', proxyErrorHandler);

    // Request Listener
    proxy.on('proxyReq', proxyRequestInterceptor);

    // Response Listener
    // proxy.on('proxyReq', proxyResponseInterceptor);

    /**
     * Cleanup and Setup Proxy Listening
     */
    find('port', mock.proxyPort)
        .then((pList) => {
            pList.map((item) => {
                logger.info('Killing Process...', item.pid);
                process.kill(item.pid);
                logger.info('Killed process...', item.pid);
            });
            setTimeout(() => proxy.listen(mock.proxyPort), 3000);
        })
};

module.exports = {
    setupProxy
};