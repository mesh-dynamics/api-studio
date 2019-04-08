#!/usr/bin/env python3
import sys
import json
import requests

request_filename_prefix = "request_template"
json_file_suffix = ".json"
headers = {'Content-type': 'application/json'}

def main():
    version = sys.argv[1]
    gateway = sys.argv[2]
    customer = sys.argv[3]
    app = sys.argv[4]
    print (version + " " + gateway + " "  + customer + " " + app)
    request_filename = request_filename_prefix +  json_file_suffix
    #templates = json.load(request_filename)
    # defining the api-endpoint
    api_endpoint =  "http://" + gateway + "/as/registerTemplate/request/" + customer + "/" + app
    print (api_endpoint)
    template_json = read_json_file(request_filename)
    for entry in template_json:
        servicename = entry["service"]
        path = entry["path"]
        template = entry["template"]
        template_api_endpoint = api_endpoint + "/" + servicename + "/" + path
        print(template)
        r = requests.post(url=template_api_endpoint , json=template , headers=headers)
        print(r.text)
        #print(r.status)

def read_json_file(filename):
    with open(filename ,encoding='utf-8', errors='ignore') as json_data:
        template_as_dict = json.load(json_data , strict=False)
        return template_as_dict

if __name__ == "__main__":
    main()
