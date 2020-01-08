
const fetchGoldenContract = async (goldenId, service, api) => {
    try {
        const response = await fetch("http://www.mocky.io/v2/5e1460b42d00006c00166fb4");
        const data = await response.json();
    
        return data;
    } catch(e) {
        console.log("Error Fetching Data From Server", e);
    }
    
};

const fetchGoldenExamples = async (goldenId, service, api, selectedPageNumber) => {
    try {
        const response = await fetch("http://www.mocky.io/v2/5dfc54bb3100006d00d2be1e");
        const data = await response.json();
    
        return data;
    } catch(e) {
        console.log("Error Fetching Data From Server", e);
    }
    
};

export {
    fetchGoldenContract,
    fetchGoldenExamples
};