FROM tomcat:9-jre11 AS prod
ARG JAR_FILE
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
COPY /target/$JAR_FILE /usr/local/tomcat/webapps/ROOT.war
## copy the modified server.xml with http2 support in the tomcat
USER root
COPY server.xml /usr/local/tomcat/conf/
