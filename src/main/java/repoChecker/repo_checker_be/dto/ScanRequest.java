package repoChecker.repo_checker_be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ScanRequest(
		@NotBlank(message = "repoUrl is required") @Pattern(regexp = "^https?://github\\.com/[\\w.\\-]+/[\\w.\\-]+/?(\\.git)?/?$", message = "Must be a valid public GitHub repository URL") String repoUrl) {
}
