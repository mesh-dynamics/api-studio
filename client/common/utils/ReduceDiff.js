import generator from './generator/json-path-generator';

const BEGIN_BRACKET = "<BEGIN>",
    END_BRACKET = "<END>",
    REMOVED = "<REMOVED>",
    NA = "<NA>",
    ADDED = "<ADDED>",
    DONE = "<DONE>";

class ReduceDiff {
    constructor(prefix, actualJSON, expectedJSON, computedDiff) {
        this.actualJSON = actualJSON;
        this.expectedJSON = expectedJSON;
        this.actualJSONObj = JSON.parse(this.actualJSON);
        this.expectedJSONObj = JSON.parse(this.expectedJSON);
        this.computedDiff = computedDiff;

        /* Has to provided dynamically */
        this.prefix = prefix;

        /* This is to prepare the json in pretty format with 4 spaces indentation */
        this.stringifiedActJSON = JSON.stringify(this.actualJSONObj, undefined, 4);
        this.stringifiedExpJSON = JSON.stringify(this.expectedJSONObj, undefined, 4);
        
        /* Get pretty printed json into a array of lines, to compare */
        this.prettyPrintedActJSONLines = this.stringifiedActJSON.split(/(?:\r\n|\r|\n)/g).map((each) => each + "\n");
        this.prettyPrintedExpJSONLines = this.stringifiedExpJSON.split(/(?:\r\n|\r|\n)/g).map((each) => each + "\n");

        /* generate JSON Paths */
        this.actJSONPaths = generator(this.actualJSONObj, BEGIN_BRACKET, END_BRACKET, this.prefix);
        this.expJSONPaths = generator(this.expectedJSONObj, BEGIN_BRACKET, END_BRACKET, this.prefix);
    }

    _findPathInComputedDiff(jPath, computedDiff) {
        let tempPath = jPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "");
        for (let i in computedDiff) {
            if(computedDiff[i]["path"] === tempPath)
                return computedDiff[i];
        }
        return null;
    }

    _updateReducedDiffArray(diffReason, currentDiffReason, jsonStringLine, tempReducedDiffArray, reducedDiffArray) {
        /*
            This method updates the pre-final result to final result. The pre-final result (tempReducedDiffArray) has line by line diff changes and once a diff reason changes, empty this pre-final result to combine similar groups and add to the final result.
            This method is to combine similar diffed blocks (removed or added or no changes) to the final result. 
        */
        if(diffReason === currentDiffReason) {
            tempReducedDiffArray.push(jsonStringLine);
        } else {
            reducedDiffArray.push({
                value: tempReducedDiffArray.join(""),
                removed: diffReason === NA ? false : diffReason === REMOVED ? true : false,
                added: diffReason === NA ? false : diffReason === ADDED ? true : false,
                count: (tempReducedDiffArray.join("").match(/(?:\r\n|\r|\n)/g) || []).length
            });
            tempReducedDiffArray = [];
            diffReason = currentDiffReason;
            tempReducedDiffArray.push(jsonStringLine);
        }
        return [diffReason, tempReducedDiffArray];
    }

    _addDiffedJSONSubObjToReducedDiffArray(diffReason, currentDiffReason, tempReducedDiffArray, reducedDiffArray, iter, jsonPath, jsonPathArray, prettyPrintedJSONLines) {
        /*
            This method loops through the entire object which is either added or removed and add to the pre-final result and then to final result.
        */
        let jsonPathWOBegin = jsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, ""),
        jsonPathWOEND;
        while (jsonPathWOEND !== jsonPathWOBegin) {
            [diffReason, tempReducedDiffArray] = this._updateReducedDiffArray(diffReason, currentDiffReason, prettyPrintedJSONLines[iter], tempReducedDiffArray, reducedDiffArray);
            iter++;
            jsonPath = jsonPathArray[iter] ? jsonPathArray[iter][0] : "";
            jsonPathWOEND = jsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "");
        }
        [diffReason, tempReducedDiffArray] = this._updateReducedDiffArray(diffReason, currentDiffReason, prettyPrintedJSONLines[iter], tempReducedDiffArray, reducedDiffArray);
        return [diffReason, tempReducedDiffArray, iter];
    }

    computeDiffArray() {
        let tempString,
            expectedJSONPathArray = Array.from(this.expJSONPaths), actualJSONPathArray = Array.from(this.actJSONPaths);
        let leftStack = [], rightStack = [], tempStack = [], expIter = 0, actIter = 0,
            isJSObject = false, tempDiffArray = [], tempDiffReason = NA, reducedDiffArray = [], tempReducedDiffArray = [];
        while (expIter < expectedJSONPathArray.length || actIter < actualJSONPathArray.length) {
            let tempExpJsonPath = expectedJSONPathArray[expIter] ? expectedJSONPathArray[expIter][0] : "",
                tempActJsonPath = actualJSONPathArray[actIter] ? actualJSONPathArray[actIter][0] : "",
                removedPathObject, addedPathObject;
            /* 
                When two paths are same, check whether are js objects (array or object)
                if they are in fact objects then add them to respective stacks.
                and pop them out once the object is completely traversed
            */
            if(tempExpJsonPath && tempActJsonPath && tempExpJsonPath ===  tempActJsonPath) {
                if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                    leftStack.push(tempExpJsonPath);
                    rightStack.push(tempExpJsonPath);
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                } else if (tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                    leftStack.pop();
                    rightStack.pop();
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                } else {
                    /* 
                        When tow paths are same and they are not objects, then either their values too same or not.
                        If the values are not same, then add to the pre-final result and then to final result (reducedDiffArray) tagging them as removed and added respectively.
                        reducedDiffArray -> this is an array which resembles the diff presentation library expects
                    */
                    if(tempExpJsonPath) removedPathObject = this._findPathInComputedDiff(tempExpJsonPath, this.computedDiff);
                    if(tempActJsonPath) addedPathObject = this._findPathInComputedDiff(tempActJsonPath, this.computedDiff);
                    if(removedPathObject) {
                        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                    }
                    if(addedPathObject) {
                        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray);
                    }
                    if(!removedPathObject && !addedPathObject) {
                        /*
                            If paths and their values are same, but due to syntax the pretty printed lines may be different (eg: commas before a removed path or an added path), in that case showing them as different for now. but later should be shown differently other than reg or green shades.
                        */
                        if(this.prettyPrintedExpJSONLines[expIter] !== this.prettyPrintedActJSONLines[actIter]) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray);
                        } else {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                        }
                        
                    }
                }
                expIter++;
                actIter++;
            } else if(tempExpJsonPath && tempActJsonPath && tempExpJsonPath !==  tempActJsonPath) {
                /*
                    if the paths are not same, then check whether a jsonpath is added or removed.
                    Either the case, check a simple key and value is added or an object is added or removed
                    If its an object which is added or removed then add the object path to a temporary stack to match the brackets.
                    If its a simple key/value pair, then add to the pre-final result.
                */
                if(tempExpJsonPath) removedPathObject = this._findPathInComputedDiff(tempExpJsonPath, this.computedDiff);
                if(tempActJsonPath) addedPathObject = this._findPathInComputedDiff(tempActJsonPath, this.computedDiff);
                if(removedPathObject) {
                    if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        tempStack.push(tempExpJsonPath);
                    } else if (tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                        tempStack.pop();
                    }
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray);
                    expIter++;
                }
                if(addedPathObject) {
                    if(tempActJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        tempStack.push(tempActJsonPath);
                    } else if (tempActJsonPath.indexOf(END_BRACKET) > -1) {
                        tempStack.pop();
                    }
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray);
                    actIter++;
                }
                /*
                    If its not removed or added, assume its removed. TBD.
                */
               if(!removedPathObject && !addedPathObject) {
                    console.error("Circuit Breaker!");
                    return [];
               }
                /*
                    Iterate through the entire object and add to the final result. This part is done as a separate method -> _addDiffedJSONSubObjToReducedDiffArray
                */
                if(tempStack.length > 0) {
                    if(removedPathObject && tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        [tempDiffReason, tempReducedDiffArray, expIter] = this._addDiffedJSONSubObjToReducedDiffArray(tempDiffReason, REMOVED, tempReducedDiffArray, reducedDiffArray, expIter, tempExpJsonPath, expectedJSONPathArray, this.prettyPrintedExpJSONLines);
                        tempStack.pop();
                        expIter++;
                    } else if(addedPathObject && tempActJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        [tempDiffReason, tempReducedDiffArray, actIter] = this._addDiffedJSONSubObjToReducedDiffArray(tempDiffReason, ADDED, tempReducedDiffArray, reducedDiffArray, actIter, tempActJsonPath, actualJSONPathArray, this.prettyPrintedActJSONLines);
                        tempStack.pop();
                        actIter++;
                    }
                }
            } else {
                console.error("Circuit Breaker!");
                return [];
            }
        }
        /*
            Finally whatever left in the pre-final result, add to to final result itself.
        */
        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, DONE, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray);
        return reducedDiffArray;
    }

}

export default ReduceDiff;