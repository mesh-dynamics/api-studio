FROM maven:3.6.0-jdk-11-slim AS build
COPY department-service ./department-service
COPY employee-service ./employee-service
#Add settings.xml file for github auth
ARG TOKEN
RUN mkdir ~/.m2 && \
echo "<settings><servers><server><id>github</id><username>x-access-token</username><password>${TOKEN}</password></server></servers></settings>" > ~/.m2/settings.xml
RUN cd ./employee-service && \
mvn package && \
cd ../department-service && \
mvn package

################################################
####Copy Service app to production image####
################################################
FROM tomcat:9-jre11 AS serviceapp
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build employee-service/target/jersey_1_19_emp.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build employee-service/src/main/java/resources/samplerconfig.json /tmp/samplerconfig.json
# adding line below to speedup tomcat startup
# see https://github.com/theotherp/nzbhydra2/issues/42
# reduced time from 360 s to 6s!
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security

################################################
####Copy department app to production image####
################################################
# uses java version 11.0.2 as per https://github.com/docker-library/tomcat/blob/ec2d88f0a3b34292c1693e90bdf786e2545a157e/9.0/jre11/Dockerfile
FROM tomcat:9-jre11 AS deptapp
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build department-service/target/jersey_1_19_sampleapp_dept.war /usr/local/tomcat/webapps/ROOT.war
# adding line below to speedup tomcat startup
# see https://github.com/theotherp/nzbhydra2/issues/42
# reduced time from 360 s to 6s!
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
