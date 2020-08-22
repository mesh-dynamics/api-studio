#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'pyaml'])
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'jinja2'])

import yaml
import os
from fitchconfig import *
from jinja2 import Environment, FileSystemLoader

def main():
    template_dir = baseDir + "/templates"
    generate_filters(template_dir)

def generate_filters(template_dir):
    env = Environment(loader=FileSystemLoader(template_dir))
    outfile = baseDir + "/kubernetes/envoy-record-cs.yaml"
    template = env.get_template("envoy-record-cs.j2")
    env = Environment(loader=FileSystemLoader(template_dir))
    service_file = baseDir + "/templates/services"
    data_stream = open(service_file, 'r')
    dataMap = yaml.safe_load(data_stream)
    with open(outfile, "w") as out:
        for service in dataMap['services']:
            output_from_template = template.render(service_name=service,namespace=namespace, app=app, auth_token=auth_token, customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, run_type=run_type, recording_type=recording_type, cube_host=cube_host)
            out.write("---\n")
            out.write(output_from_template + "\n" * 2)
    out.close()

if __name__ == "__main__":
    main()