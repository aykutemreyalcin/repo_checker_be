package repoChecker.repo_checker_be.store;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import repoChecker.repo_checker_be.model.ScanJob;

@Component
public class InMemoryScanJobStore {

	private final ConcurrentHashMap<String, ScanJob> jobs = new ConcurrentHashMap<>();

	public ScanJob create(String repoUrl) {
		String jobId = UUID.randomUUID().toString();
		ScanJob job = new ScanJob(jobId, repoUrl);
		jobs.put(jobId, job);
		return job;
	}

	public Optional<ScanJob> findById(String jobId) {
		return Optional.ofNullable(jobs.get(jobId));
	}
}
