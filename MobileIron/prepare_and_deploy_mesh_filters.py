#!/usr/bin/env python3

import sys
import yaml
import os
from config import *
from jinja2 import Environment, FileSystemLoader
from kubernetes import client, config

def main():

    deployment_info = getDeploymentsAndLabels(namespace)
    generate_filters(deployment_info, "templates")

def getDeploymentsAndLabels(namespace):
    # Configs can be set in Configuration class directly or using helper utility
    config.load_kube_config()
    v1 = client.AppsV1Api()
    deployment_info = {}

    try:
        ret = v1.list_namespaced_deployment(namespace=namespace, pretty="true")
        for i in ret.items:
            deployment = i.metadata.name
            deployment_info[deployment] = {}
            if i.metadata.labels:
                for label, value in i.metadata.labels.items():
                    deployment_info[deployment][label] = value
    except Exception as e:
        print(e)
    finally:
        return deployment_info

def generate_filters(deployment_info, template_dir):
    try:
        env = Environment(loader=FileSystemLoader(template_dir))
        template = env.get_template("envoyfilter.j2")
        env = Environment(loader=FileSystemLoader(template_dir))
        if service_file_provided == "yes":
            service_file = "templates/services"
            data_stream = open(service_file, 'r')
            dataMap = yaml.safe_load(data_stream)
            for service in dataMap['services']:
                for deployment, labels in deployment_info.items():
                    if service == deployment:
                        outfile = "output/" + deployment + ".yaml"
                        with open(outfile, "w") as out:
                            output_from_template = template.render(service_name=deployment, labels=labels, namespace=namespace, app=app, auth_token=auth_token, customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, run_type=run_type, recording_type=recording_type, cube_host=cube_host)
                            out.write(output_from_template)
                        out.close()
        else:
            for deployment, labels in deployment_info.items():
                outfile = "output/" + deployment + ".yaml"
                with open(outfile, "w") as out:
                    output_from_template = template.render(service_name=deployment, labels=labels, namespace=namespace, app=app, auth_token=auth_token, customer=customer, cube_application=cube_application, cube_instanceid=cube_instanceid, run_type=run_type, recording_type=recording_type, cube_host=cube_host)
                    out.write(output_from_template)
                out.close()

    except Exception as e:
        print(e)

if __name__ == "__main__":
    main()

