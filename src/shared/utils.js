/**
 * @param {*} object Non empty object which contents key value pairs
 * @param {*} key Key name whose value needs to extracted
 */

const getParameterCaseInsensitive = (object, key) => {
    return object[
        Object.keys(object)
        .find(k => k.toLowerCase() === key.toLowerCase())
    ];
}

const isJsonOrGrpcMime = (contentType) => {
    return contentType && (contentType.toLowerCase().indexOf("json") || contentType.toLowerCase().indexOf("grpc"));
}

module.exports = { getParameterCaseInsensitive, isJsonOrGrpcMime };