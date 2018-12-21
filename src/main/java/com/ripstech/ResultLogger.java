package com.ripstech;

import com.ripstech.api.entity.receive.application.scan.Issue;
import com.ripstech.api.utils.Severity;
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

	void printNumberOfIssues(Map<String, Integer> totalIssues, Map<String, Integer> newIssues) {
		logger.info( String.format("%d issues have been found. %d are new issues.",
		                           totalIssues.values()
				                           .stream()
				                           .mapToInt(i -> i)
				                           .sum(),
		                           newIssues.values()
				                           .stream()
				                           .mapToInt(i -> i)
				                           .sum()));
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
				System.out.println(String.format("\n%s:", issueTypeNames.get(issue.getType().getId())));
			}
			System.out.println(String.format("%s:%d",
			                          issueFile,
			                          startLine));
		}
	}

	void printThresholdStats(Map<Severity, Integer> reachedThresholds, String uiUrl, long applicationId, long scanId) throws MojoFailureException {
		if (!reachedThresholds.isEmpty()) {
			logger.error("The following thresholds have been exceeded:");
			reachedThresholds.forEach((key, value) ->
					                          logger.error(
					                          		String.format("Severity: %s, Number of Issues: %d",
						                                          key, value)));


			logger.error("Detailed view: " + uiUrl + "/scan/" + applicationId + "/" + scanId);
			throw new MojoFailureException("Thresholds have been exceeded.");

		}
	}

	void printThresholdStats(Map<Severity, Integer> reachedThresholds) throws MojoFailureException {
		if (!reachedThresholds.isEmpty()) {
			logger.error("The following thresholds have been exceeded:");
			reachedThresholds.forEach((key, value) ->
					                          logger.error(
							                          String.format("Severity: %s, Number of Issues: %d",
							                                        key, value)));


			throw new MojoFailureException("Thresholds have been exceeded.");
		}

	}

}
