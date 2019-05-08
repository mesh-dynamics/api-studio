#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'pyaml'])
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'jinja2'])

import yaml
import os
from jinja2 import Environment, FileSystemLoader

env = Environment(loader=FileSystemLoader('moviebook/templates'))
customer = sys.argv[1]
namespace = sys.argv[2]
cube_application = os.environ['CUBE_APPLICATION']
cube_instanceid = os.environ['CUBE_INSTANCEID']
data_stream = open('moviebook/services.yaml', 'r')
dataMap = yaml.safe_load(data_stream)

template_record = env.get_template('moviebook_record.j2')
template_replay = env.get_template('moviebook_replay.j2')
with open("moviebook/moviebook-envoy-cs.yaml", "w") as record:
    with open("moviebook/moviebook-envoy-replay-cs.yaml", "w") as replay:
        for service in dataMap['services']:
            output_from_record_template = template_record.render(service_name=service,customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, namespace=namespace)
            output_from_replay_template = template_replay.render(service_name=service,customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, namespace=namespace)
            record.write("---\n")
            replay.write("---\n")
            record.write(output_from_record_template + "\n" * 2)
            replay.write(output_from_replay_template + "\n" * 2)
        replay.close()
    record.close()





