#!/usr/bin/env python

import yaml
import os
from jinja2 import Environment, FileSystemLoader

env = Environment(loader=FileSystemLoader('moviebook/templates'))
customer = os.environ['USERNAME']
application = os.environ['APPLICATION']
instanceid = os.environ['INSTANCEID']
data_stream = open('moviebook/service.yaml', 'r')
dataMap = yaml.safe_load(data_stream)

template_record = env.get_template('moviebook_record.j2')
template_replay = env.get_template('moviebook_replay.j2')
with open("moviebook/moviebook-envoy-cs.yaml", "wb") as record:
    with open("moviebook/moviebook-envoy-replay-cs.yaml", "wb") as replay:
        for service in dataMap['services']:
            output_from_record_template = template_record.render(service_name=service,customer=customer, application=application, instanceid=instanceid)
            output_from_replay_template = template_replay.render(service_name=service,customer=customer, application=application, instanceid=instanceid)
            record.write("---\n")
            replay.write("---\n")
            record.write(output_from_record_template + "\n" * 2)
            replay.write(output_from_replay_template + "\n" * 2)
        replay.close()
    record.close()





