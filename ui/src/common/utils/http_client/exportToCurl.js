import { stringify, parse } from 'query-string';
import {detect} from 'detect-browser';

const escapeCharacter = (x) => {
    var code = x.charCodeAt(0);
    if (code < 256) {
        // Add leading zero when needed to not care about the next character.
        return code < 16 ? "\\x0" + code.toString(16) : "\\x" + code.toString(16);
    }
    code = code.toString(16);
    return "\\u" + ("0000" + code).substr(code.length, 4);
}

const escapeString = (str) => {
    const browser = detect();
    if(browser.os.startsWith('win')) {
        return "\"" + str.replace(/"/g, "\"\"")
                    .replace(/%/g, "\"%\"")
                    .replace(/\\/g, "\\\\")
                    .replace(/[\r\n]+/g, "\"^$&\"") + "\"";
    } else {
        if (/[^\x20-\x7E]|\'/.test(str)) {
            // Use ANSI-C quoting syntax.
            return "$\'" + str.replace(/\\/g, "\\\\")
                              .replace(/\'/g, "\\\'")
                              .replace(/\n/g, "\\n")
                              .replace(/\r/g, "\\r")
                              .replace(/[^\x20-\x7E]/g, escapeCharacter) + "'";
        } else {
            // Use single quote syntax.
            return "'" + str + "'";
        }
    }
};

const exportToCurl = (requestMethod, requestUrl, requestUrlQueryParams, requestHeaders, requestBody, reqIsAllowCertiValidation) => {

    let command = ['curl'],
    ignoredHeaders = ['host', 'method', 'path', 'scheme', 'version'],
    data = [],
    contentType = requestHeaders['content-type'],
    queryStringValue = requestUrlQueryParams ? stringify(requestUrlQueryParams) : "",
    requestUrlWithQueryParams = requestUrl + (queryStringValue.length ? "?" + queryStringValue : "");

    
    command.push(escapeString(requestUrlWithQueryParams).replace(/[[{}\]]/g, "\\$&"));

    command.push('\\\n ');

    if (requestBody && requestBody !== '') {
        ignoredHeaders.push('content-length');

        if (contentType && contentType.startsWith('application/x-www-form-urlencoded')) {
            data.push('--data');
        } else {
            data.push('--data-binary');
        }

        data.push(escapeString(requestBody));
    }

    if (requestMethod && "post" !== requestMethod.toLowerCase() && "get" !== requestMethod.toLowerCase()) {
        command.push('-X');
        command.push(requestMethod);
        command.push('\\\n ');
    }

    if(requestHeaders) {
        Object.entries(requestHeaders).forEach(([key, [name, value]]) => {
            if(ignoredHeaders.indexOf(name) < -0) {
                command.push('-H');
                command.push(escapeString(name.replace(/^:/, '') + ': ' + value));
                command.push('\\\n ');
            }
        });
    }
    
    if(data.length > 0) {
        command = command.concat(data);
        command.push('\\\n ');
    }
    
    command.push('--compressed');
    command.push('\\\n ');

    if (reqIsAllowCertiValidation) {
        command.push('--insecure');
    }

    return command.join(' ');
}

export { 
    exportToCurl
}