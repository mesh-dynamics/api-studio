#########################################
####Copy build to production image####
#########################################
# uses java version 11.0.2 as per https://github.com/docker-library/tomcat/blob/ec2d88f0a3b34292c1693e90bdf786e2545a157e/9.0/jre11/Dockerfile
FROM tomcat:9-jre11 AS prod
ARG JAR_FILE
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY server.xml /usr/local/tomcat/conf/server.xml
COPY ca.cer ca.cer
COPY sectigo.cer sectigo.cer
RUN echo yes | keytool -importcert -alias sectigo -keystore \
    /docker-java-home/lib/security/cacerts -storepass changeit -file sectigo.cer
RUN echo yes | keytool -importcert -alias startssl -keystore \
    /docker-java-home/lib/security/cacerts -storepass changeit -file ca.cer
COPY target/$JAR_FILE /usr/local/tomcat/webapps/ROOT.war
RUN mkdir -p /usr/local/tomcat/newrelic/logs
#Data Copy for embedded Solr
RUN mkdir -p /var/lib/meshd/data/solr
RUN mkdir -p /var/lib/meshd/datasrc/embedded_solr_config

#copy the local solr to container
#COPY src/main/resources/embedded_solr_config/ /var/lib/meshd/data/solr/
#Copy the solr resources to datasrc directory where if required it can be copied to data/solr by the process.
COPY src/main/resources/embedded_solr_config/ /var/lib/meshd/datasrc/embedded_solr_config/
RUN ls -la /var/lib/meshd/datasrc/embedded_solr_config/*

RUN useradd tomcat
RUN chown -R tomcat /usr/local/tomcat/newrelic/logs
ADD ./src/main/resources/newrelic.jar /usr/local/tomcat/newrelic/newrelic.jar
ADD ./src/main/resources/newrelic.yml /usr/local/tomcat/newrelic/newrelic.yml

#For protoc compiler
# Install protoc (cf. http://google.github.io/proto-lens/installing-protoc.html)
ENV PROTOC_ZIP=protoc-3.13.0-linux-x86_64.zip
RUN curl -OL https://github.com/protocolbuffers/protobuf/releases/download/v3.13.0/$PROTOC_ZIP \
    && unzip -o $PROTOC_ZIP -d /usr/local bin/protoc \
    && unzip -o $PROTOC_ZIP -d /usr/local 'include/*' \
    && rm -f $PROTOC_ZIP

# adding line below to speedup tomcat startup
# see https://github.com/theotherp/nzbhydra2/issues/42
# reduced time from 360 s to 6s!
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
EXPOSE 8082
#############
####Dev####
#############
FROM tomcat:9-jre11 AS dev
ARG JAR_FILE
RUN rm -rf /usr/local/tomcat/webapps/ROOT
RUN perl -0777 -i -pe 's/securerandom.source=file:\/dev\/random/securerandom.source=file:\/dev\/urandom/' /etc/java-11-openjdk/security/java.security
ADD target/$JAR_FILE /usr/local/tomcat/webapps/ROOT.war
COPY ca.cer ca.cer
RUN echo yes | keytool -importcert -alias startssl -keystore \
    /docker-java-home/lib/security/cacerts -storepass changeit -file ca.cer
COPY server.xml /usr/local/tomcat/conf/server.xml
RUN mkdir -p /usr/local/tomcat/newrelic/logs
RUN useradd tomcat
RUN chown -R tomcat /usr/local/tomcat/newrelic/logs
ADD ./src/main/resources/newrelic.jar /usr/local/tomcat/newrelic/newrelic.jar
ADD ./src/main/resources/newrelic.yml /usr/local/tomcat/newrelic/newrelic.yml

#For protoc compiler
# Install protoc (cf. http://google.github.io/proto-lens/installing-protoc.html)
ENV PROTOC_ZIP=protoc-3.13.0-linux-x86_64.zip
RUN curl -OL https://github.com/protocolbuffers/protobuf/releases/download/v3.13.0/$PROTOC_ZIP \
    && unzip -o $PROTOC_ZIP -d /usr/local bin/protoc \
    && unzip -o $PROTOC_ZIP -d /usr/local 'include/*' \
    && rm -f $PROTOC_ZIP

# TODO: SET run_mode as "local" when moved to final repo
#ENV run_mode=local
ENV data_dir=/var/lib/meshd/data/

