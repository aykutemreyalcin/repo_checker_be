package repoChecker.repo_checker_be.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import repoChecker.repo_checker_be.dto.ScanJobResponse;
import repoChecker.repo_checker_be.dto.ScanReportResponse;
import repoChecker.repo_checker_be.model.ScanJob;
import repoChecker.repo_checker_be.model.ScanStatus;
import repoChecker.repo_checker_be.scan.ScanOrchestrator;
import repoChecker.repo_checker_be.store.InMemoryScanJobStore;
import repoChecker.repo_checker_be.util.RepoUrlParser;

@Service
public class GitHubService {

	private final InMemoryScanJobStore jobStore;
	private final ScanOrchestrator scanOrchestrator;

	public GitHubService(InMemoryScanJobStore jobStore, ScanOrchestrator scanOrchestrator) {
		this.jobStore = jobStore;
		this.scanOrchestrator = scanOrchestrator;
	}

	public ScanJobResponse startSecurityScan(String repoUrl) {
		RepoUrlParser.parse(repoUrl);

		ScanJob job = jobStore.create(repoUrl.trim());
		scanOrchestrator.runAsync(job.getJobId(), job.getRepoUrl());

		return new ScanJobResponse(job.getJobId(), ScanStatus.PENDING);
	}

	public Optional<ScanReportResponse> getScanResult(String jobId) {
		return jobStore.findById(jobId).map(ScanReportResponse::from);
	}
}
