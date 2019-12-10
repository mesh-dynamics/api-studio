import _ from 'lodash';

const getSearchHistoryParams = () => {
    const urlParameters = _.chain(window.location.search)
                        .replace('?', '')
                        .split('&')
                        .map(_.partial(_.split, _, '=', 2))
                        .fromPairs()
                        .value();

    const { requestHeaders, requestParams, requestBody, responseHeaders, responseBody } = getCheckboxParams();
    const apiPath = urlParameters["apiPath"] ? urlParameters["apiPath"]  : "%2A";
    const replayId = urlParameters["replayId"];
    const app = urlParameters["app"];
    const recordingId = urlParameters["recordingId"];
    const currentTemplateVer = urlParameters["currentTemplateVer"];
    const service = urlParameters["service"];
    const selectedReqRespMatchType = urlParameters["selectedReqRespMatchType"];
    const selectedResolutionType = urlParameters["selectedResolutionType"];
    const searchFilterPath = urlParameters["searchFilterPath"];
    
    return constructUrlParams({
        app, 
        service, 
        replayId, 
        requestBody, 
        recordingId, 
        responseBody,
        requestParams, 
        requestHeaders, 
        responseHeaders, 
        searchFilterPath, 
        currentTemplateVer, 
        selectedAPI: apiPath, 
        selectedResolutionType, 
        selectedService: service,
        selectedReqRespMatchType,
    });
}

const getCheckboxParams = () => {
    const urlParameters = _.chain(window.location.search)
                        .replace('?', '')
                        .split('&')
                        .map(_.partial(_.split, _, '=', 2))
                        .fromPairs()
                        .value();
    const requestHeaders = urlParameters["requestHeaders"] || false;
    const requestParams = urlParameters["requestParams"] || false;
    const requestBody = urlParameters["requestBody"] || false;
    const responseHeaders = urlParameters["responseHeaders"] || false;
    const responseBody = urlParameters["responseBody"] || true;

    return { requestHeaders, requestParams, requestBody, responseHeaders, responseBody };

};

const constructUrlParams = (params) => {
    const {
        app, 
        replayId, 
        requestBody, 
        selectedAPI, 
        recordingId, 
        responseBody,
        requestParams, 
        requestHeaders, 
        selectedService, 
        responseHeaders, 
        searchFilterPath, 
        currentTemplateVer, 
        selectedReqRespMatchType,
        selectedResolutionType, 
    } = params;

    return `?replayId=${replayId}&app=${app}&apiPath=${selectedAPI}&service=${selectedService}&recordingId=${recordingId}&currentTemplateVer=${currentTemplateVer}&selectedReqRespMatchType=${selectedReqRespMatchType}&selectedResolutionType=${selectedResolutionType}&searchFilterPath=${searchFilterPath}&requestHeaders=${requestHeaders}&requestParams=${requestParams}&requestBody=${requestBody}&responseHeaders=${responseHeaders}&responseBody=${responseBody}`;
};

const updateSearchHistoryParams = (metaDataType, value, state) => {
    const { app, replayId, apiPath, service, recordingId, currentTemplateVer, selectedReqRespMatchType,selectedResolutionType, searchFilterPath } = state;
    const { requestHeaders, requestParams, requestBody, responseHeaders, responseBody } = getCheckboxParams();
    const params = {
        app, 
        service, 
        replayId, 
        requestBody, 
        recordingId, 
        responseBody,
        requestParams, 
        requestHeaders, 
        responseHeaders, 
        searchFilterPath, 
        currentTemplateVer, 
        selectedAPI: apiPath, 
        selectedResolutionType, 
        selectedService: service,
        selectedReqRespMatchType,
    };

    params[metaDataType] = value;

    return constructUrlParams(params);
};

export { getSearchHistoryParams, updateSearchHistoryParams };