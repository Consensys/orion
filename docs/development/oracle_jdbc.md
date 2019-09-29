# Oracle Maven Access
* Orion bundles Oracle JDBC driver which is downloaded from Oracle Maven repository by Gradle.
* You need to provide your Oracle account credentials to access Oracle Maven repository.
If you don't have Oracle credentials, you can create one at 
https://profile.oracle.com/myprofile/account/create-account.jspx
* Create (or update) `gradle.properties` in project root folder and add following
gradle properties 
```
oracleMavenUser=YourOracleAccountEmail
oracleMavenPassword=YourOracleAccountPassword
```
* Instead of modifying `gradle.properties`, following environment variables can also be used
```
ORG_GRADLE_PROJECT_oracleMavenUser
ORG_GRADLE_PROJECT_oracleMavenPassword	
```