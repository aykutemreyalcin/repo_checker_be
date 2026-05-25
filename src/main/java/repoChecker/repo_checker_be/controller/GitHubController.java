package repoChecker.repo_checker_be.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import repoChecker.repo_checker_be.dto.ScanJobResponse;
import repoChecker.repo_checker_be.dto.ScanReportResponse;
import repoChecker.repo_checker_be.dto.ScanRequest;
import repoChecker.repo_checker_be.service.GitHubService;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

	private final GitHubService githubService;

	public GitHubController(GitHubService githubService) {
		this.githubService = githubService;
	}

	@PostMapping("/scan")
	public ResponseEntity<ScanJobResponse> startScan(@Valid @RequestBody ScanRequest request) {
		ScanJobResponse response = githubService.startSecurityScan(request.repoUrl());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}

	@GetMapping("/scan/{jobId}")
	public ResponseEntity<ScanReportResponse> getScan(@PathVariable String jobId) {
		return githubService.getScanResult(jobId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
