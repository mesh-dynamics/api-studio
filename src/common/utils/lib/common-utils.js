const processInstanceList = (instanceList) => {
    // TODO: Plucked from services. Refactoring this function    
    // Below code not to be removed now. 
    // if (pageLocationURL.indexOf('.prod.v2.') != -1) {
    //                 for (const il of iList) {
    //                     il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.v2.");
    //                 }
    //             } else if (pageLocationURL.indexOf('.prod.') != -1) {
    //                 for (const il of iList) {
    //                     il.gatewayEndpoint = il.gatewayEndpoint.replace(".dev.", ".prod.");
    //                 }
    //             }
    
    const pageLocationURL = window.location.href;
    
    if (pageLocationURL.indexOf('.prod.v2.') != -1) {
        return instanceList.map(item => {
            item.gatewayEndpoint.replace(".dev.", ".prod.v2.");
            return item;
        });
    }
    
    if (pageLocationURL.indexOf('.prod.') != -1) {
        return instanceList.map(item => {
            item.gatewayEndpoint.replace(".dev.", ".prod.");
            return item;
        });
    }

    return instanceList;
};

const getAccesToken = (state) => (state.authentication.user.access_token);

const getRefreshToken = (state) => (state.authentication.user.refresh_token);

const isCaptchaEnabled = (configList) => {
    const config = configList.find(configItem => configItem.key === 'captcha-config');

    if(config) {
        const configValue = JSON.parse(config.value);

        return configValue.captchaEnabled;
    }

    return false;
};

const getDomainNameFromHostname = (hostname) => {
    const hostNameParts = hostname.split('.');

    const domain = hostNameParts.slice(hostNameParts.length - 2, hostNameParts.length).join('.');

    return domain;
};

export { 
    processInstanceList, 
    getAccesToken,
    getRefreshToken,
    isCaptchaEnabled,
    getDomainNameFromHostname
};

// Do not delete. Sample curl insert captcha for a customer
// curl --location --request POST 'https://demo.dev.cubecorp.io/api/config/insert' \
// --header 'Connection: keep-alive' \
// --header 'Accept: application/json, text/plain, */*' \
// --header 'DNT: 1' \
// --header 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.83 Safari/537.36' \
// --header 'Content-Type: application/json;charset=UTF-8' \
// --header 'Sec-Fetch-Site: cross-site' \
// --header 'Sec-Fetch-Mode: cors' \
// --header 'Sec-Fetch-Dest: empty' \
// --header 'Accept-Language: en-US,en;q=0.9' \
// --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTYwMTI2ODIwNCwiZXhwIjoxNjAxMzU0NjA0fQ._ZUzuLDYij7hIr16Tm5mp3s5lEFu7uRcO97vn1xQolI' \
// --data-raw '{
//     "customer": "CubeCorp",
//     "configType": "captcha",
//     "key": "captcha-config",
//     "value": "{\"captchaEnabled\":true}"
// }
// '