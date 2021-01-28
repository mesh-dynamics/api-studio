const axios = require('axios');
const logger = require('electron-log');
const { transformForCollection } = require('./collection-utils');
const { store } = require('../fs-utils');
const url = require("url");

const proxyLiveResponseInterceptor = (proxyRes, req, res, options) => {
    let body = [];

    const handleResponseOnEnd = () => {
        body = Buffer.concat(body).toString();

        logger.info('Received body in response :', body);

        const { user, mockContext } = options;

        const domain = store.get('domain');
        const parsedUrl = url.parse(domain);

        // Transform the body of response to be stored in a collection
        const transformedRequestResponseBody = transformForCollection(proxyRes, options, body);

        logger.info('Transformed request and response', JSON.stringify(transformedRequestResponseBody));

        // Add request options for storing request response
        const requestOptions = {
            headers: {
                'Authorization': `${user.tokenType} ${user.accessToken}`,
                'Host': `${parsedUrl.hostname}`,
                'Content-Type': 'application/json'
            },
            
        };

        logger.info('Request headers for storeEventBatch call', JSON.stringify(requestOptions));

        const storeEventUrl = `${domain}/api/cs/storeEventBatch`;

        logger.info('Posting request response details to :', storeEventUrl);
        
        // POST Format => URL, Body, Headers
        axios.post(storeEventUrl, transformedRequestResponseBody, requestOptions).then((response) => {
            logger.info(`storeEventBatch call completed returning status: ${response.status}, body: ${JSON.stringify(response.data)}`);
        }).catch((error) => {
            logger.info('Error in response interceptor', error);
        })
    }
    
    proxyRes.on('data', (chunk) => { body.push(chunk) });

    proxyRes.on('end', handleResponseOnEnd);
};

const proxyMockResponseInterceptor = (proxyRes, req, res, options) => {
    logger.info("Mock response status: ", proxyRes.statusCode)
    logger.info("Mock response headers: ", proxyRes.headers)
    
    let body = [];

    const handleResponseOnEnd = () => {
        body = Buffer.concat(body).toString();
        logger.info('Mock response body: ', body.length ? body : "(empty)");
    }
    
    proxyRes.on('data', (chunk) => { body.push(chunk) });

    proxyRes.on('end', handleResponseOnEnd);
};

module.exports = {proxyLiveResponseInterceptor, proxyMockResponseInterceptor};