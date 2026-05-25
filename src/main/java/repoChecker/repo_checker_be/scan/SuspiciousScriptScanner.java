package repoChecker.repo_checker_be.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import repoChecker.repo_checker_be.model.Finding;
import repoChecker.repo_checker_be.model.FindingCategory;
import repoChecker.repo_checker_be.model.FindingSeverity;

@Component
public class SuspiciousScriptScanner {

	private static final List<SuspiciousPattern> PATTERNS = List.of(
			new SuspiciousPattern(
					Pattern.compile("curl\\s+[^|]+\\|\\s*(ba)?sh"),
					"Pipe curl output directly into shell"),
			new SuspiciousPattern(
					Pattern.compile("wget\\s+[^|]+\\|\\s*(ba)?sh"),
					"Pipe wget output directly into shell"),
			new SuspiciousPattern(
					Pattern.compile("eval\\s*\\("),
					"Use of eval() detected"),
			new SuspiciousPattern(
					Pattern.compile("base64\\s+--decode"),
					"Base64 decode execution pattern detected"));

	private final long maxFileSizeBytes;

	public SuspiciousScriptScanner(@Value("${scan.max-file-size-bytes:1048576}") long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<Finding> scan(Path repoRoot) throws IOException {
		List<Finding> findings = new ArrayList<>();
		for (Path file : FileWalker.listTextFiles(repoRoot, maxFileSizeBytes)) {
			String relativePath = repoRoot.relativize(file).toString();
			if (!isScriptFile(relativePath)) {
				continue;
			}

			List<String> lines = Files.readAllLines(file);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				if (line.trim().startsWith("#")) {
					continue;
				}
				for (SuspiciousPattern pattern : PATTERNS) {
					if (pattern.pattern().matcher(line).find()) {
						findings.add(new Finding(
								FindingSeverity.MEDIUM,
								FindingCategory.SUSPICIOUS_SCRIPT,
								relativePath,
								i + 1,
								pattern.description(),
								line.length() > 120 ? line.substring(0, 120) + "..." : line));
						break;
					}
				}
			}
		}
		return findings;
	}

	private boolean isScriptFile(String relativePath) {
		String lower = relativePath.toLowerCase().replace('\\', '/');
		String fileName = lower.contains("/")
				? lower.substring(lower.lastIndexOf('/') + 1)
				: lower;
		return fileName.endsWith(".sh")
				|| fileName.endsWith(".bash")
				|| fileName.endsWith(".ps1")
				|| (fileName.endsWith(".js") && !fileName.endsWith(".min.js"))
				|| (fileName.endsWith(".ts") && !fileName.endsWith(".d.ts"))
				|| lower.contains("/scripts/");
	}

	private record SuspiciousPattern(Pattern pattern, String description) {
	}
}
