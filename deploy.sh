#!/usr/bin/env bash
RED="\033[0;31m"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo_success(){
    printf "${GREEN}${1} ${NC}\n"
}

echo_warn(){
    printf "${YELLOW}${1} ${NC}\n"
}

echo_danger(){
	printf "${NC}${1} : ${RED}${2} ${NC}\n"
}

echo_star(){
	printf "***************************************************************************\n"
}
echo "\n"
echo_star
echo_success "                    Creating production build"
echo_star
echo "\n"
./mvnw -Pprod clean package -DskipTests

echo "\n"
echo_star
echo_danger "                     Shutting down tomcat server"
echo_star
echo "\n"
sh ./apache-tomcat-8.5.31/bin/shutdown.sh

echo "\n"
echo_star
echo_warn "                        Replacing ROOT.war"
echo_star
echo "\n"
sudo mv ./target/backend-0.0.1.war.original apache-tomcat-8.5.31/webapps/ROOT.war


echo "\n"
echo_star
echo_success "                     Restarting Tomcat Server"
echo_star
echo "\n"
sudo sh ./apache-tomcat-8.5.31/bin/startup.sh