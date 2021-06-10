###################
#build#
#################
FROM cubeiocorp/cubeiobase:0.0.2 AS build
ARG TOKEN
COPY . /
#Add settings.xml file for github auth
RUN echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>${TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml
RUN mvn package -DskipTests

##################
# Prod #
#################
FROM openjdk:11.0-jre AS prod
#ARG JAR_FILE=target/log-collector-0.0.1.jar
#COPY ${JAR_FILE} app.jar
COPY --from=build /target/log-collector-0.0.1.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
