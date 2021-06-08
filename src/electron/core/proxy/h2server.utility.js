
const axios = require('axios');
const logger = require('electron-log');
const { transformForCollection } = require('./collection-utils');
const { store } = require('../fs-utils');
const url = require("url");


    const storeReqResEvent = (responseProps, options) => {
        logger.info('Received body in response :', responseProps.body);

        const { user } = options;

        const domain = store.get('domain');
        const parsedUrl = url.parse(domain);

        // Transform the body of response to be stored in a collection
        const transformedRequestResponseBody = transformForCollection(responseProps, options, responseProps.body);

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

    // for non-https, the fetch-h2 library defaults to http1.1, so changing it to http2 for non-https calls to force http2
    const getHttp2FetchUrl = (fetchUrl) => {
        if (fetchUrl.startsWith("http://")) {
            fetchUrl = fetchUrl.replace("http://", "http2://");
        }
        return fetchUrl;
    }

    module.exports = {storeReqResEvent, getHttp2FetchUrl};