package com.ripstech;

import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.scan.result.ThresholdViolations;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

class ResultLogger {
	private Log logger;

	ResultLogger(Log logger) {
		this.logger = logger;
	}

	void printNumberOfIssues(int totalIssues, int newIssues) {
		logger.warn(String.format("%d issues have been found. %d are new issues.", totalIssues, newIssues));
	}

	void printIssues(Map<Long, String> issueFiles, Map<Long, String> issueTypeNames, List<Issue> issues,
	                 @Nullable String uiUrl, long appId, long scanId) {

		for (Issue issue : issues) {
			if (null != issue.getSink()) {
				Path issueFile = Paths.get(issueFiles.get(issue.getSink().getFile().getId())).toAbsolutePath();
				Integer startLine = issue.getSink().getStartLine();
				Integer startColumn = issue.getSink().getStartColumn();

				logger.warn(""); // TODO: Why is this empty? Is it for a new line?
				logger.warn(String.format("%s:", issueTypeNames.get(issue.getType().getId())));

				logger.warn(String.format("%s:[%d,%d]", issueFile, startLine, startColumn));
				if (null != uiUrl) {
					logger.warn(String.format("More information: %s/issue/%d/%d/%d/%d/details",
					                          uiUrl, appId, scanId, issue.getType().getId(), issue.getId()));
				}
			}
		}

		logger.info("");
	}

	void printThresholdStats(ThresholdViolations thresholdViolations) throws MojoFailureException {
		if (thresholdViolations.isFailed()) {
			logger.info("The following thresholds have been exceeded:");
			thresholdViolations
					.getEntries()
					.forEach((key, value) -> logger.warn(String.format(
							"Severity: %s, Number of Issues: %d", key, value.getIssues())));


			throw new MojoFailureException("Thresholds have been exceeded.");
		}
	}
}
