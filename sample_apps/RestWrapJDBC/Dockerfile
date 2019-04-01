FROM tomcat:9-jre11
# FROM tomcat:9-jre8-alpine
RUN rm -rf /usr/local/tomcat/webapps/ROOT
# adding line below to speedup tomcat startup
# see https://github.com/theotherp/nzbhydra2/issues/42
# reduced time from 360 s to 6s!
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
ADD target/RestWrapJDBC-V1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
