const statusCodeList = [
    {status: 200, value: "200 OK"},
    {status: 201, value: "201 Created"},
    {status: 202, value: "202 Accepted"},
    {status: 203, value: "203 Non-Authoritative Information"},
    {status: 204, value: "204 No Content"},
    {status: 205, value: "205 Reset Content"},
    {status: 206, value: "206 Partial Content"},
    {status: 400, value: "400 Bad Request"},
    {status: 401, value: "401 Unauthorized"},
    {status: 402, value: "402 Payment Required"},
    {status: 403, value: "403 Forbidden"},
    {status: 404, value: "404 Not Found"},
    {status: 405, value: "405 Method Not Allowed"},
    {status: 406, value: "406 Not Acceptable"},
    {status: 407, value: "407 Proxy Authentication Required"},
    {status: 408, value: "408 Request Timeout"},
    {status: 409, value: "409 Conflict"},
    {status: 410, value: "410 Gone"},
    {status: 411, value: "411 Length Required"},
    {status: 412, value: "412 Precondition Failed"},
    {status: 413, value: "413 Payload Too Large"},
    {status: 414, value: "414 URI Too Long"},
    {status: 415, value: "415 Unsupported Media Type"},
    {status: 416, value: "416 Requested Range Not Satisfiable"},
    {status: 417, value: "417 Expectation Failed"},
    {status: 500, value: "500 Internal Server Error"},
    {status: 501, value: "501 Not Implemented"},
    {status: 502, value: "502 Bad Gateway"},
    {status: 503, value: "503 Service Unavailable"},
    {status: 504, value: "504 Gateway Timeout"},
    {status: 505, value: "505 HTTP Version Not Supported"},
];

export const getHttpStatus = (code) => {
    for (let httpStatus of statusCodeList) {
        if (code == httpStatus.status) {
            return httpStatus.value;
        }
    }

    return code;
};

export default statusCodeList;
