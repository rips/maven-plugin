package com.ripstech;

import com.ripstech.api.connector.Api;
import com.ripstech.api.connector.exception.ApiException;
import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.ApiUtils;
import com.ripstech.api.utils.constant.RipsDefault;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Mojo(name = "scan", defaultPhase = LifecyclePhase.VERIFY, inheritByDefault = false)
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

	@Parameter(property = "rips.profileId")
	private long profileId;

	@Parameter(property = "rips.version", defaultValue = RipsDefault.VERSION_PATTERN)
	private String version;

	@Parameter(property = "rips.thresholds")
	private Map<String, Integer> thresholds;

	@Parameter(property = "rips.analysisDepth", defaultValue = "5")
	private int analysisDepth;

	@Parameter(property = "rips.scanTimeout", defaultValue = "5")
	private int scanTimeout;

	@Parameter(property = "rips.printIssues", defaultValue = "true")
	private boolean printIssues;

	private static final String SCAN_SOURCE = "ci-build-maven";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Ignore sub modules
		if (!project.isExecutionRoot()) {
			return;
		}

		// Filter invalid thresholds and create new map with severities
		Map<Severity, Integer> severities =
				thresholds.entrySet().stream()
						.filter(entry ->
								        Arrays.stream(Severity.values())
										        .anyMatch(severity -> entry.getKey()
												                              .toUpperCase()
												                              .equals(severity.toString())))
						.collect(Collectors.toMap(entry -> Severity.valueOf(entry.getKey().toUpperCase()),
						                          Map.Entry::getValue));

		// Get Logger
		final Log logger = getLog();
		ResultLogger resultLogger = new ResultLogger(logger);
		Api api;

		try {
			api = getApi(apiUrl, email, password);

			ScanHandler scanHandler = new ScanHandler(api, applicationId, uiUrl);

			scanHandler.uploadFile(Paths.get("."), SCAN_SOURCE);
			long scanId = scanHandler.setLogger(logger::info)
					              .startScan(resolveScanVersion(version),
					                         config -> config.setSource(SCAN_SOURCE)
							                                   .setProfile(profileId)
							                                   .setAnalysisDepth(analysisDepth))
					              .getId();

			IssueHandler issueHandler = scanHandler.getIssueHandler();
			issueHandler.setTimeoutInMinutes(scanTimeout * 60);
			issueHandler.setPollIntervalInSeconds(5);
			List<Issue> issues = issueHandler.getAllIssues();

			ThresholdViolations thresholdViolations =
					ScanResultParser.getReachedThreshold(new Thresholds(severities), issueHandler.getScanResult());

			resultLogger.printNumberOfIssues(issues.size(), issueHandler.getScanResult().getAmoutOfNewIssues());

			if (printIssues) {
				Map<Long, String> issueFiles = ScanResultParser.getFilesFromIssues(api, applicationId, scanId);
				Map<Long, String> issueTypeNames = ApiUtils.getIssueTypeNames(api);

				resultLogger.printIssues(issueFiles, issueTypeNames, issues, uiUrl, applicationId, scanId);
			}
			resultLogger.printThresholdStats(thresholdViolations);
		} catch (TimeoutException | FailedLoginException | IOException | ApiException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}

	private Api getApi(String url, String email, String password)
			throws MalformedURLException, ApiException, FailedLoginException {

		if (null == EndpointValidator.api(url)) {
			throw new MalformedURLException("Invalid api endpoint");
		}

		if (!EndpointValidator.url(url)) {
			throw new MalformedURLException("Invalid api apiUrl");
		}

		if (!EndpointValidator.apiLogin(apiUrl, email, password)) {
			throw new FailedLoginException("Login is not correct.");
		}

		Api api = ApiUtils.getApiWithFallback(apiUrl, email, password);
		if (!EndpointValidator.compatibleWithApiVersion(api, ApiVersion.parse("3.0.0"))) {
			throw new ApiException("Api version is not supported by plugin");
		}

		return api;
	}

	private String resolveScanVersion(String scanVersion) {
		return new ScanVersionPattern("Maven",
		                              null,
		                              project.getName(),
		                              null,
		                              null,
		                              null)
				       .replace(scanVersion);
	}
}
