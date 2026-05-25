package repoChecker.repo_checker_be.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScanJob {

	private final String jobId;
	private final String repoUrl;
	private ScanStatus status;
	private final Instant createdAt;
	private Instant completedAt;
	private String errorMessage;
	private final List<Finding> findings = new ArrayList<>();

	public ScanJob(String jobId, String repoUrl) {
		this.jobId = jobId;
		this.repoUrl = repoUrl;
		this.status = ScanStatus.PENDING;
		this.createdAt = Instant.now();
	}

	public String getJobId() {
		return jobId;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public ScanStatus getStatus() {
		return status;
	}

	public void setStatus(ScanStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public List<Finding> getFindings() {
		return findings;
	}

	public void addFindings(List<Finding> newFindings) {
		findings.addAll(newFindings);
	}
}
