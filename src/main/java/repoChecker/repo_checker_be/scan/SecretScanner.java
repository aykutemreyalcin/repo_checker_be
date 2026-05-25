package repoChecker.repo_checker_be.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import repoChecker.repo_checker_be.model.Finding;
import repoChecker.repo_checker_be.model.FindingCategory;
import repoChecker.repo_checker_be.model.FindingSeverity;
import repoChecker.repo_checker_be.util.SnippetMasker;

@Component
public class SecretScanner {

	private static final List<SecretPattern> PATTERNS = List.of(
			new SecretPattern(
					Pattern.compile("AKIA[0-9A-Z]{16}"),
					FindingSeverity.CRITICAL,
					"Possible AWS access key detected"),
			new SecretPattern(
					Pattern.compile("ghp_[a-zA-Z0-9]{20,}"),
					FindingSeverity.CRITICAL,
					"Possible GitHub personal access token detected"),
			new SecretPattern(
					Pattern.compile("sk_live_[a-zA-Z0-9]{16,}"),
					FindingSeverity.CRITICAL,
					"Possible Stripe live secret key detected"),
			new SecretPattern(
					Pattern.compile("sk_test_[a-zA-Z0-9]{16,}"),
					FindingSeverity.HIGH,
					"Possible Stripe test secret key detected"),
			new SecretPattern(
					Pattern.compile("(?i)(api[_-]?key|secret|password|token)\\s*[=:]\\s*['\"]?([\\w\\-./+=]{8,})"),
					FindingSeverity.HIGH,
					"Possible hardcoded credential assignment detected"),
			new SecretPattern(
					Pattern.compile("Bearer\\s+[a-zA-Z0-9\\-._~+/]+=*"),
					FindingSeverity.HIGH,
					"Possible bearer token detected"));

	private final long maxFileSizeBytes;

	public SecretScanner(@Value("${scan.max-file-size-bytes:1048576}") long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<Finding> scan(Path repoRoot) throws IOException {
		List<Finding> findings = new ArrayList<>();
		for (Path file : FileWalker.listTextFiles(repoRoot, maxFileSizeBytes)) {
			List<String> lines = Files.readAllLines(file);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				for (SecretPattern secretPattern : PATTERNS) {
					Matcher matcher = secretPattern.pattern().matcher(line);
					if (matcher.find()) {
						String matched = matcher.groupCount() >= 2 ? matcher.group(2) : matcher.group();
						findings.add(new Finding(
								secretPattern.severity(),
								FindingCategory.SECRET,
								repoRoot.relativize(file).toString(),
								i + 1,
								secretPattern.description(),
								SnippetMasker.mask(matched)));
					}
				}
			}
		}
		return findings;
	}

	private record SecretPattern(Pattern pattern, FindingSeverity severity, String description) {
	}
}
