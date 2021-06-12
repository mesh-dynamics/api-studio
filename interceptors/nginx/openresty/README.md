# Introduction

OpenResty NGINX distribution with meshd LUA module enables capturing the API traffic flowing through the NGINX proxy to the application. The captured traffic can then be used to automate the regression testing, service virtualization etc.

# Modes of NGINX deployment

There are two modes in which NGINX might have been deployed

1. Native Mode
2. Container Mode

In Native mode, the NGINX and the application are deployed natively in the host system and in Container mode, the NGINX is deployed as container in the target envrinoment like Kubernetes etc.

# Native Mode

In Native mode, to enable the data capture, the following LUA code should be placed in the given location of the OpenResty NGINX installation structure.

For example, in case of EC2 instance with Amazon linux where OpenResy NGINX is installed as a reverse proxy to the applicaiton, the files should be placed in locations as given below

    1. http.lua - /usr/local/openresty/lualib/resty/ &  usr/local/openresty/lualib/ngx/
    2. http_headers.lua - /usr/local/openresty/lualib/resty/ & usr/local/openresty/lualib/ngx/
    3. request_logger.lua - /usr/local/openresty/nginx/
    4. nginx.conf - /usr/local/openresty/nginx/conf/

Once the files are place, then nginx.conf should be configured appropriately to forward the API Traffic. Refer the nginx.conf confiugration section at the end for details

Once all the above are done, reload/restart nginx server to enable traffic capture and then use UI/API to start recording and running test suite. Please refer the respective documentation on how to start recording and how to run test suite.

# Container Mode

To enable API traffic capture in an envrionment where NGINX is ruuning as container, follow the below steps

1. Build the OpenResty Docker image using the Dockerfile in the repo (interceptors/nginx/openresty/Dockerfile) 
2. Configure nginx.conf to forward the API Traffic. Refer the nginx.conf confiugration section at the end for details
3. Reload/Restart nginx to enable the API traffic capture.
4. Using UI/API, Recording and running Test suite can be done now. Please refer the respective documentation on how to start recording and how to run test suite.

# Configuring nginx.conf for traffic capture

Refer the nginx.conf sample template configuration file and replace the place holder list below with appropriate values

1. customer_name - Give appropriate meaningful name, it can refer the orginization name
2. appname - Name of the application for which the API traffic to be captured
3. servicename - Name of the specific service (service here refers a specific service in microservices application, it monolith application this can be like "APIGateway" or anything meaningful
4. token - The API token to be used to make recording calls, refer https://github.com/cube-io-corp/meshd-complete/tree/main/utils/token-generator/APITokenGenerator for more details
5. cubeStoreEventBatch - The API endpoint where the meshd server is running https://<meshd_server_endpoint>/api/cs/storeEventBatch
6. instanceId - Any meaningful name that can help identify the specific test envrionment where the applicaiton with this nginx instance is running


To capture the API traffic, the traffic should be routed through the LUA module. If all the API traffic to be captured is exposed under a specific context path prefix by the application then the routing can be configured only for the prefix path. The following example configuration shows how to configure nginx.conf with sample context path prefix '/api'

Here all the API calls with /api prefix will be routed through the request_logger.lua LUA module and the corresponding API traffic will be captured and while calls are routed to the target applicaiton which in this case is running on http://localhost:8080


    	location /api/ {
               content_by_lua_file request_logger.lua;
    	}

    	location /custom_cube/api/ {
      		rewrite ^/custom_cube(.*)$ $1 break;
            proxy_pass http://localhost:8080;
      		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      		proxy_set_header Host $host;
               proxy_set_header X-Forwarded-Port 443;
      		proxy_redirect off;
      		if (!-f $request_filename) {
          		proxy_pass http://localhost:8080;
          		break;
      		}

    	}
        

If the application does not have any specific prefix to filter API traffic then capture all the traffic including API and static contents like html, js, image, fonts etc. and then use the filter option in the UI to filter out the static contents and keep only the API traffic to create the regresssion test suite

Here is the eample config that captures everyting 

        location / {
               content_by_lua_file request_logger.lua;
        }

        location /custom_cube/ {
               rewrite ^/custom_cube(.*)$ $1 break;
               proxy_pass http://localhost:8080;
               proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
               proxy_set_header Host $host;
               proxy_set_header X-Forwarded-Port 443;
               proxy_redirect off;

               if (!-f $request_filename) {
                        proxy_pass http://localhost:8080;
                        break;
                }
        }
        
Here is the option to be selected during the recording start in the UI where the static contents can be filtered

<img width="609" alt="image" src="https://user-images.githubusercontent.com/13015877/121770899-c15d3000-cb89-11eb-9d9f-c4b7004e4dc8.png">

