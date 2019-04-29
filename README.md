# cubeui-backend

#### Grails JVM version bug fixes

* ###### For terminal 
Type the command:  
```source "$HOME/.sdkman/bin/sdkman-init.sh"```
* ###### For IntelliJ project
Normally IntelliJ overrides JVM version from module SDK over project SDK but not in case of grails application.  
One way to overcome this situation is set Project SDK to grails required JVM version and change all other modules' to existing SDK of the project.
