FROM debian:stable-slim

RUN apt-get update -y && \
apt-get install curl python3 python3-pip jq -y && \
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && \
chmod +x ./kubectl && \
mv ./kubectl /usr/local/bin/kubectl && \
ln -s /usr/bin/pip3 /usr/bin/pip && \
mkdir ~/.kube && \
mkdir code

COPY . /code/

RUN mv /code/certs ~/.kube/ && \
mv /code/config ~/.kube/

WORKDIR /code
