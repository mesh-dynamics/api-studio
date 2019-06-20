#!/usr/bin/env python3
import subprocess
import sys
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'pyaml'])
subprocess.call([sys.executable, '-m', 'pip', 'install','--quiet' , 'jinja2'])

import yaml
import os
from jinja2 import Environment, FileSystemLoader

base_dir = sys.argv[1]
namespace = sys.argv[2]
namespace_host = sys.argv[3]
template_dir = base_dir + "/templates"
env = Environment(loader=FileSystemLoader(template_dir))
for file in os.listdir(template_dir):
    if file.endswith(".yaml"):
        template = env.get_template(file)
        outfile = base_dir + "/kubernetes/" + file
        with open(outfile, "w") as out:
            output_from_template = template.render(namespace=namespace, namespace_host=namespace_host)
            out.write(output_from_template)
            out.close()