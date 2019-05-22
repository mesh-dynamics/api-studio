################
####build####
###############
FROM maven:3.6.0-jdk-11-slim AS build
COPY pom.xml ./pom.xml
# download maven dependencies
RUN mvn verify clean --fail-never
COPY src ./src
RUN mvn -U package
#########################################
####Copy build to production image####
#########################################
# uses java version 11.0.2 as per https://github.com/docker-library/tomcat/blob/ec2d88f0a3b34292c1693e90bdf786e2545a157e/9.0/jre11/Dockerfile
FROM tomcat:9-jre11 AS prod
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=build target/cubews-V1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
# adding line below to speedup tomcat startup
# see https://github.com/theotherp/nzbhydra2/issues/42
# reduced time from 360 s to 6s!
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
#############
####Dev####
#############
FROM tomcat:9-jre11 AS dev
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
ADD target/cubews-V1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
