##############
#build
#############
FROM maven:3.6.0-jdk-11-slim AS build
COPY course-service ./course-service
#Add settings.xml file for github auth
ARG TOKEN
RUN mkdir ~/.m2 && \
echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>${TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml
RUN cd ./course-service && \
mvn package

##############
#Package
#############
FROM cubeiocorp/jre:11 AS PROD

COPY --from=build course-service/target/cxf-jaxrs-implementation-0.0.1-SNAPSHOT.jar /root.jar
COPY --from=build course-service/src/main/jib/tmp/samplerconfig.json /tmp/samplerconfig.json

EXPOSE 8084