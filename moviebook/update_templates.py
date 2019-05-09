#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'requests'])

import json
import requests
import os

request_filename_prefix = "templates/template_request_"
response_filename_prefix = "templates/template_response_"
json_file_suffix = ".json"
if os.environ['CUBE_ENV'] == "default":
    headers = {'Content-type': 'application/json'}
elif os.environ['CUBE_ENV'] == "staging":
    headers =  {'Content-type': 'application/json', 'Host': 'staging.cubecorp.io'}
else:
    print ("Invalid Environment")


def main():
    version = sys.argv[1]
    gateway = sys.argv[2]
    customer = sys.argv[3]
    app = sys.argv[4]
    print (version + " " + gateway + " "  + customer + " " + app)
    request_filename = request_filename_prefix + version +  json_file_suffix
    response_filename = response_filename_prefix + version + json_file_suffix
    #templates = json.load(request_filename)
    # defining the api-endpoint
    request_api_endpoint =  "http://" + gateway + "/as/registerTemplate/request/" + customer + "/" + app
    response_api_endpoint = "http://" + gateway + "/as/registerTemplate/response/" + customer + "/" + app
    register_templates_from_file(request_filename , "request" , request_api_endpoint, customer, app)
    register_templates_from_file(response_filename , "response" , response_api_endpoint, customer, app)


def register_templates_from_file(filename , reqOrResponse, api_endpoint , customer , app):
    with open(filename ,encoding='utf-8', errors='ignore') as json_data:
        template_as_dict = json.load(json_data , strict=False)
        for entry in template_as_dict:
            servicename = entry["service"]
            path = entry["path"]
            try :
                template = entry["template"]
                template_api_endpoint = api_endpoint + "/" + servicename + "/" + path
                print("Registered " + reqOrResponse +" template json for " + customer + \
                 " :: " +  app + " :: " + " :: " + servicename + " :: " + path)
                print(template_api_endpoint)
                r = requests.post(url=template_api_endpoint , json=template , headers=headers)
                print("Got Reponse :: " + r.text)
            except Exception as e:
                print("Exception occured for " +  servicename + " :: " + path)
                print(e)


if __name__ == "__main__":
    main()
