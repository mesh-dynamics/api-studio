local customerId = ngx.var.customerId
local app = ngx.var.app
local instanceId = ngx.var.instanceId
local service = ngx.var.service
local runType = ngx.var.runType
local recordingType = ngx.var.recordingType
local cubeStoreEventBatch = ngx.var.cubeStoreEventBatch

 

 

ngx.log(ngx.ERR, "REQUEST capturing started")
json = require("cjson")
local http = require("resty.http")
 

 

function getval(v, def)
  if v == nil then
     return def
  end
  return v
end

 

function myerrorhandler( err )
   ngx.log(ngx.ERR, "ERROR: " .. err )
end

 


local function starts_with(str, start)
   return str:sub(1, #start) == start
end

 

local function ends_with(str, ending)
   return ending == "" or str:sub(-#ending) == ending
end

 

local data = {request={}, response={}}

 

 


 

 

local cube_request = data["request"]
local resp = data["response"]
cube_request["customerId"] = customerId
cube_request["app"] = app
cube_request["instanceId"] = instanceId
cube_request["service"] = service
cube_request["runType"] = runType
cube_request["recordingType"] = recordingType

 
local req_headers, err = ngx.req.get_headers()

if err == "truncated" then
   -- one can choose to ignore or reject the current request here
end

local req_headers_multimap = {}

for k, v in pairs(req_headers) do
    if type(v) == "table" then
	req_headers_multimap[k] = v
    else
       req_headers_multimap[k] = {v}
    end
end

local traceId_univ = ""

-- ngx.log(ngx.CRIT, "headers : " .. req_headers["x-b3-traceid"]);
ngx.log(ngx.CRIT, "reqId : " .. ngx.var.request_id);


if req_headers["x-b3-traceid"] == nil then
    traceId_univ = ngx.var.request_id
    req_headers_multimap["x-b3-traceid"] = {traceId_univ}
    req_headers_multimap["x-b3-spanid"] = {traceId_univ}
else
    traceId_univ = req_headers["x-b3-traceid"]
end

 

ngx.log(ngx.CRIT, "traceId_univ : " .. traceId_univ);

 


-- local reqId_univ = ""
-- if req_headers["c-src-request-id"] == nil then
--     reqId_univ = ngx.var.request_id
-- else
--     reqId_univ = req_headers["c-src-request-id"]
-- end

 

-- ngx.log(ngx.CRIT, "reqId_univ : " .. reqId_univ);

 

local apiPathToSet = ngx.var.uri
if starts_with(apiPathToSet, "/") and apiPathToSet ~= "/" then
    apiPathToSet = apiPathToSet:sub(2, #apiPathToSet)
end

 


-- if ends_with(apiPathToSet, "/") and apiPathToSet ~= "/" then
    -- apiPathToSet = apiPathToSet:sub(1,#apiPathToSet-1)
-- end

local req_query_params, err = ngx.req.get_uri_args()

if err == "truncated" then
   -- one can choose to ignore or reject the current request here
end

local req_query_params_multimap = {}

for k, v in pairs(req_query_params) do
    if type(v) == "table" then
       req_query_params_multimap[k] = v
    else
       req_query_params_multimap[k] = {v}
    end
end 

local b='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/' -- You will need this for encoding/decoding
function enc(data)
    return ((data:gsub('.', function(x)
        local r,b='',x:byte()
        for i=8,1,-1 do r=r..(b%2^i-b%2^(i-1)>0 and '1' or '0') end
        return r;
    end)..'0000'):gsub('%d%d%d?%d?%d?%d?', function(x)
        if (#x < 6) then return '' end
        local c=0
        for i=1,6 do c=c+(x:sub(i,i)=='1' and 2^(6-i) or 0) end
        return b:sub(c+1,c+1)
    end)..({ '', '==', '=' })[#data%3+1])
end
 

local req_payload = {}
req_payload["hdrs"] = req_headers_multimap
req_payload["queryParams"] = req_query_params_multimap
req_payload["method"] = ngx.req.get_method()
req_payload["path"] = ngx.var.uri
ngx.req.read_body()
local text = ngx.var.request_body
if text == nil then
	ngx.log(ngx.CRIT, "request body is nil : ");
        req_payload["body"] = ""
else 
	ngx.log(ngx.CRIT, "request body is not nil : " .. text);
	req_payload["body"] = enc(ngx.var.request_body)
end

 
local req_payload_wrapper = {
}

req_payload_wrapper={"HTTPRequestPayload", req_payload}

cube_request["payload"] = req_payload_wrapper

 
cube_request["apiPath"] = apiPathToSet
cube_request["traceId"] = traceId_univ
cube_request["spanId"] = traceId_univ
cube_request["parentSpanId"] = ""
cube_request["reqId"] = ngx.var.request_id
cube_request["eventType"] = "HTTPRequest"
cube_request["timestamp"] = ngx.req.start_time()

 

content_type = getval(ngx.var.CONTENT_TYPE, "")

 

ngx.log(ngx.CRIT, "URI : " .. ngx.var.uri);

 


-- res = ngx.location.capture("/custom_cube_loc" .. ngx.var.uri);
-- xpcall( myfunction, myerrorhandler )
-- upstream_response = xpcall(ngx.location.capture("/custom_cube" .. ngx.var.uri), myerrorhandler)

 


local subReqMethod = ngx.req.get_method();
if (subReqMethod == "POST") then 
    subReqMethod = ngx.HTTP_POST
elseif (subReqMethod == "GET") then
    subReqMethod = ngx.HTTP_GET
elseif (subReqMethod == "DELETE") then
    subReqMethod = ngx.HTTP_DELETE
elseif (subReqMethod == "PUT") then
    subReqMethod = ngx.HTTP_PUT
elseif (subReqMethod == "PATCH") then
    subReqMethod = ngx.HTTP_PATCH
end

local uri=""

if (ngx.var.QUERY_STRING == nil) then
	uri = ngx.var.uri
else
	uri = ngx.var.uri .. "?" .. ngx.var.QUERY_STRING
end

local upstream_response = ngx.location.capture("/custom_cube" .. uri, {
    method = subReqMethod,
    always_forward_body = true
})

 

local cube_response = {}

 

cube_response["customerId"] = customerId
cube_response["app"] = app
cube_response["instanceId"] = instanceId
cube_response["service"] = service
cube_response["runType"] = runType
cube_response["recordingType"] = recordingType

 

 

local resp_headers, err = upstream_response.header

if err == "truncated" then
   -- one can choose to ignore or reject the current request here
end

local resp_headers_multimap = {}

for k, v in pairs(resp_headers) do
    if type(v) == "table" then
       resp_headers_multimap[string.lower(k)] = v
    else
       resp_headers_multimap[string.lower(k)] = {v}
    end
end

local resp_payload = {}

resp_payload["hdrs"] = resp_headers_multimap
resp_payload["status"] = upstream_response.status
resp_payload["body"] = enc(upstream_response.body)

local resp_payload_wrapper = {
}

resp_payload_wrapper={"HTTPResponsePayload", resp_payload}
--resp_payload_wrapper[1] = resp_payload


cube_response["payload"] = resp_payload_wrapper

cube_response["apiPath"] = apiPathToSet
cube_response["traceId"] = cube_request["traceId"]
cube_response["spanId"] = traceId_univ
cube_response["parentSpanId"] = ""
cube_response["reqId"] = cube_request["reqId"]
cube_response["eventType"] = "HTTPResponse"
cube_response["timestamp"] = ngx.now()


data["response"] = cube_response


local cube_event_body =  "{\"cubeEvent\": " .. json.encode(data["request"]) .. "}" .. "\n" .. "{\"cubeEvent\": " .. json.encode(data["response"]) .. "}"

 


local httpc = http.new()
     local cube_res, err = httpc:request_uri(cubeStoreEventBatch, {
       method = "POST",
       body = cube_event_body,
       headers = {
         ["Content-Type"] = "application/x-ndjson",
         ["Authorization"] = ngx.var.token
       },
       keepalive_timeout = 2500,
       keepalive_pool = 10,
       ssl_verify = false
})

ngx.log(ngx.CRIT, "response from cube");
ngx.log(ngx.CRIT, err); 

local forbiddenHeaders = {
"accept-aharset",
"accept-encoding",
"access-control-request-headers",
"access-control-request-method",
"connection",
"content-length",
"cookie",
"cookie2",
"date",
"dnt",
"expect",
"feature-policy",
"host",
"keep-alive",
"origin",
"proxy-",
"sec-",
"te",
"trailer",
"transfer-encoding",
"upgrade",
"via",
"referrer"
}

local function has_value (tab, val)
    for index, value in ipairs(tab) do
        -- We grab the first index of our sub-table instead
        if value == string.lower(val) then
            return true
        end
    end

    return false
end

if upstream_response~=nil then
    ngx.status = upstream_response.status
    local resp_headers = upstream_response.header
     for k, v in pairs(resp_headers) do
         if not has_value(forbiddenHeaders, k) then
              ngx.header[k] = v
         end
     end

 

    -- ngx.header.content_encoding = gzip
    ngx.log(ngx.CRIT, "printing response body");
    ngx.log(ngx.CRIT, upstream_response.body);
    ngx.say(upstream_response.body);
    local eofrsp = ngx.eof();
    ngx.flush(true);
    --ngx.log(ngx.CRIT, "printing eof response");
    --ngx.log(ngx.CRIT, eofrsp);
else 
    ngx.say("No response from upstream");
    ngx.eof();
    ngx.flush(true);
end

 

-- ngx.log(ngx.CRIT, "PRINTING REQUEST RESPONSE OF UPSTREAM");

 

-- ngx.log(ngx.CRIT, "PRINTING REQUEST RESPONSE OF UPSTREAM");
-- ngx.print(res_out.body);
-- ngx.log(ngx.CRIT, res.body);
ngx.log(ngx.CRIT, "PRINTING1 REQUEST RESPONSE OF UPSTREAM");
ngx.log(ngx.CRIT, cube_event_body);
