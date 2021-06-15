##############
#build
#############
FROM maven:3.6.0-jdk-11-slim AS build
COPY ./ ./route-guider
#Add settings.xml file for github auth
#ARG TOKEN
#RUN mkdir ~/.m2 && \
#echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>${TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml

#RUN cd ./route-guider && \
#mvn package

##############
#Package
#############
FROM openjdk:11.0-jre AS PROD
#FROM cubeiocorp/jre:11 AS PROD

COPY --from=build route-guider/target/grpc-route-guider-1.0-SNAPSHOT.jar /root.jar

CMD ["java", "-jar", "/root.jar"]

EXPOSE 8981
