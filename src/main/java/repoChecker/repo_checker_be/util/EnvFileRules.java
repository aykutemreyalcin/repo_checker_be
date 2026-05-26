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

	/** Placeholder check for real .env files (stricter noise reduction). */
	public static boolean isPlaceholder(String value) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.length() < 3) {
			return true;
		}
		return isObviousPlaceholder(normalized)
				|| normalized.contains("changeme")
				|| normalized.contains("your_")
				|| normalized.contains("sample")
				|| normalized.contains("placeholder")
				|| normalized.contains("replace")
				|| normalized.contains("insert")
				|| normalized.contains("dummy")
				|| normalized.contains("fake")
				|| normalized.contains("redact");
	}

	/**
	 * Placeholder check for .env.example — only skip obvious dummy values,
	 * not values merely containing the word "example" (e.g. api.example.com).
	 */
	public static boolean isTemplatePlaceholder(String value) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.length() < 3) {
			return true;
		}
		return isObviousPlaceholder(normalized)
				|| normalized.equals("your_api_key")
				|| normalized.equals("your_secret")
				|| normalized.startsWith("your_")
				|| normalized.startsWith("<")
				|| normalized.contains("${");
	}

	private static boolean isObviousPlaceholder(String normalized) {
		return normalized.equals("xxx")
				|| normalized.equals("todo")
				|| normalized.equals("null")
				|| normalized.equals("none")
				|| normalized.equals("undefined")
				|| normalized.equals("changeme")
				|| normalized.matches("^[*x\\-_.]+$");
	}
}
