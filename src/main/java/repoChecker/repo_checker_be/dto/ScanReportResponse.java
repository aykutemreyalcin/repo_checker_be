package repoChecker.repo_checker_be.dto;

import java.time.Instant;
import java.util.List;

import repoChecker.repo_checker_be.model.ScanJob;
import repoChecker.repo_checker_be.model.ScanStatus;

public record ScanReportResponse(
		String jobId,
		String repoUrl,
		ScanStatus status,
		Instant createdAt,
		Instant completedAt,
		String errorMessage,
		int findingCount,
		List<FindingResponse> findings) {

	public static ScanReportResponse from(ScanJob job) {
		List<FindingResponse> findings = job.getFindings().stream()
				.map(FindingResponse::from)
				.toList();
		return new ScanReportResponse(
				job.getJobId(),
				job.getRepoUrl(),
				job.getStatus(),
				job.getCreatedAt(),
				job.getCompletedAt(),
				job.getErrorMessage(),
				findings.size(),
				findings);
	}
}
