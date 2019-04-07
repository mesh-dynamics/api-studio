#!/usr/bin/env python3
import sys
import json
import requests

request_filename_prefix = "template_request_"
json_file_suffix = ".json"
headers = {'Content-type': 'application/json'}

def main():
    version = sys.argv[1]
    gateway = sys.argv[2]
    customer = sys.argv[3]
    app = sys.argv[4]
    request_filename = request_filename_prefix + version + json_file_suffix
    #templates = json.load(request_filename)
    # defining the api-endpoint
    api_endpoint =  "http://" + gateway + "/as/registerTemplateApp/request/" + customer + "/" + app
    template_as_dict = json.loads(request_filename)
    #data = open(request_filename, "r")
    template_as_string = json.dumps(template_as_dict)
    r = requests.post(url=api_endpoint , json=template_as_string , headers=headers)
    print(r.text)
    print(request_filename)
    print(api_endpoint)

if __name__ == "__main__":
    main()
