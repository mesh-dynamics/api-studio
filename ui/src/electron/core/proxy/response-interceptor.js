/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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