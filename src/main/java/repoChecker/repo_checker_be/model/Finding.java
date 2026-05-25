package repoChecker.repo_checker_be.model;

public record Finding(
		FindingSeverity severity,
		FindingCategory category,
		String filePath,
		Integer lineNumber,
		String description,
		String snippet) {
}
