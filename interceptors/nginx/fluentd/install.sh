#!/usr/bin/env bash

#
# Copyright 2021 MeshDynamics.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -x

if [ -z "$1" ]; then
	ENDPOINT=demo.dev.cubecorp.io
else
	ENDPOINT=$1
fi

#Check if fluentd is already installed
VERSION="$(/opt/td-agent/embedded/bin/fluentd --version | awk '{print $NF}')"
if [[ $VERSION == 1.7.* ]]; then
	echo "Complatible version of fluentd already installed, Skipping Fluentd installation.."
else
	#registers a new rpm repository and installs the td-agent rpm package
	curl -L https://toolbelt.treasuredata.com/sh/install-amazon2-td-agent3.sh | sh
fi

#rename the default config file
sudo mv /etc/td-agent/td-agent.conf /etc/td-agent/td-agent.org

#Create configuration for cube
sudo bash -c 'echo "
<source>
  @type tail
  @id in_tail_cube_nginx
  path /var/log/nginx/cube.http.access.log
  pos_file /home/ec2-user/cube.http.access.log.pos
  tag cube.nginx
  read_from_head true
  <parse>
    @type json
  </parse>
</source>
<filter cube.nginx>
  @type parser
  key_name hdrs
  hash_value_field hdrs
  reserve_data true
  #remove_key_name_field true
  <parse>
    @type json
  </parse>
</filter>
<match cube.nginx>
  @type http
  endpoint  http://$ENDPOINT/cs/rrbatch
  open_timeout 5
  <format>
    @type json
  </format>
  <buffer log>
    @type memory
    chunk_limit_records 1
  </buffer>
</match>" > /etc/td-agent/td-agent.conf'

#Start fluentd in background
sudo service td-agent start

