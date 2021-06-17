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

function isEmpty(obj) {
	if (Array.isArray(obj)) {
		return obj.length === 0;
	}
	return !obj || Object.keys(obj).length === 0;
}

function generator(JSONObject, beginBracket, endBracket, prefix) {
	let pathMap = new Map();
	let lineCount = 0;
	function traverse(value, path) {
		if (value && typeof value === "object") {
			++lineCount;
			let temp;
			if (!isEmpty(value)) pathMap.set(path + beginBracket, value);
			else pathMap.set(path + beginBracket + endBracket, value);
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
			if (!isEmpty(value)) pathMap.set(path + endBracket, value);
		} else {

			++lineCount;
			pathMap.set(path, value);
		}
	}
	traverse(JSONObject, prefix);
	return pathMap;
}


export default generator;