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

module.exports = { getParameterCaseInsensitive };