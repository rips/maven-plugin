package com.ripstech;

import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.*;
import com.ripstech.api.utils.file.FileExtensions;
import com.ripstech.api.utils.validation.ApiEndpointValidator;
import com.ripstech.api.utils.validation.ApiVersion;
import com.ripstech.apiconnector2.Api;
import com.ripstech.apiconnector2.exception.ApiException;

import com.ripstech.apiconnector2.service.application.ScanService;
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
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Mojo(name = "rips",
        defaultPhase = LifecyclePhase.VERIFY,
        inheritByDefault = false)
public class RipsScanMojo extends AbstractMojo {

  private Log logger;

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

  @Parameter(property = "rips.profileId", required = true)
  private long profileId;

  @Parameter(property = "rips.version", required = true)
  private String version;

  @Parameter(property = "rips.thresholds")
  private Map<String, Integer> thresholds = new HashMap<>();

  @Parameter(property = "rips.analysisDepth", defaultValue = "5")
  private int analysisDepth;

  @Parameter(property = "rips.scanTimeout", defaultValue = "5")
  private int scanTimeout;

  @Parameter(property = "rips.printIssues", defaultValue = "true")
  private boolean printIssues;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    // Initialize thresholds not set by user.
    initThresholds();

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

      RipsFileFilter ripsFileFilter = new RipsFileFilter(
              FileExtensions.getFileExtensionsByAppId(api, applicationId));
      Archiver archiver = new Archiver(ripsFileFilter);
      zipSources(archiver);

      long uploadId = ApiUtils.uploadFile(api, archiver.getArchive(), applicationId);
      logger.debug("Upload ID: " + uploadId);

      // Start scan
      long scanId = ApiUtils.startScan(api, applicationId, profileId, uploadId, version);
      logger.debug("Scan ID: " + scanId);

      archiver.removeZipFile();

      // Wait for results
      ScanService scanService = api.application(applicationId).scans();
      List<Issue> issues = ApiUtils.getScanIssues(scanService, api.application(applicationId).scan(scanId).issues(), scanId, logger::info)
              .get(5, TimeUnit.HOURS);

      // Evaluate results
      Map<String, Integer> totalIssues = ScanResultParser.getTotalIssues(api, applicationId, scanId);
      Map<String, Integer> newIssues = ScanResultParser.getNewIssues(api, applicationId, scanId);

      resultLogger.printNumberOfIssues(totalIssues, newIssues);

      if(printIssues) {
        Map<Long, String> issueFiles = ScanResultParser.getFilesFromIssues(api, applicationId, scanId);
        Map<Long, String> issueTypeNames = ApiUtils.getIssueTypeNames(api);

        resultLogger.printIssues(issueFiles, issueTypeNames, issues);
      }

      Map<Severity, Integer> reachedThresholds = ScanResultParser
                                                         .getReachedThreshold(api, applicationId,
                                                                              scanId, new Thresholds(thresholds));

      if(ApiEndpointValidator.validateUrl(uiUrl)) {
        resultLogger.printThresholdStats(reachedThresholds, uiUrl, applicationId, scanId);
      }
      else {
        resultLogger.printThresholdStats(reachedThresholds);
      }

    } catch (ApiException | ExecutionException | InterruptedException | TimeoutException | FailedLoginException | IOException e) {
      throw new MojoExecutionException(e.getMessage());
    }


  }

  private Api getApi(String url, String email, String password) throws MalformedURLException, ApiException, FailedLoginException {

    if(null == ApiEndpointValidator.validateApiEndpoint(url)) {
      throw new MalformedURLException("Invalid api endpoint");
    }

    if(!ApiEndpointValidator.validateUrl(url)) {
      throw new MalformedURLException("Invalid api apiUrl");
    }

    if(!ApiEndpointValidator.validateApiLogin(apiUrl, email, password)) {
      throw new FailedLoginException("Login is not correct.");
    }

    Api api =  new Api.Builder(new URL(url).toString())
            .withXPassword(email, password)
            .build();
    if(!ApiEndpointValidator.compatibleWithApiVersion(api, ApiVersion.version("3.0.0"))) {
      throw new ApiException("Api version is not supported by plugin");
    }

    return api;
  }

  private void zipSources(Archiver archiver) throws MojoExecutionException, ApiException {
    logger.debug("Collecting source Files.");
    try {
      archiver.createZip(Paths.get("."));
    } catch (IOException e) {
      throw new MojoExecutionException("Error while trying to zip source directory", e);
    }
    logger.debug("Created zip");

  }

  private void initThresholds() {
    thresholds.putIfAbsent("critical", 0);
    thresholds.putIfAbsent("high", 0);
    thresholds.putIfAbsent("medium", 0);
    thresholds.putIfAbsent("low", 0);
  }
}

