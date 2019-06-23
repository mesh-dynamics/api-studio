#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'pyaml'])
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'jinja2'])

import yaml
import os
from jinja2 import Environment, FileSystemLoader

def main():
    base_dir = sys.argv[2]
    namespace = sys.argv[3]
    template_dir = base_dir + "/templates"
    if sys.argv[1] == "init":
        init_files(base_dir, template_dir, namespace)
    elif sys.argv[1] == "record":
        record_files(base_dir, template_dir, namespace)

def init_files(base_dir, template_dir, namespace):
    namespace_host = sys.argv[4]
    env = Environment(loader=FileSystemLoader(template_dir))
    for file in os.listdir(template_dir):
        if file.endswith(".yaml"):
            template = env.get_template(file)
            outfile = base_dir + "/kubernetes/" + file
            with open(outfile, "w") as out:
                output_from_template = template.render(namespace=namespace, namespace_host=namespace_host)
                out.write(output_from_template)
                out.close()

def record_files(base_dir, template_dir, namespace):
    cube_application = sys.argv[4]
    cube_customer = sys.argv[5]
    cube_instanceid = sys.argv[6]
    outfile = base_dir + "/kubernetes/envoy-record-cs.yaml"
    env = Environment(loader=FileSystemLoader(template_dir))
    template = env.get_template("envoy-record-cs.j2")
    service_file = base_dir + "/templates/services"
    data_stream = open(service_file, 'r')
    dataMap = yaml.safe_load(data_stream)
    with open(outfile, "w") as out:
        for service in dataMap['services']:
            output_from_template = template.render(service_name=service, customer=cube_customer, cube_application=cube_application, cube_instanceid=cube_instanceid, namespace=namespace)
            out.write("---\n")
            out.write(output_from_template + "\n" * 2)
    out.close()


if __name__ == "__main__":
    main()