package repoChecker.repo_checker_be.dto;

import repoChecker.repo_checker_be.model.ScanStatus;

public record ScanJobResponse(String jobId, ScanStatus status) {
}
