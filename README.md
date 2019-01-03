# Rips Maven Plugin


## Configuration
- rips.apiUrl(required): Your RIPS Api Url.
- rips.uiUrl(optional): Your RIPS Ui Url.
- rips.email(required): Your RIPS Api login email.
- rips.password(required): Your RIPS Api password.
- rips.applicationId(required): The id of the RIPS application to use.
- rips.profileId(optional): The RIPS analysis profile.
- rips.version(optional): The version name of the scan. 
- rips.thresholds(optional): Map of tolerated numbers of issues by severity. Possible severities: critical, high, medium, low. (E.g. critical: 0, high: 0, medium: 5, low: 10) 
- rips.analysisDepth(optional): Overwrite default analysis depth(5).
- rips.scanTimeout(optional): Overwrite default scan timeout (5) in hours.
- rips.printIssues(optional): Set to false to suppress detailed output of all issues.