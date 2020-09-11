import { allowedCaptchaDomains } from '../enums/app-config';

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

const isAllowedDomain = () => allowedCaptchaDomains.includes(window.location.hostname);

export { 
    processInstanceList, 
    getAccesToken,
    isAllowedDomain,
    getRefreshToken
};