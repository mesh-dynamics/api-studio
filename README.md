# nginx-record-module
Nginx Module to capture http traffic to a log file (headers and body for both request and response). This module has been derived from [Capture Response Body Module] (http ://github.com/ZigzagAK/ngx_http_response_body_module)

## Compile the module in a static fashion

- Download the nginx source code, create a temp directory containing *config* and *ngx_http_response_body_module.c* file
- Configure
```
./configure --add-module=./module_dir
```
- Make and install
```
make install
```
- Before starting nginx make sure to change the *nginx.conf* in the installation directory appropriately


## Compile the module dynamically

- Download the nginx source code (make sure this source code has the same major-minor version as the target nginx binary which will eventually load the module), create a tmp directory containing *config* and *ngx_http_response_body_module.c* file
- Configure
```
sudo ./configure --add-dynamic-module=./module_dir
```
- Build only the modules
```
make modules
```
- This will create a *.so* file in the *objs* folder. Copy the *.so* file to the *modules* folder in the installation directory
```
sudo mkdir -p /usr/local/nginx/modules && sudo cp objs/ngx_http_response_body_module.so /usr/local/nginx/modules
```
- Add the following line in the *nginx.conf* file to load the module dynamically (Ideally this will be the first line in the config file).
Also add the module specific directives in the config file (as specified in *sample_nginx_conf*)
```
load_module modules/ngx_http_response_body_module.so;
```
- Restart your nginx server
