FROM openresty/openresty:buster

COPY http.lua /usr/local/openresty/lualib/resty/
COPY http_headers.lua /usr/local/openresty/lualib/resty/
COPY nginx.conf /etc/openresty/nginx.conf
COPY request_logger.lua /usr/local/openresty/nginx/request_logger.lua
