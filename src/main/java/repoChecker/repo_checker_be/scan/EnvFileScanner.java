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

	/** Matches OPENAI_API_KEY, GITHUB_TOKEN, AWS_SECRET_ACCESS_KEY, etc. */
	private static final Pattern SENSITIVE_ENV_ASSIGNMENT = Pattern.compile(
			"(?i)^[A-Za-z0-9_.-]*(?:api[_-]?key|secret|password|token|private[_-]?key|credential|auth)[A-Za-z0-9_.-]*\\s*=\\s*(.+)$");

	private static final Pattern SECRET_TOKEN_IN_VALUE = Pattern.compile(
			"(?i)(ghp_[a-zA-Z0-9]{20,}|sk_live_[a-zA-Z0-9]{16,}|sk_test_[a-zA-Z0-9]{16,}|AKIA[0-9A-Z]{16}|Bearer\\s+[a-zA-Z0-9\\-._~+/]+=*)");

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

				boolean placeholder = template
						? EnvFileRules.isTemplatePlaceholder(extractValue(line))
						: EnvFileRules.isPlaceholder(extractValue(line));

				var keyMatcher = SENSITIVE_ENV_ASSIGNMENT.matcher(line);
				if (keyMatcher.matches()) {
					String value = keyMatcher.group(1).trim();
					if (!value.isEmpty() && !(template
							? EnvFileRules.isTemplatePlaceholder(value)
							: EnvFileRules.isPlaceholder(value))) {
						findings.add(new Finding(
								lineSeverity,
								FindingCategory.ENV_FILE,
								relativePath,
								i + 1,
								template
										? "Sensitive key with non-placeholder value in environment template"
										: "Sensitive value in environment file",
								SnippetMasker.mask(value)));
					}
					continue;
				}

				var secretMatcher = SECRET_TOKEN_IN_VALUE.matcher(line);
				if (secretMatcher.find() && !placeholder) {
					findings.add(new Finding(
							lineSeverity,
							FindingCategory.ENV_FILE,
							relativePath,
							i + 1,
							template
									? "Possible secret pattern in environment template file"
									: "Possible secret pattern in environment file",
							SnippetMasker.mask(secretMatcher.group(1))));
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

	private String extractValue(String line) {
		int eq = line.indexOf('=');
		if (eq < 0 || eq == line.length() - 1) {
			return "";
		}
		return line.substring(eq + 1).trim();
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
