
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

const generateServiceOptionsFromFacets = (serviceFacets) => {
    
    if(serviceFacets && serviceFacets.length > 0) {
        return serviceFacets.map(service => service.val);
    }

    return [];
};

const generateApiOptionsFromFacets = (serviceFacets, selectedService) => {

    if(serviceFacets && serviceFacets.length > 0 && selectedService) {
        
        return serviceFacets
                .find(service => service.val === selectedService)
                .path_facets
                .map(path => path.val)
    }

    return [];
};

const validateGoldenName = (inputText) => {
    // Special characters allowed are "+", "_" and  "-" and whitespace
    const regex = /[~`*!@#$%^&()="{}'|\\[\]:;,.<>\/?]/; 

    if(!inputText) {
        return {
            goldenNameIsValid: false,
            goldenNameErrorMessage: "Recording name cannot be empty"
        }
    }

    if(inputText.trim().match(regex)) {
        return {
            goldenNameIsValid: false,
            goldenNameErrorMessage: "Special characters not allowed in test suite name"
        }
    }

    return {
        goldenNameIsValid: true,
        goldenNameErrorMessage: ""
    };
}
    
const resolveEndPoint = (hdrs, selectedApi) => {
    try {
        if(hdrs) {

            if(hdrs[":path"]) {
                return hdrs[":path"][0].split("?")[0];
            }
    
            return selectedApi;
        }
    
        return "[URL]" 
    } catch (e) {
        return "[URL]";
    }
};

export { 
    generateServiceOptionsFromFacets,
    generateApiOptionsFromFacets,
    validateGoldenName,
    resolveEndPoint
};