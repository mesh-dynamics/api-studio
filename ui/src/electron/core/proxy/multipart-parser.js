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

/**
 * multipart/form-data consists of multiple parts which are bounded by 
 * a boundary. Each boundary is dynamically defined in the boundary key
 * in the content string.
 * 
 * The content defined in each boundary can be of different type.
 * In theory it can contain key value pair or string or a plain string or
 * binary file. For each of these content types the content-type is defined
 * at the begining of the boundary along with key.
 * 
 * If the content-type is not defined for a part then it can be assumed that
 * it is a simple key value pair.
 * 
 * This parser currently handles simple key value pairs as well as parts 
 * containing plain text files.
 * 
 * Images and other binaries are not handled yet.
 * 
 * For a multipart/form-data containing the following data 
 * key1 = value1 (simple key value pair)
 * key2 = value2 (simple key value pair)
 * 
 * The output will be 
 * 
 * key1=value1&key2=value2
 * 
 * For a multipart/form-data containing the following data 
 * key1 = value1 (simple key value pair)
 * key2 = file2.txt (simple key value pair)
 * key3 = value3 (simple key value pair)
 * 
 * key1=value1&key2=value2#content=content&key3=value3
 */
const logger = require('electron-log');

const EMPTY_STRING = '';

const processPartWithContentType = (bodyPart) => {
    const lineParts = bodyPart.split('\r\n');
    // index 0 is EMPTY_STRING
    // index 1 is the line with Content-Disposition
    // index 2 is the line with Content-Type if it exists
    const contentDispositionSection = lineParts[1];
    const contentTypeSection = lineParts[2];
    const contentParts = lineParts.slice(3, lineParts.length - 1);

    if(contentTypeSection && contentTypeSection.toLowerCase().includes('content-type: text')) {
        const [emptyString, fieldSection, fileNameSection] = contentDispositionSection.split(';');
        const content = contentParts.join('\r\n');

        const fieldName = fieldSection.split('=')[1].replace(/"/g,"")
        const fileName = fileNameSection.split('=')[1].replace(/"/g,"")
        
        const multipartString = `${fieldName}=${fileName}#content=${String(content)}`
        
        return multipartString;
    }

    return EMPTY_STRING;
};

/**
 * 
 * @param {*} part Each multipart section bounded with a boundary
 * @return {[string]} Returns a list of Key Value Pairs
 */
const parseMultipartBoundary = (bodyPart) => {
    if(bodyPart.toLowerCase().includes('content-disposition: form-data') 
        && bodyPart.toLowerCase().includes('content-type')) {
            return processPartWithContentType(bodyPart);
    }

    if(bodyPart.toLowerCase().includes('content-disposition: form-data') 
        && (!bodyPart.toLowerCase().includes('content-type'))) {
            // clean up the white spaces, special characters and 
            // convert to format expected in the extractor
            return bodyPart
                    .replace('\r\nContent-Disposition: form-data; name=\"', '')
                    .replace('\"\r\n\r\n', '=')
                    .replace('\r\n--', '');        
    }

    return EMPTY_STRING;
};

/**
 * 
 * @param {*} buffer raw request body string from request event
 * @param {*} contentType content type present in the header of request event
 */
const parseMultipart = (buffer, contentType) => {
    // Get Boundary
    const boundary = contentType.split('boundary=')[1];
    
    // Split the content by boundary
    const splitBody = buffer.split(boundary);
    
    // Clean up the body parts
    const cleanedAndJoinedParts = splitBody.map(parseMultipartBoundary)
                                                .filter(Boolean)
                                                .join('&');

    return cleanedAndJoinedParts;
};

module.exports = { parseMultipartBoundary, parseMultipart }
