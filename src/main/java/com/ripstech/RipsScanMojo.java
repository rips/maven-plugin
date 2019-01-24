package com.ripstech;

import com.ripstech.api.connector.Api;
import com.ripstech.api.connector.exception.ApiException;
import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.*;
import com.ripstech.api.utils.constant.Severity;
import com.ripstech.api.utils.issue.IssueHandler;
import com.ripstech.api.utils.scan.ScanHandler;
import com.ripstech.api.utils.scan.result.ScanResultParser;
import com.ripstech.api.utils.scan.result.ThresholdViolations;
import com.ripstech.api.utils.scan.result.Thresholds;
import com.ripstech.api.utils.validation.ApiVersion;

import com.ripstech.api.utils.validation.EndpointValidator;
import com.ripstech.api.utils.version.ScanVersionPattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Mojo(name = "scan",
        defaultPhase = LifecyclePhase.VERIFY,
        inheritByDefault = false)
public class RipsScanMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Parameter(property = "rips.apiUrl", required = true)
  private String apiUrl;

  @Parameter(property = "rips.uiUrl")
  private String uiUrl;

  @Parameter(property = "rips.email", required = true)
  private String email;

  @Parameter(property = "rips.password", required = true)
  private String password;

  @Parameter(property = "rips.applicationId", required = true)
  private int applicationId;

  @Parameter(property = "rips.profileId", defaultValue = "1")
  private long profileId;

  @Parameter(property = "rips.version")
  private String version;

  @Parameter(property = "rips.thresholds")
  private Map<String, Integer> thresholds;

  @Parameter(property = "rips.analysisDepth", defaultValue = "5")
  private int analysisDepth;

  @Parameter(property = "rips.scanTimeout", defaultValue = "5")
  private int scanTimeout;

  @Parameter(property = "rips.printIssues", defaultValue = "true")
  private boolean printIssues;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Log logger;
    // Ignore sub modules
    if(!project.isExecutionRoot()) {
      return;
    }

    // Get Logger
    logger = getLog();
    ResultLogger resultLogger = new ResultLogger(logger);
    Api api;

    try {
      api = getApi(apiUrl, email, password);

      ScanHandler scanHandler = new ScanHandler(api, applicationId, uiUrl);

      scanHandler.uploadFile(Paths.get("."));
      long scanId = scanHandler.setLogger(logger::info).startScan(new ScanVersionPattern("Maven")
                                          .replace(ScanVersionPattern.ISO_DATE_TIME),
                                          config -> config.setSource("ci-build-maven"))
                            .getId();

      IssueHandler issueHandler = scanHandler.getIssueHandler();
      List<Issue> issues = issueHandler.getAllIssues();

      Map<Severity, Integer> thresholdsSeverity = thresholds.entrySet().stream()
                                                          .collect(Collectors.toMap(entry -> Severity.valueOf(entry.getKey().toUpperCase()),
                                                                                    Map.Entry::getValue));

      ThresholdViolations thresholdViolations = ScanResultParser.getReachedThreshold(new Thresholds(thresholdsSeverity),
                                                                                     issueHandler.getScanResult());

      resultLogger.printNumberOfIssues(issues.size(),
                                       issueHandler.getScanResult().getAmoutOfNewIssues());

      if(printIssues) {
        Map<Long, String> issueFiles = ScanResultParser.getFilesFromIssues(api, applicationId, scanId);
        Map<Long, String> issueTypeNames = ApiUtils.getIssueTypeNames(api);

        resultLogger.printIssues(issueFiles, issueTypeNames, issues);
      }
      resultLogger.printThresholdStats(thresholdViolations);


    } catch (TimeoutException | FailedLoginException | IOException | ApiException e) {
      throw new MojoExecutionException(e.getMessage());
    }


  }

  private Api getApi(String url, String email, String password) throws MalformedURLException, ApiException, FailedLoginException {

    if(null == EndpointValidator.api(url)) {
      throw new MalformedURLException("Invalid api endpoint");
    }

    if(!EndpointValidator.url(url)) {
      throw new MalformedURLException("Invalid api apiUrl");
    }

    if(!EndpointValidator.apiLogin(apiUrl, email, password)) {
      throw new FailedLoginException("Login is not correct.");
    }

    Api api =  ApiUtils.getApiWithFallback(apiUrl, email, password);
    if(!EndpointValidator.compatibleWithApiVersion(api, ApiVersion.parse("3.0.0"))) {
      throw new ApiException("Api version is not supported by plugin");
    }

    return api;
  }
}

