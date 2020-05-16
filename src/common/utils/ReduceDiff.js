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

    _updateReducedDiffArray(diffReason, currentDiffReason, jsonStringLine, tempReducedDiffArray, reducedDiffArray, serverSideDiff, currentJsonPath, isLast=false) {
        /*
            This method updates the pre-final result to final result. The pre-final result (tempReducedDiffArray) has line by line diff changes and once a diff reason changes, empty this pre-final result to combine similar groups and add to the final result.
            This method is to combine similar diffed blocks (removed or added or no changes) to the final result. 
        */
        // FIX: refactor this method. Current logic doesnt need if and else but just the push
        if(diffReason === currentDiffReason) {
            tempReducedDiffArray.push(jsonStringLine);
        } else {
            tempReducedDiffArray = [];
            diffReason = currentDiffReason;
            tempReducedDiffArray.push(jsonStringLine);
        }
    // author: siddhant.mutha@meshdynamics.io
    // using isLastRemoved to handle the alignment of two objects                
        reducedDiffArray.push({
            value: jsonStringLine,
            removed: currentDiffReason === NA ? false : currentDiffReason === REMOVED ? true : false,
            added: currentDiffReason === NA ? false : currentDiffReason === ADDED ? true : false,
            count: 1,
            serverSideDiff: serverSideDiff,
            jsonPath: currentJsonPath,
            hasChildren: currentJsonPath && currentJsonPath.indexOf(BEGIN_BRACKET) > -1,
            isLastRemoved : currentDiffReason === REMOVED && isLast,
        });
        return [diffReason, tempReducedDiffArray];
    }

    _addDiffedJSONSubObjToReducedDiffArray(diffReason, currentDiffReason, tempReducedDiffArray, reducedDiffArray, iter, jsonPath, jsonPathArray, prettyPrintedJSONLines, tempJsonPathWithBegin) {
        /*
            This method loops through the entire object which is either added or removed and add to the pre-final result and then to final result.
        */
        let jsonPathWOBegin = tempJsonPathWithBegin.replace(BEGIN_BRACKET, "").replace(END_BRACKET, ""),
        jsonPathWOEND;
        while (jsonPathWOEND !== jsonPathWOBegin) {
            [diffReason, tempReducedDiffArray] = this._updateReducedDiffArray(diffReason, currentDiffReason, prettyPrintedJSONLines[iter], tempReducedDiffArray, reducedDiffArray, null, jsonPath);
            iter++;
            jsonPath = jsonPathArray[iter] ? jsonPathArray[iter][0] : "";
            jsonPathWOEND = jsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "");
        }
        // author: siddhant.mutha@meshdynamics.io
        // This is a fix to align two arrays/objects (one removed and other added right after) which were overlapping and showing an incorrect diff (see CUBE-1413 & CUBE-1414).   
        // Since this is the last line in the object, we pass the isLast flag as true, which sets the isLastRemoved flag if it's part of a removed object, and is used later while rendering the lines.
        [diffReason, tempReducedDiffArray] = this._updateReducedDiffArray(diffReason, currentDiffReason, prettyPrintedJSONLines[iter], tempReducedDiffArray, reducedDiffArray, null, jsonPath, true);
        
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
           // FIX: remove unused stacks.
            if(tempExpJsonPath && tempActJsonPath && tempExpJsonPath ===  tempActJsonPath) {
                if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                    leftStack.push(tempExpJsonPath);
                    rightStack.push(tempExpJsonPath);
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                } else if (tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                    leftStack.pop();
                    rightStack.pop();
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                } else {
                    /* 
                        When tow paths are same and they are not objects, then either their values too same or not.
                        If the values are not same, then add to the pre-final result and then to final result (reducedDiffArray) tagging them as removed and added respectively.
                        reducedDiffArray -> this is an array which resembles the diff presentation library expects
                    */
                    if(tempExpJsonPath) removedPathObject = this._findPathInComputedDiff(tempExpJsonPath, this.computedDiff);
                    if(tempActJsonPath) addedPathObject = this._findPathInComputedDiff(tempActJsonPath, this.computedDiff);
                    if(removedPathObject) {
                        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, removedPathObject, tempExpJsonPath);
                    }
                    if(addedPathObject) {
                        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, addedPathObject, tempActJsonPath);
                    }
                    if(!removedPathObject && !addedPathObject) {
                        /*
                            If paths and their values are same, but due to syntax the pretty printed lines may be different (eg: commas before a removed path or an added path), in that case showing them as different for now. but later should be shown differently other than reg or green shades.
                        */
                        if(this.prettyPrintedExpJSONLines[expIter] !== this.prettyPrintedActJSONLines[actIter]) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, null, tempActJsonPath);
                        } else {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, NA, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                        }
                        
                    }
                }
                expIter++;
                actIter++;
            } else if(tempExpJsonPath !==  tempActJsonPath) {
                /*
                    if the paths are not same, then check whether a jsonpath is added or removed.
                    Either the case, check a simple key and value is added or an object is added or removed
                    If its an object which is added or removed then add the object path to a temporary stack to match the brackets.
                    If its a simple key/value pair, then add to the pre-final result.
                */
                if(tempExpJsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "") === tempActJsonPath.replace(BEGIN_BRACKET, "").replace(END_BRACKET, "")) {
                    if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1 || tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                        if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, null, tempActJsonPath);
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                            expIter++;
                            tempExpJsonPath = expectedJSONPathArray[expIter] ? expectedJSONPathArray[expIter][0] : "";
                        }
                        if(tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                            expIter++;
                            actIter++;
                            continue;
                        }
                    }
                    if(tempActJsonPath.indexOf(BEGIN_BRACKET) > -1 || tempActJsonPath.indexOf(END_BRACKET) > -1) {
                        if(tempActJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, null, tempExpJsonPath);
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, null, tempActJsonPath);
                            actIter++;
                            tempActJsonPath = actualJSONPathArray[actIter] ? actualJSONPathArray[actIter][0] : "";
                        }
                        if(tempActJsonPath.indexOf(END_BRACKET) > -1) {
                            [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, null, tempActJsonPath);
                            actIter++;
                            expIter++;
                            continue;
                        }
                    }
                }
                if(tempExpJsonPath) removedPathObject = this._findPathInComputedDiff(tempExpJsonPath, this.computedDiff);
                if(!removedPathObject && tempActJsonPath) addedPathObject = this._findPathInComputedDiff(tempActJsonPath, this.computedDiff);
                if(removedPathObject) {
                    if(tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        tempStack.push(tempExpJsonPath);
                    } else if (tempExpJsonPath.indexOf(END_BRACKET) > -1) {
                        tempStack.pop();
                    }
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, REMOVED, this.prettyPrintedExpJSONLines[expIter], tempReducedDiffArray, reducedDiffArray, removedPathObject, tempExpJsonPath);
                    expIter++;
                }
                if(addedPathObject) {
                    if(tempActJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        tempStack.push(tempActJsonPath);
                    } else if (tempActJsonPath.indexOf(END_BRACKET) > -1) {
                        tempStack.pop();
                    }
                    [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, ADDED, this.prettyPrintedActJSONLines[actIter], tempReducedDiffArray, reducedDiffArray, addedPathObject, tempActJsonPath);
                    actIter++;
                }
                /*
                    Its not removed or added, its TBD.
                */
               if(!removedPathObject && !addedPathObject) {
                    console.error("Circuit Breaker!");
                    return null;
               }
                /*
                    Iterate through the entire object and add to the final result. This part is done as a separate method -> _addDiffedJSONSubObjToReducedDiffArray
                */
                if(tempStack.length > 0) {
                    if(removedPathObject && tempExpJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        let tempExpJsonPathWithBegin = tempExpJsonPath;
                        tempExpJsonPath = expectedJSONPathArray[expIter] ? expectedJSONPathArray[expIter][0] : "";
                        [tempDiffReason, tempReducedDiffArray, expIter] = this._addDiffedJSONSubObjToReducedDiffArray(tempDiffReason, REMOVED, tempReducedDiffArray, reducedDiffArray, expIter, tempExpJsonPath, expectedJSONPathArray, this.prettyPrintedExpJSONLines, tempExpJsonPathWithBegin);
                        tempStack.pop();
                        expIter++;
                    } else if(addedPathObject && tempActJsonPath.indexOf(BEGIN_BRACKET) > -1) {
                        let tempActJsonPathWithBegin = tempActJsonPath;
                        tempActJsonPath = actualJSONPathArray[actIter] ? actualJSONPathArray[actIter][0] : "";
                        [tempDiffReason, tempReducedDiffArray, actIter] = this._addDiffedJSONSubObjToReducedDiffArray(tempDiffReason, ADDED, tempReducedDiffArray, reducedDiffArray, actIter, tempActJsonPath, actualJSONPathArray, this.prettyPrintedActJSONLines, tempActJsonPathWithBegin);
                        tempStack.pop();
                        actIter++;
                    }
                }
            // FIX: this else is concoted. refactor this as well
            } else {
                /*
                    A circuit breaker.
                */
                console.error("Circuit Breaker!");
                return null;
            }
        }
        /*
            Finally whatever left in the pre-final result, add to to final result itself.
        */
        [tempDiffReason, tempReducedDiffArray] = this._updateReducedDiffArray(tempDiffReason, DONE, "", tempReducedDiffArray, reducedDiffArray, null);
        return reducedDiffArray;
    }

}

export default ReduceDiff;