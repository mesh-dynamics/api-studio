
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
            goldenNameErrorMessage: "Special characters not allowed in golden name"
        }
    }

    return {
        goldenNameIsValid: true,
        goldenNameErrorMessage: ""
    };
};

export { 
    generateServiceOptionsFromFacets,
    generateApiOptionsFromFacets,
    validateGoldenName
};