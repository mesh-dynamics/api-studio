const transformResponseContractToTable = (contract) => {
    try {
        // Get All Keys
        const allKeys = Object.keys(contract);

        // Remove any key that is  status for hdrs and take only the ones related to body
        const filteredBodyKeys = allKeys.filter(key => !(key.includes("hdrs") || key.includes("status"))).sort();

        // construct the table structure
        const responseBodyData = filteredBodyKeys.map(key => {
            return {
                keyName: contract[key].path ||  "...{",
                type: contract[key].dt,
                presence: contract[key].pt,
                transformation: contract[key].em,
                comparison: contract[key].ct
            };
        });

        return responseBodyData;
    } catch (e) {
        console.log("Error constructing tabular data", e);
        return [];
    }
    
};

const transformResponseContractToJson = (contract) => {
    try {
        const responseBodyData = {};
        // Get All Keys
        const allKeys = Object.keys(contract).sort();

        // Remove any key that is  status for hdrs and take only the ones related to body
        const filteredBodyKeys = allKeys.filter(key => !(key.includes("hdrs") || key.includes("status")));

        // construct the json structure
        filteredBodyKeys.map(key => responseBodyData[key] = contract[key]);

        return responseBodyData;
    } catch (e) {
        console.log("Error constructing tabular data", e);
        return {};
    }
};

const transformToTable = (rules) => {
    
    const ruleKeys = Object.keys(rules);
    
    // construct the table structure
    const tableStructure = ruleKeys.map(key => {
        return {
            keyName: rules[key].path ||  "...{",
            type: rules[key].dt,
            presence: rules[key].pt,
            transformation: rules[key].em,
            comparison: rules[key].ct
        };
    });

    return tableStructure;
    
}

const transformRequestContract = (contract) => {
    const headers = {};
    const queryParams = {};
    const formParams = {};
    const body = {};

    // If rules are not present or if the rules are 
    // empty object return null to not render the component
    if(contract && Object.keys(contract).length > 0) {
        Object
            .keys(contract)
            .sort()
            .forEach(key => {
                
                if(key.includes("hdrs")) {
                    headers[key] = contract[key];
                }

                if(key.includes("queryParams")) {
                    queryParams[key] = contract[key];
                }

                if(key.includes("formParams")) {
                    formParams[key] = contract[key];
                }

                if (key.includes("body")) {
                    body[key] = contract[key];
                }
        });

        

        return {
            body: { table: transformToTable(body), json: body },
            headers: { table: transformToTable(headers), json: headers },
            formParams: { table: transformToTable(formParams), json: formParams },
            queryParams: { table: transformToTable(queryParams), json: queryParams },
        }
    }

    return null;
    
};

export { 
    transformRequestContract,
    transformResponseContractToJson,
    transformResponseContractToTable, 
};