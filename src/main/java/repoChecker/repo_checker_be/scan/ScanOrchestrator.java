package repoChecker.repo_checker_be.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import repoChecker.repo_checker_be.model.Finding;
import repoChecker.repo_checker_be.model.ScanJob;
import repoChecker.repo_checker_be.model.ScanStatus;
import repoChecker.repo_checker_be.store.InMemoryScanJobStore;
import repoChecker.repo_checker_be.util.RepoUrlParser;

@Component
public class ScanOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(ScanOrchestrator.class);

	private final InMemoryScanJobStore jobStore;
	private final RepoCloneService repoCloneService;
	private final SecretScanner secretScanner;
	private final EnvFileScanner envFileScanner;
	private final SuspiciousScriptScanner suspiciousScriptScanner;
	private final String tempDirPrefix;

	public ScanOrchestrator(
			InMemoryScanJobStore jobStore,
			RepoCloneService repoCloneService,
			SecretScanner secretScanner,
			EnvFileScanner envFileScanner,
			SuspiciousScriptScanner suspiciousScriptScanner,
			@Value("${scan.temp-dir-prefix:repo-checker-}") String tempDirPrefix) {
		this.jobStore = jobStore;
		this.repoCloneService = repoCloneService;
		this.secretScanner = secretScanner;
		this.envFileScanner = envFileScanner;
		this.suspiciousScriptScanner = suspiciousScriptScanner;
		this.tempDirPrefix = tempDirPrefix;
	}

	@Async("scanTaskExecutor")
	public void runAsync(String jobId, String repoUrl) {
		ScanJob job = jobStore.findById(jobId).orElse(null);
		if (job == null) {
			return;
		}

		job.setStatus(ScanStatus.RUNNING);
		Path cloneDir = null;

		try {
			RepoUrlParser.ParsedRepo parsedRepo = RepoUrlParser.parse(repoUrl);
			cloneDir = Files.createTempDirectory(tempDirPrefix + jobId + "-");
			Path repoRoot = cloneDir.resolve("repo");

			repoCloneService.cloneRepository(parsedRepo.cloneUrl(), repoRoot);

			List<Finding> findings = new ArrayList<>();
			findings.addAll(secretScanner.scan(repoRoot));
			findings.addAll(envFileScanner.scan(repoRoot));
			findings.addAll(suspiciousScriptScanner.scan(repoRoot));

			findings.sort(Comparator
					.comparing(Finding::severity)
					.thenComparing(Finding::filePath, Comparator.nullsLast(String::compareTo)));

			job.addFindings(findings);
			job.setStatus(ScanStatus.COMPLETED);
			job.setCompletedAt(Instant.now());
		}
		catch (Exception ex) {
			log.error("Scan failed for job {}", jobId, ex);
			job.setStatus(ScanStatus.FAILED);
			job.setErrorMessage(ex.getMessage());
			job.setCompletedAt(Instant.now());
		}
		finally {
			if (cloneDir != null) {
				deleteRecursively(cloneDir);
			}
		}
	}

	private void deleteRecursively(Path path) {
		try {
			if (Files.exists(path)) {
				try (var paths = Files.walk(path)) {
					paths.sorted(Comparator.reverseOrder()).forEach(p -> {
						try {
							Files.deleteIfExists(p);
						}
						catch (IOException ex) {
							log.warn("Failed to delete {}", p, ex);
						}
					});
				}
			}
		}
		catch (IOException ex) {
			log.warn("Failed to cleanup clone directory {}", path, ex);
		}
	}
}
