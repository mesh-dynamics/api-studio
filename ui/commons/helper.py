#!/usr/bin/env python
from xml.etree import ElementTree as et
import os
import sys
def findVersion(filename, nextVersion):
    ns = "http://maven.apache.org/POM/4.0.0"
    et.register_namespace('', ns)
    tree = et.ElementTree()
    tree.parse(filename)
    version = tree.getroot().find("{%s}version" % ns)
    version.text = nextVersion
    tree.write(filename)


def main():
    try:
        nextVersion = sys.argv[1]
        print(nextVersion)
        dirname = os.path.dirname(__file__)
        filename = os.path.join(dirname, 'pom.xml')
        findVersion(filename, nextVersion)
    except IndexError as e:
        print("Verison not provided as system argument")
    except Exception as e:
        print(e)


if __name__ == '__main__':
    main()