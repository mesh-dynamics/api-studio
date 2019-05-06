#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'pyaml'])


import yaml
import os

data_stream = open('moviebook/moviebook.yaml', 'r')
dataMap = yaml.load_all(data_stream, Loader=yaml.SafeLoader)
with open("moviebook/services.yaml", "w") as f:
    f.write("services:\n")
    for config in dataMap:
        if config['kind'] == 'Service':
            service = config['metadata']['name']
            f.write("  - " + service + "\n")
    f.close()
