Introduction

OpenResty NGINX distribution with meshd LUA module enables capturing the API traffic flowing through the NGINX proxy to the application. The captured traffic can then be used to automate the regression testing, service virtualization etc.

Modes of NGINX deployment

There are two modes in which NGINX might have been deployed

1. Native Mode
2. Container Mode

In Native mode, the NGINX and the application are deployed natively in the host system and in Container mode, the NGINX is deployed as container in the target envrinoment like Kubernetes etc.

Native Mode

In Native, to enable the data capture, the following LUA code should be placed the given location of the OpenResty NGINX installation structure. 
