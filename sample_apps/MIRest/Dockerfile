FROM tomcat:9-jre8-alpine
RUN rm -rf /usr/local/tomcat/webapps/ROOT
ADD target/MIRest-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
#EXPOSE 8080
# And run tomcat
#CMD ["/usr/local/tomcat/bin/catalina.sh", "run"]