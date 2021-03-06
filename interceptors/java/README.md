Please refer the wiki page https://github.com/cube-io-corp/meshd-complete/wiki/Mesh-Dynamics-Listeners to know more about listerners and java interceptors

# Spring Boot

To use Sprint boot interceptor to capture API traffic from your applicaiton, follow the below steps

## Adding the dependency in the pom.xml
The following dependency to be added to the pom.xml of the target application.

<img width="766" alt="image" src="https://user-images.githubusercontent.com/13015877/121775812-60445500-cba7-11eb-8b2a-8cf79264750c.png">

## Adding the Ingress Interceptor
Add the io.cube as part of the scanPackages of the Spring application as below:

<img width="790" alt="image" src="https://user-images.githubusercontent.com/13015877/121775826-718d6180-cba7-11eb-8610-fa3f218894d4.png">


## Adding the Egress Interceptor
While making REST calls as a client, the place where the RestTemplate bean is registered, add the interceptors as below:
<img width="779" alt="image" src="https://user-images.githubusercontent.com/13015877/121775857-8833b880-cba7-11eb-8b27-da9b00b91843.png">

Please note that RestTemplateDataInterceptor() is to be registered only if the egress data capture is required. Otherwise this can be skipped.

# JAXRS Interceptor

## Adding the dependency in the pom.xml
The following dependency to be added to the pom.xml of the target application.

<img width="595" alt="image" src="https://user-images.githubusercontent.com/13015877/121777851-bddd9f00-cbb1-11eb-85b6-845ce5c97783.png">

## Adding the Ingress/Egress Interceptor 

Add the ingress/egress interceptor classesto the Jersey provider packagesin the web.xmlfor it to scan.

<img width="677" alt="image" src="https://user-images.githubusercontent.com/13015877/121777909-04cb9480-cbb2-11eb-8a2e-d86c983e7cd4.png">

## Registering the Egress Interceptor

While making REST calls as a client, the place where the Jerseyclient is built using the ClientBuilder, register the egress interceptors as below:

<img width="650" alt="image" src="https://user-images.githubusercontent.com/13015877/121777933-2298f980-cbb2-11eb-9c77-eb87cf84ea75.png">

ClientLoggingFilter.classis to be registered only if the egress data capture is required, otherwise it can be skipped
