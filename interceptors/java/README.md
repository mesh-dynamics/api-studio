# interceptor-framework
Interceptors for different REST frameworks


To package multiple interceptors in the same jar:  
1. Add the interceptor to be packed as a dependency in `pom_new.xml`  
2. Include interceptor in shade plugin's include tag in `pom_new.xml`  
3. Run `mvn install && mvn package -f pom_new.xml`

Running above command will generate jar with selected interceptors and `dependency-reduced-pom.xml`.
