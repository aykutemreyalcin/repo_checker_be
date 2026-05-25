package repoChecker.repo_checker_be.util;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Shared rules for detecting env files and template/safe variants.
 */
public final class EnvFileRules {

	private static final Pattern ENV_FILE_NAME = Pattern.compile(
			"(?i)^\\.env(\\.[^/\\\\]+)?$|.*\\.env$|(^|/)\\.env\\.[^/\\\\]+$");

	private static final Pattern ENV_TEMPLATE_NAME = Pattern.compile(
			"(?i)^env\\.example$|^\\.env\\.(example|sample|template|dist)$|\\.env\\.example$");

	private EnvFileRules() {
	}

	public static boolean isEnvFile(Path file) {
		if (isEnvTemplate(file)) {
			return true;
		}
		String fileName = file.getFileName().toString();
		String relative = file.toString().replace('\\', '/');
		return ENV_FILE_NAME.matcher(fileName).matches()
				|| ENV_FILE_NAME.matcher(relative).find();
	}

	/** Committed template files (e.g. .env.example) — no "file committed" alert. */
	public static boolean isEnvTemplate(Path file) {
		String fileName = file.getFileName().toString();
		return ENV_TEMPLATE_NAME.matcher(fileName).matches();
	}

	public static boolean isPlaceholder(String value) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.length() < 3) {
			return true;
		}
		return normalized.contains("changeme")
				|| normalized.contains("your_")
				|| normalized.contains("example")
				|| normalized.contains("sample")
				|| normalized.contains("placeholder")
				|| normalized.contains("replace")
				|| normalized.contains("insert")
				|| normalized.contains("dummy")
				|| normalized.contains("fake")
				|| normalized.contains("redact")
				|| normalized.startsWith("<")
				|| normalized.contains("${")
				|| normalized.equals("xxx")
				|| normalized.equals("todo")
				|| normalized.equals("null")
				|| normalized.equals("none")
				|| normalized.equals("undefined")
				|| normalized.equals("test")
				|| normalized.matches("^[*x\\-_.]+$");
	}
}
