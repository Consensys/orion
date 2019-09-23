# Oracle Maven Access
* Orion bundles Oracle JDBC driver which is downloaded from Oracle Maven repository by Gradle.
* You need to provide your Oracle account credentials to access Oracle Maven repository.
If you don't have Oracle credentials, you can create one at 
https://profile.oracle.com/myprofile/account/create-account.jspx
* Create (or update) gradle.properties in project root folder and add following
gradle properties (or create environment variables with same name and values):
```
oracleMavenUser=YourOracleAccountEmail
oracleMavenPassword=YourOracleAccountPassword
```
* 