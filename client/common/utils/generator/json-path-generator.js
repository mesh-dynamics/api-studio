function generator (JSONObject, beginBracket, endBracket, prefix) {
    let pathMap = new Map();
    let lineCount = 0;
	function traverse(value, path) {
		if (value && typeof value === "object") {
            ++lineCount;
            let temp;
            pathMap.set(path + beginBracket, value);
	        if (Array.isArray(value)) {
	            value.forEach((element, i) => {
	                temp = path + "/" + i;
	                if (element && typeof element === "object") {
	                }
	                traverse(element, temp);
                });
	        } else {
	            Object.keys(value).forEach((name) => {
	                temp = path + "/" + name;
	                if (value[name] && typeof value[name] === "object") {
	                }
	                traverse(value[name], temp);
                });
            }
            ++lineCount;
            pathMap.set(path + endBracket, value);
	    } else {
            
            ++lineCount;
            pathMap.set(path, value);
	    }
	}
    traverse(JSONObject, prefix);
    console.log(lineCount);
	return pathMap;
}


export default generator;