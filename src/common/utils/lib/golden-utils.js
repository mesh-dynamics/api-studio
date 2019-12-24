const generateServiceOptionsFromTimeLine = (timelineResult) => {

    if(timelineResult) {
        const { results } = timelineResult;
        
        const services = Array.from(new Set(results.map(result => result.service))).filter(Boolean);

        return services;
    }

    return [];

};

const generateApiOptionsFromTimeLine = (timelineResult, selectedService) => {
    if(timelineResult && selectedService) {
        const  { results } = timelineResult;
        
        const apiPath = Array.from(
            results
                .filter(result => result.service === selectedService)
                .map(item => item.path)
                .filter(Boolean));
                
        return apiPath;
    }
    return [];
};

export { 
    generateServiceOptionsFromTimeLine,
    generateApiOptionsFromTimeLine
};