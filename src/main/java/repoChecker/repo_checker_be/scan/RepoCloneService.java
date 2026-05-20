package repoChecker.repo_checker_be.scan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepoCloneService {

	private final long cloneTimeoutSeconds;

	public RepoCloneService(@Value("${scan.clone-timeout-seconds:120}") long cloneTimeoutSeconds) {
		this.cloneTimeoutSeconds = cloneTimeoutSeconds;
	}

	public Path cloneRepository(String cloneUrl, Path targetDir) throws IOException, InterruptedException {
		Files.createDirectories(targetDir.getParent());

		ProcessBuilder processBuilder = new ProcessBuilder(
				"git",
				"clone",
				"--depth",
				"1",
				cloneUrl,
				targetDir.toString());
		processBuilder.redirectErrorStream(true);

		Process process = processBuilder.start();
		String output;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			output = reader.lines().collect(Collectors.joining("\n"));
		}

		boolean finished = process.waitFor(cloneTimeoutSeconds, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new IOException("git clone timed out after " + cloneTimeoutSeconds + " seconds");
		}

		if (process.exitValue() != 0) {
			throw new IOException("git clone failed: " + output);
		}

		return targetDir;
	}
}
