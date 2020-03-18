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
    const timeStamp = decodeURI(urlParameters["timeStamp"]);
    
    return constructUrlParams({
        app, 
        service, 
        replayId, 
        timeStamp,
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
        timeStamp, 
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
        currentPageNumber = 0
    } = params;

    return `?replayId=${replayId}&app=${app}&apiPath=${selectedAPI}&service=${selectedService}&recordingId=${recordingId}&currentTemplateVer=${currentTemplateVer}&timeStamp=${timeStamp}&selectedReqRespMatchType=${selectedReqRespMatchType}&selectedResolutionType=${selectedResolutionType}&searchFilterPath=${searchFilterPath}&requestHeaders=${requestHeaders}&requestParams=${requestParams}&requestBody=${requestBody}&responseHeaders=${responseHeaders}&responseBody=${responseBody}&currentPageNumber=${currentPageNumber}`;
};

const updateSearchHistoryParams = (metaDataType, value, state) => {
    const { app, replayId, timeStamp, apiPath, service, recordingId, currentTemplateVer, selectedReqRespMatchType,selectedResolutionType, searchFilterPath } = state;
    const { requestHeaders, requestParams, requestBody, responseHeaders, responseBody } = getCheckboxParams();

    const params = {
        app, 
        service, 
        replayId, 
        timeStamp,
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

    // selectedService is a special case where if 
    //the service changes the api path is reset
    if(metaDataType === "selectedService") {
        params["selectedAPI"] = "";
    }

    params[metaDataType] = value;

    return constructUrlParams(params);
};

const constructUrlParamsDiffResults = (state, isNextPage) => {
    const {
        app, replayId, timeStamp, recordingId, searchFilterPath, currentTemplateVer,
        filter : {
            selectedService, selectedAPI, 
            selectedReqMatchType, selectedDiffType, 
            selectedResolutionType,  
            startIndex, endIndex,
        },
        diffToggleRibbon: {
            showResponseMessageHeaders, // response headers
            showResponseMessageBody, // response body
            showRequestMessageHeaders, // request header
            showRequestMessageQParams, // request query params
            showRequestMessageFParams, // request form params
            showRequestMessageBody, // request body
        }
    } =  state;

    return `replayId=${replayId}&app=${app}&recordingId=${recordingId}&currentTemplateVer=${currentTemplateVer}&timeStamp=${timeStamp}&searchFilterPath=${searchFilterPath}&selectedService=${selectedService}&selectedAPI=${selectedAPI}&selectedResolutionType=${selectedResolutionType}&requestHeaders=${showRequestMessageHeaders}&requestQParams=${showRequestMessageQParams}&requestFParams=${showRequestMessageFParams}&requestBody=${showRequestMessageBody}&responseHeaders=${showResponseMessageHeaders}&responseBody=${showResponseMessageBody}&selectedReqMatchType=${selectedReqMatchType}&selectedDiffType=${selectedDiffType}&` + (isNextPage ? `startIndex=${startIndex}` : `endIndex=${endIndex}`);

}

const getTransformHeaders = (customHeaders) => {
    const headerIds = Object.keys(customHeaders);
    const [defaultItem] = customHeaders;
    const requestTransforms = {};

    // If there is only one default header... and the key is empty
    if(headerIds.length === 1 && defaultItem.key === "") {
        // Will return empty
        // return has to be object
        return { requestTransforms };
    }

    // if there are multiple header id... map it in the structure required
    headerIds.map(id => 
        requestTransforms[customHeaders[id].key] = [{ 
            source: "*",
            target: customHeaders[id].value
    }]);

    return { requestTransforms };
};

export { getTransformHeaders, constructUrlParamsDiffResults, getSearchHistoryParams, updateSearchHistoryParams };