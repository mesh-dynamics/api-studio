#!/#!/usr/bin/env python
from xml.etree import ElementTree as et
import os
def findVersion(filename):
    ns = "http://maven.apache.org/POM/4.0.0"
    et.register_namespace('', ns)
    tree = et.ElementTree()
    tree.parse(filename)
    p = tree.getroot().find("{%s}version" % ns)
    print p.text
    return p.text

def updateSampleApp(clonePath, interceptorVersion):
    filename = clonePath + "/course-service/pom.xml"
    ns = "http://maven.apache.org/POM/4.0.0"
    et.register_namespace('', ns)
    tree = et.ElementTree()
    tree.parse(filename)
    p = tree.getroot()
    for child in p.findall("{%s}dependencies" % ns):
        for dependency in child.findall("{%s}dependency" % ns):
            for artifactId in dependency.findall("{%s}artifactId" % ns):
                if artifactId.text == "md-apache-cxf-interceptor":
                    version = dependency.find("{%s}version" % ns)
                    version.text = interceptorVersion
    tree.write(filename)

def main():
    githubtoken = os.environ['TOKEN']
    dirname = os.path.dirname(__file__)
    filename = os.path.join(dirname, '../unified_pom_apache_cxf.xml')
    interceptorVersion = findVersion(filename)
    clonePath = os.path.join(dirname, 'cxf-app')
    cloneCommand = "git clone --branch=cxf-2.7-Deploy --config=http.sslVerify=false -v https://%s:x-oauth-basic@github.com/cube-io-corp/sampleapp-cxf-course.git --depth=1 " %githubtoken + clonePath
    os.system(cloneCommand)
    updateSampleApp(clonePath, interceptorVersion)
    gitcommit = "cd %s && git add course-service/pom.xml && git commit -m 'Update interceptor version'" %clonePath
    gitpush = "cd %s && git push https://%s:x-oauth-basic@github.com/cube-io-corp/sampleapp-cxf-course.git" %(clonePath, githubtoken)
    os.system(gitcommit)
    os.system(gitpush)

if __name__ == '__main__':
    main()
