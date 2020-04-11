
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

export { 
    generateServiceOptionsFromFacets,
    generateApiOptionsFromFacets,
};