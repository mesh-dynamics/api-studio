const axios = require('axios');
const logger = require('electron-log');
const { transformForCollection } = require('./collection-utils');
const { store } = require('../fs-utils');

const proxyLiveResponseInterceptor = (proxyRes, req, res, options) => {
    let body = [];

    const handleResponseOnEnd = async () => {
        body = Buffer.concat(body).toString();

        logger.info('Received body in response :', body);

        const { user, mockContext } = options;

        const domain = store.get('domain');
        const host = store.get('mockHost');

        try {
            // Transform the body of response to be stored in a collection
            const transformedRequestResponseBody = transformForCollection(proxyRes, options, body);

            logger.info('Transformed request and response', JSON.stringify(transformedRequestResponseBody));

            // Add request options for storing request response
            const requestOptions = {
                headers: {
                    'Authorization': `${user.tokenType} ${user.accessToken}`,
                    'Host': `${host}`,
                    'Content-Type': 'application/json'
                },
                
            };

            logger.info('Request headers for storeEventBatch call', JSON.stringify(requestOptions));

            const url = `${domain}/api/cs/storeEventBatch`;

            logger.info('Posting request response details to :', url);
            
            // POST Format => URL, Body, Headers
            const response = await axios.post(url, transformedRequestResponseBody, requestOptions)

            logger.info(`storeEventBatch call completed returning status: ${response.status}, body: ${JSON.stringify(response.data)}`);

            // Set proxyRes headers to propagate up
            Object.keys(proxyRes.headers).forEach(headerKey => res.setHeader(headerKey, proxyRes.headers[headerKey]))

            res.statusMessage = proxyRes.statusMessage;

            res.statusCode = proxyRes.statusCode;

            res.end(body);

        } catch (error) {
            logger.info('Error in response interceptor', error);

            logger.info('Response Body: ', body);

            Object.keys(proxyRes.headers).forEach(headerKey => res.setHeader(headerKey, proxyRes.headers[headerKey]))

            res.statusMessage = proxyRes.statusMessage;
            
            res.statusCode = proxyRes.statusCode;
            
            res.end(body);
        }
    }
    
    proxyRes.on('data', (chunk) => { body.push(chunk) });

    proxyRes.on('end', handleResponseOnEnd);
};

const proxyMockResponseInterceptor = (proxyRes, req, res, options) => {
    logger.info("Response status: ", proxyRes.statusCode)
    logger.info("Response headers: ", proxyRes.headers)
    
    let body = [];

    const handleResponseOnEnd = async () => {
        body = Buffer.concat(body).toString();

        logger.info('Response body: ', body.length ? body : "(empty)");

        try {
            // Set proxyRes headers to propagate up
            Object.keys(proxyRes.headers).forEach(headerKey => res.setHeader(headerKey, proxyRes.headers[headerKey]))

            res.statusMessage = proxyRes.statusMessage;

            res.statusCode = proxyRes.statusCode;

            res.end(body);

        } catch (error) {
            logger.info('Error in response interceptor', error);
        }
    }
    
    proxyRes.on('data', (chunk) => { body.push(chunk) });

    proxyRes.on('end', handleResponseOnEnd);
};

module.exports = {proxyLiveResponseInterceptor, proxyMockResponseInterceptor};