# RIPS Maven Plugin

Apache Maven is a popular build management tool for Java applications. RIPS security analysis can be easily integrated as a build task in order to fail your build whenever new security vulnerabilities are discovered.

## Configuration

You can add and configure the plugin in your pom.xml:
```XML
<build>
	<plugins>
		<plugin>
			<groupId>com.ripstech.maven</groupId>
			<artifactId>rips-maven-plugin</artifactId>
			<version>1.0.0</version>
			<configuration>
				<apiUrl>https://api-3.ripstech.com</apiUrl>
				<uiUrl>https://saas.ripstech.com</uiUrl>
				<email>test@company</email>
				<password>yourPassword</password>
				<applicationId>yourApplicationId</applicationId>
				<scanVersion>{isoDateTime}</scanVersion>
				<thresholds>
					<low>10</low>
					<medium>5</medium>
					<high>0</high>
					<critical>0</critical>
				</thresholds>
				<printIssues>true</printIssues>
			</configuration>
			<executions>
                <execution>
                    <goals>
                        <goal>scan</goal>
                    </goals>
                </execution>
            </executions>
		</plugin>
	</plugins>
</build>
```

## Local Setup
Add the plugin to your local repository:

```shell
mvn install:install-file -Dfile=<path-to-jar> -DgroupId=com.ripstech \
-DartifactId=rips-maven-plugin -Dversion=1.0.0 -Dpackaging=jar
```

## Details

- rips.apiUrl(required): Your RIPS Api Url.
- rips.uiUrl(optional): Your RIPS Ui Url.
- rips.email(required): Your RIPS Api login email.
- rips.password(required): Your RIPS Api password.
- rips.applicationId(required): The id of the RIPS application to use.
- rips.profileId(optional): The RIPS analysis profile.
- rips.scanVersion(optional): The version name of the scan. 
- rips.thresholds(optional): Map of tolerated numbers of issues by severity. Possible severities: critical, high, medium, low. (E.g. critical: 0, high: 0, medium: 5, low: 10) 
- rips.analysisDepth(optional): Overwrite default analysis depth(5).
- rips.scanTimeout(optional): Overwrite default scan timeout (5) in hours.
- rips.printIssues(optional): Set to false to suppress detailed output of all issues.

## Setting the build phase
The plugins' default build phase is 'verify' which means it scans during integration tests.
You can change this using the ```<executions>``` tag. For instance if you want to set it to 'deploy':

```XML
<executions>
    <execution>
        <phase>deploy</phase>
        <goals>
            <goal>scan</goal>
        </goals>
    </execution>
</executions>
```