package repoChecker.repo_checker_be.dto;

import repoChecker.repo_checker_be.model.Finding;
import repoChecker.repo_checker_be.model.FindingCategory;
import repoChecker.repo_checker_be.model.FindingSeverity;

public record FindingResponse(
		FindingSeverity severity,
		FindingCategory category,
		String filePath,
		Integer lineNumber,
		String description,
		String snippet) {

	public static FindingResponse from(Finding finding) {
		return new FindingResponse(
				finding.severity(),
				finding.category(),
				finding.filePath(),
				finding.lineNumber(),
				finding.description(),
				finding.snippet());
	}
}
