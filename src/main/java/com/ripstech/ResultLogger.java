package com.ripstech;

import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.scan.result.ThresholdViolations;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

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
		logger.info( String.format("%d issues have been found. %d are new issues.", totalIssues, newIssues));
	}

	void printIssues(Map<Long, String> issueFiles, Map<Long, String> issueTypeNames, List<Issue> issues) {

		for(Issue issue : issues) {
			Path issueFile = null;
			Integer startLine = null;
			if(null != issue.getSource()) {
				issueFile = Paths.get(issueFiles.get(issue.getSource().getFile().getId())).toAbsolutePath();
				startLine = issue.getSource().getStartLine();
			} else if (null != issue.getSink()) {
				issueFile = Paths.get(issueFiles.get(issue.getSink().getFile().getId())).toAbsolutePath();
				startLine = issue.getSink().getStartLine();

			}
			if(null != issue.getSink()) {
				logger.info("");
				logger.info(String.format("%s:", issueTypeNames.get(issue.getType().getId())));
			}
			logger.info(String.format("%s:%d",
			                          issueFile,
			                          startLine));
		}
		logger.info("");
	}


	void printThresholdStats(ThresholdViolations thresholdViolations) throws MojoFailureException {
		if (thresholdViolations.isFailed()) {
			logger.info("The following thresholds have been exceeded:");
			thresholdViolations.getEntries().forEach((key, value) ->
					                          logger.error(
							                          String.format("Severity: %s, Number of Issues: %d",
							                                        key, value.getIssues())));


			throw new MojoFailureException("Thresholds have been exceeded.");
		}

	}

}
