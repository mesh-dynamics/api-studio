##############
#build
#############
FROM maven:3.6.0-jdk-11-slim AS build
COPY ./ ./route-guide
#Add settings.xml file for github auth
#ARG TOKEN
#RUN mkdir ~/.m2 && \
#echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>${TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml

RUN cd ./route-guide && \
mvn package

##############
#Package
#############
FROM openjdk:11.0-jre AS PROD
#FROM cubeiocorp/jre:11 AS PROD


COPY --from=build route-guide/target/grpc-route-guide-1.0-SNAPSHOT.jar /root.jar
COPY --from=build route-guide/src/main/resources/io/grpc/examples/routeguide/dd-java-agent-0.68.0.jar /dd-java.jar

CMD ["java", "-javaagent:/dd-java.jar", "-jar", "/root.jar"]

EXPOSE 8980
