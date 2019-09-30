#!/usr/bin/env python3
import subprocess
import sys
import time
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'requests'])

import json
import requests
import os

host_header = sys.argv[6]
app_dir = sys.argv[7]
templateset_filename_prefix = app_dir + "/config/matcher/template_"
json_file_suffix = ".json"
headers = {'Content-type': 'application/json', 'Host': host_header}


def main():
    template_scenario = sys.argv[1]
    gateway = sys.argv[2]
    customer = sys.argv[3]
    app = sys.argv[4]
    template_version_temp_file = sys.argv[5]

    print(template_scenario + " " + gateway + " " + customer + " " + app)
    templateset_filename = templateset_filename_prefix + template_scenario + json_file_suffix
    # defining the api-endpoint
    save_template_endpoint = "http://" + gateway + "/as/saveTemplateSet/" + customer + "/" + app
    template_version = register_templates_from_file(templateset_filename, save_template_endpoint, customer, app)
    if template_version == None:
        print("Cannot write template version to file")
    else:
        with open(template_version_temp_file, "w") as f:
            f.write(template_version)
        print("Written template version: " + template_version + " to file " + template_version_temp_file)


def register_templates_from_file(templateset_filename, api_endpoint, customer, app):
    try:
        with open(templateset_filename, encoding='utf-8', errors='ignore') as json_data:
            template_as_dict = json.load(json_data, strict=False)
            # Override previous invalid timestamp
            template_as_dict["timestamp"] = time.time()
            r = requests.post(url=api_endpoint, json=template_as_dict, headers=headers)
            print("Registered template json for " + customer + \
                  " :: " + app)
            print(api_endpoint)
            print("Got Response :: " + r.text)
            response_json = r.json()
            template_version = response_json["templateSetVersion"]
            return template_version

    except json.JSONDecodeError as e:
        print("Cannot serialize templateSet: " + templateset_filename + " JSON to object ")
        print(e)
    except Exception as e:
        print("Exception occurred for Customer " + customer + " App :: " + app)
        print(e)


if __name__ == "__main__":
    main()
