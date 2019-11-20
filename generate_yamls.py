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
    cube_application = sys.argv[4]
    cube_customer = sys.argv[5]
    template_dir = base_dir + "/templates"
    if sys.argv[1] == "init":
        init_files(base_dir, template_dir, namespace, cube_application, cube_customer)
    elif sys.argv[1] == "record" or sys.argv[1] == "replay":
        operation = sys.argv[1]
        recap_files(operation, base_dir, template_dir, namespace, cube_application, cube_customer)

def init_files(base_dir, template_dir, namespace, cube_application, cube_customer):
    host = sys.argv[6]
    namespace_host= sys.argv[7]
    cube_host = sys.argv[8]
    staging_host=sys.argv[9]
    cube_instance=sys.argv[10]
    springboot_profile=sys.argv[11]
    solr_core=sys.argv[12]
    cubeio_tag=sys.argv[13]
    cubeui_tag=sys.argv[14]
    cubeui_backend_tag=sys.argv[15]
    movieinfo_tag=sys.argv[16]
    env = Environment(loader=FileSystemLoader(template_dir))
    for file in os.listdir(template_dir):
        if file.endswith(".yaml") or file.endswith(".json"):
            template = env.get_template(file)
            outfile = base_dir + "/kubernetes/" + file
            with open(outfile, "w") as out:
                output_from_template = template.render(namespace=namespace, namespace_host=namespace_host, cube_application=cube_application, customer=cube_customer, record_host=host, cube_host=cube_host, staging_host=staging_host, cube_instance=cube_instance, springboot_profile=springboot_profile, solr_core=solr_core, cubeio_tag=cubeio_tag, cubeui_tag=cubeui_tag, cubeui_backend_tag=cubeui_backend_tag, movieinfo_tag=movieinfo_tag)
                out.write(output_from_template)
                out.close()

def recap_files(operation, base_dir, template_dir, namespace, cube_application, cube_customer):
    cube_instanceid = sys.argv[6]
    master_namespace = sys.argv[7]
    env = Environment(loader=FileSystemLoader(template_dir))
    if operation == "record":
        outfile = base_dir + "/kubernetes/envoy-record-cs.yaml"
        template = env.get_template("envoy-record-cs.j2")
    elif operation == "replay":
        outfile = base_dir + "/kubernetes/envoy-replay-cs.yaml"
        template = env.get_template("envoy-replay-cs.j2")
    env = Environment(loader=FileSystemLoader(template_dir))
    service_file = base_dir + "/templates/services"
    data_stream = open(service_file, 'r')
    dataMap = yaml.safe_load(data_stream)
    with open(outfile, "w") as out:
        for service in dataMap['services']:
            output_from_template = template.render(service_name=service, customer=cube_customer, cube_application=cube_application, cube_instanceid=cube_instanceid, master_namespace=master_namespace, namespace=namespace)
            out.write("---\n")
            out.write(output_from_template + "\n" * 2)
    out.close()
    if operation == "replay":
        app = os.path.split(base_dir)
        mock_file = base_dir + "/templates/mock-all-except-" + app[1] + ".j2"
        if os.path.isfile(mock_file):
            outfile = base_dir + "/kubernetes/mock-all-except-" + app[1] + ".yaml"
            template = env.get_template("mock-all-except-" + app[1] + ".j2")
            with open(outfile, "w") as out:
                output_from_template = template.render(namespace=namespace, customer=cube_customer, cube_application=cube_application, cube_instance=cube_instanceid, master_namespace=master_namespace)
                out.write(output_from_template)
                out.close()

if __name__ == "__main__":
    main()