###################
#build#
#################
FROM maven:3.6.0-jdk-11-slim AS build
COPY . /
RUN mkdir ~/.m2
RUN echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>5cec32b5cc9a3f779ec122f6b47c6973f619d992</password></server></servers></settings>" > ~/.m2/settings.xml
RUN mvn clean install -DskipTests \
&& ./mvnw -Pprod clean package -DskipTests
##################
# Prod #
#################
FROM tomcat:9-jre11 AS prod
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
COPY --from=build /target/backend-0.0.1.war /usr/local/tomcat/webapps/ROOT.war

