const axios = require('axios');
const logger = require('electron-log');
const { transformForCollection } = require('./collection-utils');
const { store } = require('../fs-utils');
const url = require("url");
const { storeReqResEvent } = require('./h2server.utility');

const proxyLiveResponseInterceptor = (proxyRes, req, res, options) => {
    let body = [];

    const handleResponseOnEnd = () => {
        body = Buffer.concat(body).toString("base64");

        const responseProps = {
            method: proxyRes.req.method.toUpperCase(),
            headers: proxyRes.headers,
            statusCode: proxyRes.statusCode,
            status : proxyRes.statusMessage,
            body
        }

        storeReqResEvent(responseProps, options);
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