# CubeIO Backend

This application was generated using `Spring-Boot (v4.3.18)`.  
Minimum requirement: 

    Java version : 11
    PostgreSQL   : 10.8

## Development


The `./mvnw` or `./mvnw -Pdev` command will run the project in development mode.
    
#### Building war for dev

To create war package for deployment for development, run:

    ./mvnw -Pdev clean package


## Production

The `./mvnw -Pprod` command will run the project in production mode.

#### Building for production

To create war package for deployment for production, run:

    ./mvnw -Pprod clean package
    
War package will be created in `./target` folder with name `backend-0.0.1.war`.  
You can change the name by modifying profile section of `pom.xml` file:

    <profile>
        <id>prod</id>
        <properties>
            <spring.profiles.active>prod</spring.profiles.active>
        </properties>
        <build>
            <finalName>ROOT-PROD-${version}</finalName>
        </build>
    </profile>

## Information

-   Separate databases need to be created for `dev` and `prod` profiles. See `application-dev.yml` and `application-prod.yml` for details
-   Mail will be sent to user upon signup, activation and password-reset request. User can only access API's on activation.
-   Use postman to test the features with base path [http://localhost:8080](http://localhost:8080).

#### Modifications required in `*.yml` files

-   Change database username/passwords if required
-   Frontend `basepath` url and `activation`, `login`, `reset` url endpoints.
-   Mail credentials
-   Secret-key and expiry time for jwt-authentication