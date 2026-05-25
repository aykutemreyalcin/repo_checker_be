package repoChecker.repo_checker_be.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import repoChecker.repo_checker_be.model.Finding;
import repoChecker.repo_checker_be.model.FindingCategory;
import repoChecker.repo_checker_be.model.FindingSeverity;
import repoChecker.repo_checker_be.util.EnvFileRules;
import repoChecker.repo_checker_be.util.SnippetMasker;

@Component
public class EnvFileScanner {

	private static final Pattern SUSPICIOUS_VALUE = Pattern.compile(
			"(?i)^(api[_-]?key|secret|password|token|private[_-]?key)\\s*=\\s*(.+)$");
	private static final Pattern SUSPICIOUS_VALUE_CONTENT = Pattern.compile(
			"(?i)(ghp_|sk_live_|sk_test_|AKIA|Bearer\\s+)");

	private final long maxFileSizeBytes;

	public EnvFileScanner(@Value("${scan.max-file-size-bytes:1048576}") long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<Finding> scan(Path repoRoot) throws IOException {
		List<Finding> findings = new ArrayList<>();

		try (Stream<Path> paths = Files.walk(repoRoot)) {
			paths.filter(Files::isRegularFile)
					.filter(path -> !FileWalker.isUnderSkippedDirectory(path))
					.filter(this::withinSizeLimit)
					.filter(EnvFileRules::isEnvFile)
					.forEach(path -> scanEnvFile(repoRoot, path, findings));
		}

		return findings;
	}

	private void scanEnvFile(Path repoRoot, Path file, List<Finding> findings) {
		String relativePath = repoRoot.relativize(file).toString();
		boolean template = EnvFileRules.isEnvTemplate(file);

		if (!template) {
			findings.add(new Finding(
					FindingSeverity.HIGH,
					FindingCategory.ENV_FILE,
					relativePath,
					null,
					"Environment file committed to repository",
					null));
		}

		try {
			List<String> lines = Files.readAllLines(file);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i).trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				FindingSeverity lineSeverity = template
						? FindingSeverity.MEDIUM
						: FindingSeverity.CRITICAL;

				var matcher = SUSPICIOUS_VALUE.matcher(line);
				if (matcher.matches()) {
					String value = matcher.group(2).trim();
					if (!value.isEmpty() && !EnvFileRules.isPlaceholder(value)) {
						findings.add(new Finding(
								lineSeverity,
								FindingCategory.ENV_FILE,
								relativePath,
								i + 1,
								template
										? "Non-placeholder value in environment template file"
										: "Sensitive value in environment file",
								SnippetMasker.mask(value)));
					}
				}
				else if (SUSPICIOUS_VALUE_CONTENT.matcher(line).find()
						&& !EnvFileRules.isPlaceholder(line)) {
					findings.add(new Finding(
							lineSeverity,
							FindingCategory.ENV_FILE,
							relativePath,
							i + 1,
							template
									? "Possible secret pattern in environment template file"
									: "Possible secret pattern in environment file",
							SnippetMasker.mask(line)));
				}
			}
		}
		catch (IOException ex) {
			findings.add(new Finding(
					FindingSeverity.LOW,
					FindingCategory.ENV_FILE,
					relativePath,
					null,
					"Could not read environment file: " + ex.getMessage(),
					null));
		}
	}

	private boolean withinSizeLimit(Path path) {
		try {
			return Files.size(path) <= maxFileSizeBytes;
		}
		catch (IOException ex) {
			return false;
		}
	}
}
