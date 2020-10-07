#!/usr/bin/env python3
import subprocess
import sys
import yaml
import os
from config import *
from jinja2 import Environment, FileSystemLoader

def main():
    generate_filters("templates")

def generate_filters(template_dir):
    try:
        env = Environment(loader=FileSystemLoader(template_dir))
        template = env.get_template("envoyfilter.j2")
        env = Environment(loader=FileSystemLoader(template_dir))
        service_file = "templates/services"
        data_stream = open(service_file, 'r')
        dataMap = yaml.safe_load(data_stream)
        for service in dataMap['services']:
            outfile = service + ".yaml"
            with open(outfile, "w") as out:
                output_from_template = template.render(service_name=service, auth_token=auth_token, customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, run_type=run_type, recording_type=recording_type)
                out.write(output_from_template)
            out.close()
    except Exception as e:
        print(e)

if __name__ == "__main__":
    main()