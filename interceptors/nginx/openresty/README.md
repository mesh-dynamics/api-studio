# Introduction

OpenResty NGINX distribution with meshd LUA module enables capturing the API traffic flowing through the NGINX proxy to the application. The captured traffic can then be used to automate the regression testing, service virtualization etc.

# Modes of NGINX deployment

There are two modes in which NGINX might have been deployed

1. Native Mode
2. Container Mode

In Native mode, the NGINX and the application are deployed natively in the host system and in Container mode, the NGINX is deployed as container in the target envrinoment like Kubernetes etc.

# Native Mode

In Native mode, to enable the data capture, the following LUA code should be placed in the given location of the OpenResty NGINX installation structure.

For example, in case of EC2 instance with Amazon linux where OpenResy NGINX is installed as reverse proxy to the applicaiton, the files should be placed in locations as given below

    1. http.lua - /usr/local/openresty/lualib/resty/ &  usr/local/openresty/lualib/ngx/
    2. http_headers.lua - /usr/local/openresty/lualib/resty/ & usr/local/openresty/lualib/ngx/
    3. request_logger.lua - /usr/local/openresty/nginx/
    4. nginx.conf - /usr/local/openresty/nginx/conf/

Configure nginx.conf to forward the API Traffic. Refer the nginx.conf confiugration section for details

# Container Mode

To enable API traffic capture in an envrionment where NGINX is ruuning as container, follow the below steps

1. Build the OpenResty Docker image using the Dockerfile in the repo (interceptors/nginx/openresty/Dockerfile) 
2. Configure nginx.conf to forward the API Traffic. Refer the nginx.conf confiugration section for details
3. Reload/Restart nginx to enable the API traffic capture.
4. Using UI/API, Recording and running Test suite can be done now. Please refer the respective documentation


Refer the nginx.conf sample template configuration file and replace the place holder list below with appropriate values
    1. customer_name - Give appropriate meaningful name, it can refer the orginization name
    2. appname - Name of the application for which the API traffic to be captured
    3. servicename - Name of the specific service (service here refers a specific service in microservices application, it monolith application this can be like "APIGateway" or anything meaningful
    4. token - The API token to be used to make recording calls, refer https://github.com/cube-io-corp/meshd-complete/tree/main/utils/token-generator/APITokenGenerator for more details
    5. cubeStoreEventBatch - The API endpoint where the meshd server is running https://<meshd_server_endpoint>/api/cs/storeEventBatch
    6. instanceId - Any meaningful name that can help identify the specific test envrionment where the applicaiton with this nginx instance is running
